package com.aipichandler.cropper;

import com.aipichandler.config.SmartCropConfig;
import com.aipichandler.model.BoundingBox;
import com.aipichandler.model.CropAspectRatio;
import com.aipichandler.model.CropResult;
import com.aipichandler.model.DetectionResult;
import com.aipichandler.model.PaddingConfig;
import com.aipichandler.util.NmsUtils;

import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

public class SmartCropStrategy {

    /**
     * 主体至少保留比例
     */
    private static final double DEFAULT_MIN_SUBJECT_VISIBLE_RATIO = 0.92;

    /**
     * 人物视觉中心偏移
     * 人脸通常位于 bbox 上方
     */
    private static final double PERSON_VISUAL_CENTER_Y_RATIO = 0.35;

    private final SmartCropConfig config;
    private final SaliencyProvider saliencyProvider;
    private final boolean enableSubjectCentering;
    private final double minSubjectVisibleRatio;

    public SmartCropStrategy(SmartCropConfig config, SaliencyProvider saliencyProvider) {
        this(config, saliencyProvider, false, DEFAULT_MIN_SUBJECT_VISIBLE_RATIO);
    }

    public SmartCropStrategy(
            SmartCropConfig config,
            SaliencyProvider saliencyProvider,
            boolean enableSubjectCentering
    ) {
        this(config, saliencyProvider, enableSubjectCentering, DEFAULT_MIN_SUBJECT_VISIBLE_RATIO);
    }

    public SmartCropStrategy(
            SmartCropConfig config,
            SaliencyProvider saliencyProvider,
            boolean enableSubjectCentering,
            double minSubjectVisibleRatio
    ) {
        this.config = config;
        this.saliencyProvider = saliencyProvider;
        this.enableSubjectCentering = enableSubjectCentering;
        this.minSubjectVisibleRatio = minSubjectVisibleRatio;
    }

    public CropResult crop(
            BufferedImage source,
            List<DetectionResult> detections,
            CropAspectRatio ratio
    ) {
        return crop(source, detections, ratio.value());
    }

    public CropResult crop(
            BufferedImage source,
            List<DetectionResult> detections,
            double ratio
    ) {

        double targetRatio = ratio > 0
                ? ratio
                : CropAspectRatio.RATIO_1_1.value();

        if (detections == null || detections.isEmpty()) {

            BoundingBox fallback = centerCrop(
                    source.getWidth(),
                    source.getHeight(),
                    targetRatio
            );

            return buildResult(
                    source,
                    fallback,
                    fallback,
                    "center_fallback",
                    List.of()
            );
        }

        List<DetectionResult> merged = mergeCloseSubjects(detections);

        BoundingBox subjectBox = buildVisualSubjectBox(merged);

        if (subjectBox == null) {

            BoundingBox fallback = centerCrop(
                    source.getWidth(),
                    source.getHeight(),
                    targetRatio
            );

            return buildResult(
                    source,
                    fallback,
                    fallback,
                    "center_fallback",
                    detections
            );
        }

        DetectionResult mainSubject = selectMainSubject(
                merged,
                source.getWidth(),
                source.getHeight()
        );

        PaddingConfig padding = mainSubject != null && mainSubject.isPerson()
                ? config.personPadding()
                : config.defaultPadding();

        BoundingBox expanded = subjectBox
                .expanded(
                        padding.left(),
                        padding.top(),
                        padding.right(),
                        padding.bottom()
                )
                .clamp(source.getWidth(), source.getHeight());

        BoundingBox finalCrop = adaptToAspectRatio(
                expanded,
                targetRatio,
                source.getWidth(),
                source.getHeight()
        );

        if (enableSubjectCentering) {

            finalCrop = adjustForSubjectCentering(
                    finalCrop,
                    subjectBox,
                    mainSubject != null && mainSubject.isPerson(),
                    source.getWidth(),
                    source.getHeight()
            );
        }

        if (saliencyProvider != null) {
            saliencyProvider.inferSalientRegions(source);
        }

        return buildResult(
                source,
                finalCrop,
                subjectBox,
                "detected_smart_crop",
                detections
        );
    }

    private CropResult buildResult(
            BufferedImage source,
            BoundingBox cropBox,
            BoundingBox subjectBox,
            String strategy,
            List<DetectionResult> detections
    ) {

        Rectangle r = cropBox.toRectangle();

        r = clampRect(
                r,
                source.getWidth(),
                source.getHeight()
        );

        BufferedImage cropped = source.getSubimage(
                r.x,
                r.y,
                r.width,
                r.height
        );

        return new CropResult(
                new BoundingBox(r.x, r.y, r.width, r.height),
                subjectBox,
                strategy,
                detections,
                cropped
        );
    }

