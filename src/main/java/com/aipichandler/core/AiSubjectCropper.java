package com.aipichandler.core;

import com.aipichandler.config.SmartCropConfig;
import com.aipichandler.config.YoloModelConfig;
import com.aipichandler.cropper.SmartCropStrategy;
import com.aipichandler.detector.YoloOnnxDetector;
import com.aipichandler.model.CropAspectRatio;
import com.aipichandler.model.CropResult;
import com.aipichandler.model.DetectionResult;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

public class AiSubjectCropper implements AutoCloseable {

    private final Consumer<String> logger;
    private final SmartCropStrategy cropStrategy;
    private YoloOnnxDetector detector;

    public AiSubjectCropper(ProcessingConfig config, Consumer<String> logger) {
        this.logger = logger;
        this.cropStrategy = new SmartCropStrategy(
                SmartCropConfig.defaultConfig(),
                null,
                config.enableSubjectCentering(),
                config.minSubjectVisibleRatio()
        );
        loadDetector(config);
    }

    private void loadDetector(ProcessingConfig config) {
        try {
            YoloModelConfig yoloConfig = YoloModelConfig.defaultConfig(config.modelUrl());
            this.detector = new YoloOnnxDetector(yoloConfig);
            logger.accept("YOLO11 ONNX 模型加载成功（CPUExecutionProvider）。模型: " + config.modelUrl());
        } catch (Exception e) {
            logger.accept("YOLO11 模型加载失败，回退中心裁剪。原因: " + e.getMessage());
            detector = null;
        }
    }

    public BufferedImage cropMainSubjectSquare(BufferedImage source, double tolerance) {
        CropResult result = cropMainSubject(source, CropAspectRatio.RATIO_1_1);
        return result.croppedImage();
    }

    public BufferedImage cropMainSubject(BufferedImage source, double ratio) {
        CropResult result = cropMainSubjectResult(source, ratio);
        return result.croppedImage();
    }

    public CropResult cropMainSubject(BufferedImage source, CropAspectRatio ratio) {
        return cropMainSubjectResult(source, ratio.value());
    }

    public CropResult cropMainSubjectResult(BufferedImage source, double ratio) {
        List<DetectionResult> detections = detect(source);
        return cropStrategy.crop(source, detections, ratio);
    }

    private List<DetectionResult> detect(BufferedImage source) {
        if (detector == null) {
            return Collections.emptyList();
        }
        try {
            return detector.detect(source);
        } catch (Exception e) {
            logger.accept("YOLO 推理失败，本张图片回退中心裁剪。原因: " + e.getMessage());
            return Collections.emptyList();
        }
    }

    public static BufferedImage toRgbImage(BufferedImage source) {
        if (source.getType() == BufferedImage.TYPE_INT_RGB) {
            return source;
        }
        BufferedImage rgb = new BufferedImage(source.getWidth(), source.getHeight(), BufferedImage.TYPE_INT_RGB);
        Graphics2D g = rgb.createGraphics();
        g.drawImage(source, 0, 0, null);
        g.dispose();
        return rgb;
    }

    @Override
    public void close() {
        if (detector != null) {
            try {
                detector.close();
            } catch (Exception e) {
                logger.accept("关闭 ONNX Runtime 资源时发生异常: " + e.getMessage());
            }
        }
    }
}
