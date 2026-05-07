package com.aipichandler.model;

import java.awt.image.BufferedImage;
import java.util.List;

public class CropResult {

    private final BoundingBox cropBox;
    private final BoundingBox subjectBox;
    private final String strategy;
    private final List<DetectionResult> detections;
    private final BufferedImage croppedImage;

    public CropResult(
            BoundingBox cropBox,
            BoundingBox subjectBox,
            String strategy,
            List<DetectionResult> detections,
            BufferedImage croppedImage
    ) {
        this.cropBox = cropBox;
        this.subjectBox = subjectBox;
        this.strategy = strategy;
        this.detections = detections;
        this.croppedImage = croppedImage;
    }

    public BoundingBox cropBox() {
        return cropBox;
    }

    public BoundingBox subjectBox() {
        return subjectBox;
    }

    public String strategy() {
        return strategy;
    }

    public List<DetectionResult> detections() {
        return detections;
    }

    public BufferedImage croppedImage() {
        return croppedImage;
    }
}
