package com.aipichandler.detector;

import com.aipichandler.config.YoloModelConfig;
import com.aipichandler.model.BoundingBox;
import com.aipichandler.model.DetectionResult;
import com.aipichandler.util.ImagePreprocessor;
import com.aipichandler.util.NmsUtils;
import ai.onnxruntime.OnnxTensor;
import ai.onnxruntime.NodeInfo;
import ai.onnxruntime.OrtEnvironment;
import ai.onnxruntime.OrtException;
import ai.onnxruntime.OrtSession;
import ai.onnxruntime.TensorInfo;
import ai.onnxruntime.ValueInfo;

import java.awt.image.BufferedImage;
import java.nio.FloatBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class YoloOnnxDetector implements ObjectDetector, AutoCloseable {

    private final YoloModelConfig config;
    private final OrtEnvironment environment;
    private final OrtSession session;
    private final String inputName;
    private final List<String> labels;
    private final long[] modelInputShape;

    public YoloOnnxDetector(YoloModelConfig config) throws OrtException {
        this.config = config;
        this.labels = config.cocoLabels();
        Path modelPath = Path.of(config.modelPath());
        if (!Files.exists(modelPath)) {
            throw new IllegalArgumentException("YOLO model not found: " + modelPath.toAbsolutePath());
        }

        this.environment = OrtEnvironment.getEnvironment();
        OrtSession.SessionOptions options = new OrtSession.SessionOptions();
        options.addCPU(true);
        options.setOptimizationLevel(OrtSession.SessionOptions.OptLevel.ALL_OPT);
        this.session = environment.createSession(modelPath.toString(), options);

        Map<String, NodeInfo> inputs = session.getInputInfo();
        if (inputs.isEmpty()) {
            throw new IllegalStateException("ONNX model has no input nodes.");
        }
        this.inputName = inputs.keySet().iterator().next();
        ValueInfo valueInfo = inputs.get(inputName).getInfo();
        if (!(valueInfo instanceof TensorInfo tensorInfo)) {
            throw new IllegalStateException("Model input is not tensor: " + inputName);
        }
        this.modelInputShape = tensorInfo.getShape();
    }

    @Override
    public List<DetectionResult> detect(BufferedImage image) throws OrtException {
        int[] inputSize = resolveInputSize(image.getWidth(), image.getHeight());
        ImagePreprocessor.PreprocessResult pre = ImagePreprocessor.letterbox(image, inputSize[0], inputSize[1]);

        long[] shape = new long[]{1, 3, pre.inputHeight(), pre.inputWidth()};
        try (OnnxTensor tensor = OnnxTensor.createTensor(environment, FloatBuffer.wrap(pre.chwData()), shape);
             OrtSession.Result result = session.run(Map.of(inputName, tensor))) {
            List<DetectionResult> decoded = decode(result, pre);
            return NmsUtils.nms(decoded, config.nmsThreshold());
        }
    }

    private int[] resolveInputSize(int imageWidth, int imageHeight) {
        long h = modelInputShape.length >= 3 ? modelInputShape[2] : -1;
        long w = modelInputShape.length >= 4 ? modelInputShape[3] : -1;
        if (h > 0 && w > 0) {
            return new int[]{(int) w, (int) h};
        }

        int longest = Math.max(imageWidth, imageHeight);
        int dynamicTarget = Math.max(config.minDynamicSize(), Math.min(longest, config.maxDynamicSize()));
        dynamicTarget = alignToStride(dynamicTarget, 32);
        return new int[]{dynamicTarget, dynamicTarget};
    }

    private int alignToStride(int value, int stride) {
        int aligned = (int) Math.ceil(value / (double) stride) * stride;
        return Math.max(stride, aligned);
    }

    private List<DetectionResult> decode(OrtSession.Result result, ImagePreprocessor.PreprocessResult pre) throws OrtException {
        List<DetectionResult> detections = new ArrayList<>();
        if (result.size() == 0) {
            return detections;
        }

        Object output = result.get(0).getValue();
        if (!(output instanceof float[][][] tensor3d)) {
            throw new IllegalStateException("Unexpected YOLO output type: " + output.getClass());
        }
        if (tensor3d.length == 0) {
            return detections;
        }

        float[][] raw = tensor3d[0];
        boolean transposed = raw.length > 4 && raw[0].length > raw.length;
        if (transposed) {
            // shape: [84, N] -> [N, 84]
            int cols = raw[0].length;
            int rows = raw.length;
            for (int n = 0; n < cols; n++) {
                float[] row = new float[rows];
                for (int r = 0; r < rows; r++) {
                    row[r] = raw[r][n];
                }
                parsePredictionRow(row, pre, detections);
            }
        } else {
            // shape: [N, 84]
            for (float[] row : raw) {
                parsePredictionRow(row, pre, detections);
            }
        }
        return detections;
    }

    private void parsePredictionRow(
            float[] row,
            ImagePreprocessor.PreprocessResult pre,
            List<DetectionResult> out
    ) {
        if (row.length < 6) {
            return;
        }

        float cx = row[0];
        float cy = row[1];
        float w = row[2];
        float h = row[3];

        int bestClassId = -1;
        float bestClassConf = 0f;
        for (int c = 4; c < row.length; c++) {
            if (row[c] > bestClassConf) {
                bestClassConf = row[c];
                bestClassId = c - 4;
            }
        }
        if (bestClassId < 0 || bestClassConf < config.confidenceThreshold()) {
            return;
        }

        double x1 = (cx - w / 2.0 - pre.padX()) / pre.scale();
        double y1 = (cy - h / 2.0 - pre.padY()) / pre.scale();
        double x2 = (cx + w / 2.0 - pre.padX()) / pre.scale();
        double y2 = (cy + h / 2.0 - pre.padY()) / pre.scale();

        x1 = clamp(x1, 0, pre.originalWidth() - 1);
        y1 = clamp(y1, 0, pre.originalHeight() - 1);
        x2 = clamp(x2, 0, pre.originalWidth());
        y2 = clamp(y2, 0, pre.originalHeight());

        double boxW = x2 - x1;
        double boxH = y2 - y1;
        if (boxW < 2 || boxH < 2) {
            return;
        }

        String className = labelOf(bestClassId);
        out.add(new DetectionResult(
                bestClassId,
                className,
                bestClassConf,
                new BoundingBox(x1, y1, boxW, boxH)
        ));
    }

    private String labelOf(int classId) {
        if (classId < 0 || classId >= labels.size()) {
            return "class_" + classId;
        }
        return labels.get(classId).toLowerCase(Locale.ROOT);
    }

    private double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    @Override
    public void close() throws OrtException {
        session.close();
        environment.close();
    }
}
