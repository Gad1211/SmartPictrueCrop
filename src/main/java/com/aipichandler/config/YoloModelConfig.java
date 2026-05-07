package com.aipichandler.config;

import java.util.List;

public class YoloModelConfig {

    private final String modelPath;
    private final int fixedInputWidth;
    private final int fixedInputHeight;
    private final double confidenceThreshold;
    private final double nmsThreshold;
    private final int minDynamicSize;
    private final int maxDynamicSize;

    public YoloModelConfig(
            String modelPath,
            int fixedInputWidth,
            int fixedInputHeight,
            double confidenceThreshold,
            double nmsThreshold,
            int minDynamicSize,
            int maxDynamicSize
    ) {
        this.modelPath = modelPath;
        this.fixedInputWidth = fixedInputWidth;
        this.fixedInputHeight = fixedInputHeight;
        this.confidenceThreshold = confidenceThreshold;
        this.nmsThreshold = nmsThreshold;
        this.minDynamicSize = minDynamicSize;
        this.maxDynamicSize = maxDynamicSize;
    }

    public static YoloModelConfig defaultConfig(String modelPath) {
        return new YoloModelConfig(modelPath, 640, 640, 0.25, 0.45, 320, 960);
    }

    public String modelPath() {
        return modelPath;
    }

    public int fixedInputWidth() {
        return fixedInputWidth;
    }

    public int fixedInputHeight() {
        return fixedInputHeight;
    }

    public double confidenceThreshold() {
        return confidenceThreshold;
    }

    public double nmsThreshold() {
        return nmsThreshold;
    }

    public int minDynamicSize() {
        return minDynamicSize;
    }

    public int maxDynamicSize() {
        return maxDynamicSize;
    }

    public List<String> cocoLabels() {
        return List.of(
                "person", "bicycle", "car", "motorcycle", "airplane", "bus", "train", "truck", "boat", "traffic light",
                "fire hydrant", "stop sign", "parking meter", "bench", "bird", "cat", "dog", "horse", "sheep", "cow",
                "elephant", "bear", "zebra", "giraffe", "backpack", "umbrella", "handbag", "tie", "suitcase", "frisbee",
                "skis", "snowboard", "sports ball", "kite", "baseball bat", "baseball glove", "skateboard", "surfboard", "tennis racket", "bottle",
                "wine glass", "cup", "fork", "knife", "spoon", "bowl", "banana", "apple", "sandwich", "orange",
                "broccoli", "carrot", "hot dog", "pizza", "donut", "cake", "chair", "couch", "potted plant", "bed",
                "dining table", "toilet", "tv", "laptop", "mouse", "remote", "keyboard", "cell phone", "microwave", "oven",
                "toaster", "sink", "refrigerator", "book", "clock", "vase", "scissors", "teddy bear", "hair drier", "toothbrush"
        );
    }
}