    /**
     * 多主体合并
     */
    private BoundingBox buildVisualSubjectBox(List<DetectionResult> detections) {

        if (detections == null || detections.isEmpty()) {
            return null;
        }

        List<DetectionResult> important = detections.stream()
                .filter(d -> d.confidence() >= 0.35)
                .toList();

        if (important.isEmpty()) {
            important = detections;
        }

        BoundingBox union = important.get(0).boundingBox();

        for (int i = 1; i < important.size(); i++) {
            union = union.union(important.get(i).boundingBox());
        }

        return union;
    }

    private List<DetectionResult> mergeCloseSubjects(
            List<DetectionResult> detections
    ) {

        List<DetectionResult> sorted = new ArrayList<>(detections);

        sorted.sort(
                Comparator.comparingDouble(this::subjectScore)
                        .reversed()
        );

        boolean[] merged = new boolean[sorted.size()];

        List<DetectionResult> result = new ArrayList<>();

        for (int i = 0; i < sorted.size(); i++) {

            if (merged[i]) {
                continue;
            }

            DetectionResult base = sorted.get(i);

            BoundingBox union = base.boundingBox();

            double conf = base.confidence();

            int count = 1;

            for (int j = i + 1; j < sorted.size(); j++) {

                if (merged[j]) {
                    continue;
                }

                DetectionResult candidate = sorted.get(j);

                if (isClose(union, candidate.boundingBox())) {

                    union = union.union(candidate.boundingBox());

                    conf = Math.max(
                            conf,
                            candidate.confidence()
                    );

                    merged[j] = true;

                    count++;
                }
            }

            String className = count > 1
                    ? base.className() + "_group"
                    : base.className();

            result.add(
                    new DetectionResult(
                            base.classId(),
                            className,
                            conf,
                            union
                    )
            );
        }

        return result;
    }

    private boolean isClose(BoundingBox a, BoundingBox b) {

        double iou = NmsUtils.iou(a, b);

        if (iou >= config.mergeIoUThreshold()) {
            return true;
        }

        double dx = a.centerX() - b.centerX();

        double dy = a.centerY() - b.centerY();

        double distance = Math.sqrt(dx * dx + dy * dy);

        double scale = Math.max(
                Math.max(a.width(), a.height()),
                Math.max(b.width(), b.height())
        );

        return distance <= scale * config.mergeDistanceFactor();
    }

    private DetectionResult selectMainSubject(
            List<DetectionResult> detections,
            int imageWidth,
            int imageHeight
    ) {

        DetectionResult best = null;

        double bestScore = -1;

        for (DetectionResult d : detections) {

            double areaWeight =
                    d.boundingBox().area()
                            / (imageWidth * (double) imageHeight);

            double score =
                    d.confidence()
                            * (0.5 + areaWeight)
                            * priorityWeight(d.className());

            if (score > bestScore) {

                best = d;

                bestScore = score;
            }
        }

        return best;
    }

    private double subjectScore(DetectionResult d) {

        return d.confidence()
                * priorityWeight(d.className())
                * Math.sqrt(d.boundingBox().area());
    }

    private double priorityWeight(String className) {

        String key = className == null
                ? ""
                : className.toLowerCase(Locale.ROOT);

        for (var entry : config.classPriorityWeights().entrySet()) {

            if (key.contains(entry.getKey())) {
                return entry.getValue();
            }
        }

        if (isCommonObject(key)) {
            return 1.2;
        }

        return 1.0;
    }

    private boolean isCommonObject(String key) {

        return key.contains("car")
                || key.contains("chair")
                || key.contains("bottle")
                || key.contains("phone")
                || key.contains("tv");
    }

    private BoundingBox centerCrop(
            int imageWidth,
            int imageHeight,
            double ratio
    ) {

        double cropW = imageWidth;

        double cropH = cropW / ratio;

        if (cropH > imageHeight) {

            cropH = imageHeight;

            cropW = cropH * ratio;
        }

        double x = (imageWidth - cropW) / 2.0;

        double y = (imageHeight - cropH) / 2.0;

        return new BoundingBox(x, y, cropW, cropH);
    }

