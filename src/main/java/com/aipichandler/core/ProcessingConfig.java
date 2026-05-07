package com.aipichandler.core;

public class ProcessingConfig {

    public enum AspectRatioMode {
        CLAMP_SOURCE_RATIO,
        STRICT_BASE_RATIO
    }

    public static final String DEFAULT_SUBJECT_PROMPT = "";
    public static final double DEFAULT_OUTPUT_ASPECT_RATIO = 1.0;
    public static final double DEFAULT_ASPECT_RATIO_UPPER_FACTOR = 1.0;
    public static final double DEFAULT_ASPECT_RATIO_LOWER_FACTOR = 1.0;
    public static final boolean DEFAULT_ENABLE_SUBJECT_CENTERING = false;
    public static final double DEFAULT_MIN_SUBJECT_VISIBLE_RATIO = 0.92;
    public static final AspectRatioMode DEFAULT_ASPECT_RATIO_MODE = AspectRatioMode.STRICT_BASE_RATIO;

    private final double ratioTolerance;
    private final double outputAspectRatio;
    private final double aspectRatioUpperFactor;
    private final double aspectRatioLowerFactor;
    private final String outputFolderName;
    private final float jpegQuality;
    private final String modelName;
    private final String modelUrl;
    private final String modelEngine;
    private final String subjectPrompt;
    private final AspectRatioMode aspectRatioMode;
    private final boolean enableSubjectCentering;
    private final double minSubjectVisibleRatio;

    public ProcessingConfig(
            double ratioTolerance,
            String outputFolderName,
            float jpegQuality,
            String modelName,
            String modelUrl,
            String modelEngine,
            String subjectPrompt
    ) {
        this(
                ratioTolerance,
                DEFAULT_OUTPUT_ASPECT_RATIO,
                DEFAULT_ASPECT_RATIO_UPPER_FACTOR,
                DEFAULT_ASPECT_RATIO_LOWER_FACTOR,
                outputFolderName,
                jpegQuality,
                modelName,
                modelUrl,
                modelEngine,
                subjectPrompt,
                DEFAULT_ASPECT_RATIO_MODE,
                DEFAULT_ENABLE_SUBJECT_CENTERING,
                DEFAULT_MIN_SUBJECT_VISIBLE_RATIO
        );
    }

    public ProcessingConfig(
            double ratioTolerance,
            double outputAspectRatio,
            double aspectRatioUpperFactor,
            double aspectRatioLowerFactor,
            String outputFolderName,
            float jpegQuality,
            String modelName,
            String modelUrl,
            String modelEngine,
            String subjectPrompt
    ) {
        this(
                ratioTolerance,
                outputAspectRatio,
                aspectRatioUpperFactor,
                aspectRatioLowerFactor,
                outputFolderName,
                jpegQuality,
                modelName,
                modelUrl,
                modelEngine,
                subjectPrompt,
                DEFAULT_ASPECT_RATIO_MODE,
                DEFAULT_ENABLE_SUBJECT_CENTERING,
                DEFAULT_MIN_SUBJECT_VISIBLE_RATIO
        );
    }

    public ProcessingConfig(
            double ratioTolerance,
            double outputAspectRatio,
            double aspectRatioUpperFactor,
            double aspectRatioLowerFactor,
            String outputFolderName,
            float jpegQuality,
            String modelName,
            String modelUrl,
            String modelEngine,
            String subjectPrompt,
            boolean enableSubjectCentering
    ) {
        this(
                ratioTolerance,
                outputAspectRatio,
                aspectRatioUpperFactor,
                aspectRatioLowerFactor,
                outputFolderName,
                jpegQuality,
                modelName,
                modelUrl,
                modelEngine,
                subjectPrompt,
                DEFAULT_ASPECT_RATIO_MODE,
                enableSubjectCentering,
                DEFAULT_MIN_SUBJECT_VISIBLE_RATIO
        );
    }

