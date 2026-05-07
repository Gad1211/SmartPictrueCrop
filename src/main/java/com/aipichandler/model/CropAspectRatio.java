package com.aipichandler.model;

public enum CropAspectRatio {
    RATIO_1_1(1.0),
    RATIO_4_3(4.0 / 3.0),
    RATIO_16_9(16.0 / 9.0),
    RATIO_9_16(9.0 / 16.0);

    private final double value;

    CropAspectRatio(double value) {
        this.value = value;
    }

    public double value() {
        return value;
    }

    public static CropAspectRatio fromText(String text) {
        if (text == null || text.isBlank()) {
            return RATIO_1_1;
        }
        return switch (text.trim()) {
            case "1:1" -> RATIO_1_1;
            case "4:3" -> RATIO_4_3;
            case "16:9" -> RATIO_16_9;
            case "9:16" -> RATIO_9_16;
            default -> RATIO_1_1;
        };
    }
}
