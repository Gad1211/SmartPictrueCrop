package com.aipichandler.config;

import com.aipichandler.model.PaddingConfig;

import java.util.Map;

public class SmartCropConfig {

    private final PaddingConfig defaultPadding;
    private final PaddingConfig personPadding;
    private final double mergeDistanceFactor;
    private final double mergeIoUThreshold;
    private final Map<String, Double> classPriorityWeights;

    public SmartCropConfig(
            PaddingConfig defaultPadding,
            PaddingConfig personPadding,
            double mergeDistanceFactor,
            double mergeIoUThreshold,
            Map<String, Double> classPriorityWeights
    ) {
        this.defaultPadding = defaultPadding;
        this.personPadding = personPadding;
        this.mergeDistanceFactor = mergeDistanceFactor;
        this.mergeIoUThreshold = mergeIoUThreshold;
        this.classPriorityWeights = classPriorityWeights;
    }

    public static SmartCropConfig defaultConfig() {
        return new SmartCropConfig(
                new PaddingConfig(0.25, 0.15, 0.15, 0.15),
                new PaddingConfig(0.35, 0.15, 0.15, 0.15),
                0.65,
                0.18,
                Map.ofEntries(
                        Map.entry("person", 2.4),
                        Map.entry("dog", 2.1),
                        Map.entry("cat", 2.1),
                        Map.entry("bird", 1.7),
                        Map.entry("horse", 1.8),
                        Map.entry("sheep", 1.7),
                        Map.entry("cow", 1.7),
                        Map.entry("bear", 1.6),
                        Map.entry("zebra", 1.6),
                        Map.entry("giraffe", 1.6)
                )
        );
    }

    public PaddingConfig defaultPadding() {
        return defaultPadding;
    }

    public PaddingConfig personPadding() {
        return personPadding;
    }

    public double mergeDistanceFactor() {
        return mergeDistanceFactor;
    }

    public double mergeIoUThreshold() {
        return mergeIoUThreshold;
    }

    public Map<String, Double> classPriorityWeights() {
        return classPriorityWeights;
    }
}
