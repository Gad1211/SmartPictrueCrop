package com.aipichandler.model;

public class DetectionResult {

    private final int classId;
    private final String className;
    private final double confidence;
    private final BoundingBox boundingBox;

    public DetectionResult(int classId, String className, double confidence, BoundingBox boundingBox) {
        this.classId = classId;
        this.className = className;
        this.confidence = confidence;
        this.boundingBox = boundingBox;
    }

    public int classId() {
        return classId;
    }

    public String className() {
        return className;
    }

    public double confidence() {
        return confidence;
    }

    public BoundingBox boundingBox() {
        return boundingBox;
    }

    public boolean isPerson() {
        return "person".equalsIgnoreCase(className);
    }
}