    public ProcessingConfig(
            double ratioTolerance,
            double outputAspectRatio,
            double aspectRatioUpperFactor,
            double aspectRatioLowerFactor,
            String outputFolderName,
            float jpegQuality,
            String modelName,
            String modelUrl,
            String modelEngine,
            String subjectPrompt,
            AspectRatioMode aspectRatioMode,
            boolean enableSubjectCentering,
            double minSubjectVisibleRatio
    ) {
        if (ratioTolerance < 0 || ratioTolerance >= 1) {
            throw new IllegalArgumentException("ratioTolerance must be in [0, 1).");
        }
        if (outputAspectRatio <= 0) {
            throw new IllegalArgumentException("outputAspectRatio must be > 0.");
        }
        if (aspectRatioUpperFactor <= 0 || aspectRatioLowerFactor <= 0) {
            throw new IllegalArgumentException("aspectRatioUpperFactor/aspectRatioLowerFactor must be > 0.");
        }
        if (outputFolderName == null || outputFolderName.trim().isEmpty()) {
            throw new IllegalArgumentException("outputFolderName cannot be blank.");
        }
        if (jpegQuality <= 0 || jpegQuality > 1) {
            throw new IllegalArgumentException("jpegQuality must be in (0, 1].");
        }
        if (modelName == null || modelName.trim().isEmpty()) {
            throw new IllegalArgumentException("modelName cannot be blank.");
        }
        if (modelUrl == null || modelUrl.trim().isEmpty()) {
            throw new IllegalArgumentException("modelUrl cannot be blank.");
        }
        if (modelEngine == null || modelEngine.trim().isEmpty()) {
            throw new IllegalArgumentException("modelEngine cannot be blank.");
        }
        if (aspectRatioMode == null) {
            throw new IllegalArgumentException("aspectRatioMode cannot be null.");
        }
        if (minSubjectVisibleRatio <= 0 || minSubjectVisibleRatio > 1) {
            throw new IllegalArgumentException("minSubjectVisibleRatio must be in (0, 1].");
        }
        this.ratioTolerance = ratioTolerance;
        this.outputAspectRatio = outputAspectRatio;
        this.aspectRatioUpperFactor = aspectRatioUpperFactor;
        this.aspectRatioLowerFactor = aspectRatioLowerFactor;
        this.outputFolderName = outputFolderName;
        this.jpegQuality = jpegQuality;
        this.modelName = modelName.trim();
        this.modelUrl = modelUrl.trim();
        this.modelEngine = modelEngine.trim();
        this.subjectPrompt = subjectPrompt == null ? DEFAULT_SUBJECT_PROMPT : subjectPrompt.trim();
        this.aspectRatioMode = aspectRatioMode;
        this.enableSubjectCentering = enableSubjectCentering;
        this.minSubjectVisibleRatio = minSubjectVisibleRatio;
    }

    public ProcessingConfig(
            double ratioTolerance,
            double outputAspectRatio,
            double aspectRatioUpperFactor,
            double aspectRatioLowerFactor,
            String outputFolderName,
            float jpegQuality,
            String modelName,
            String modelUrl,
            String modelEngine,
            String subjectPrompt,
            boolean enableSubjectCentering,
            double minSubjectVisibleRatio
    ) {
        this(
                ratioTolerance,
                outputAspectRatio,
                aspectRatioUpperFactor,
                aspectRatioLowerFactor,
                outputFolderName,
                jpegQuality,
                modelName,
                modelUrl,
                modelEngine,
                subjectPrompt,
                DEFAULT_ASPECT_RATIO_MODE,
                enableSubjectCentering,
                minSubjectVisibleRatio
        );
    }

    public double ratioTolerance() {
        return ratioTolerance;
    }

    public double outputAspectRatio() {
        return outputAspectRatio;
    }

    public double aspectRatioUpperFactor() {
        return aspectRatioUpperFactor;
    }

    public double aspectRatioLowerFactor() {
        return aspectRatioLowerFactor;
    }

    public String outputFolderName() {
        return outputFolderName;
    }

    public float jpegQuality() {
        return jpegQuality;
    }

    public String modelName() {
        return modelName;
    }

    public String modelUrl() {
        return modelUrl;
    }

    public String modelEngine() {
        return modelEngine;
    }

    public String subjectPrompt() {
        return subjectPrompt;
    }

    public AspectRatioMode aspectRatioMode() {
        return aspectRatioMode;
    }

    public boolean enableSubjectCentering() {
        return enableSubjectCentering;
    }

    public double minSubjectVisibleRatio() {
        return minSubjectVisibleRatio;
    }
}