    private BoundingBox adaptToAspectRatio(
            BoundingBox subject,
            double ratio,
            int imageWidth,
            int imageHeight
    ) {

        double centerX = subject.centerX();

        double centerY = subject.centerY();

        double cropW = subject.width();

        double cropH = subject.height();

        if (cropW / cropH > ratio) {

            cropH = cropW / ratio;

        } else {

            cropW = cropH * ratio;
        }

        cropW = Math.max(cropW, subject.width());

        cropH = Math.max(cropH, subject.height());

        if (cropW > imageWidth) {

            cropW = imageWidth;

            cropH = cropW / ratio;
        }

        if (cropH > imageHeight) {

            cropH = imageHeight;

            cropW = cropH * ratio;
        }

        double x = centerX - cropW / 2.0;

        double y = centerY - cropH / 2.0;

        x = Math.max(0, Math.min(x, imageWidth - cropW));

        y = Math.max(0, Math.min(y, imageHeight - cropH));

        BoundingBox crop = new BoundingBox(
                x,
                y,
                cropW,
                cropH
        ).clamp(imageWidth, imageHeight);

        if (!containsEnough(crop, subject)) {

            crop = forceContain(
                    crop,
                    subject,
                    ratio,
                    imageWidth,
                    imageHeight
            );
        }

        return crop;
    }

    /**
     * Soft contain
     */
    private boolean containsEnough(
            BoundingBox outer,
            BoundingBox inner
    ) {

        double ix1 = Math.max(outer.x(), inner.x());

        double iy1 = Math.max(outer.y(), inner.y());

        double ix2 = Math.min(
                outer.x() + outer.width(),
                inner.x() + inner.width()
        );

        double iy2 = Math.min(
                outer.y() + outer.height(),
                inner.y() + inner.height()
        );

        if (ix2 <= ix1 || iy2 <= iy1) {
            return false;
        }

        double intersection =
                (ix2 - ix1) * (iy2 - iy1);

        double ratio = intersection / inner.area();

        return ratio >= minSubjectVisibleRatio;
    }

    private BoundingBox forceContain(
            BoundingBox crop,
            BoundingBox subject,
            double ratio,
            int imageWidth,
            int imageHeight
    ) {

        double needW = Math.max(
                crop.width(),
                subject.width()
        );

        double needH = Math.max(
                crop.height(),
                subject.height()
        );

        if (needW / needH > ratio) {

            needH = needW / ratio;

        } else {

            needW = needH * ratio;
        }

        needW = Math.min(needW, imageWidth);

        needH = Math.min(needH, imageHeight);

        double x = subject.centerX() - needW / 2.0;

        double y = subject.centerY() - needH / 2.0;

        x = Math.max(0, Math.min(x, imageWidth - needW));

        y = Math.max(0, Math.min(y, imageHeight - needH));

        return new BoundingBox(
                x,
                y,
                needW,
                needH
        ).clamp(imageWidth, imageHeight);
    }

    /**
     * 新版居中算法：
     *
     * 1. 不缩放 crop
     * 2. 仅平移
     * 3. 尽量让主体视觉中心靠近 crop 中心
     */
    private BoundingBox adjustForSubjectCentering(
            BoundingBox crop,
            BoundingBox subject,
            boolean isPersonSubject,
            int imageWidth,
            int imageHeight
    ) {

        double cropW = crop.width();

        double cropH = crop.height();

        double visualCenterX = subject.centerX();

        double visualCenterY;

        if (isPersonSubject) {

            visualCenterY =
                    subject.y()
                            + subject.height()
                            * PERSON_VISUAL_CENTER_Y_RATIO;

        } else {

            visualCenterY = subject.centerY();
        }

        double targetX = visualCenterX - cropW / 2.0;

        double targetY = visualCenterY - cropH / 2.0;

        targetX = Math.max(
                0,
                Math.min(targetX, imageWidth - cropW)
        );

        targetY = Math.max(
                0,
                Math.min(targetY, imageHeight - cropH)
        );

        BoundingBox centered = new BoundingBox(
                targetX,
                targetY,
                cropW,
                cropH
        ).clamp(imageWidth, imageHeight);

        if (!containsEnough(centered, subject)) {
            return crop;
        }

        return centered;
    }

    private Rectangle clampRect(
            Rectangle rect,
            int imageWidth,
            int imageHeight
    ) {

        int x = Math.max(
                0,
                Math.min(rect.x, imageWidth - 1)
        );

        int y = Math.max(
                0,
                Math.min(rect.y, imageHeight - 1)
        );

        int w = Math.max(
                1,
                Math.min(rect.width, imageWidth - x)
        );

        int h = Math.max(
                1,
                Math.min(rect.height, imageHeight - y)
        );

        return new Rectangle(x, y, w, h);
    }
}