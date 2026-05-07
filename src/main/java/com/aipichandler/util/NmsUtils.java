package com.aipichandler.util;

import com.aipichandler.model.BoundingBox;
import com.aipichandler.model.DetectionResult;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public final class NmsUtils {

    private NmsUtils() {
    }

    public static List<DetectionResult> nms(List<DetectionResult> detections, double iouThreshold) {
        List<DetectionResult> sorted = new ArrayList<>(detections);
        sorted.sort(Comparator.comparingDouble(DetectionResult::confidence).reversed());

        List<DetectionResult> kept = new ArrayList<>();
        boolean[] removed = new boolean[sorted.size()];
        for (int i = 0; i < sorted.size(); i++) {
            if (removed[i]) {
                continue;
            }
            DetectionResult base = sorted.get(i);
            kept.add(base);
            for (int j = i + 1; j < sorted.size(); j++) {
                if (removed[j]) {
                    continue;
                }
                DetectionResult candidate = sorted.get(j);
                if (base.classId() != candidate.classId()) {
                    continue;
                }
                if (iou(base.boundingBox(), candidate.boundingBox()) >= iouThreshold) {
                    removed[j] = true;
                }
            }
        }
        return kept;
    }

    public static double iou(BoundingBox a, BoundingBox b) {
        double left = Math.max(a.x(), b.x());
        double top = Math.max(a.y(), b.y());
        double right = Math.min(a.x() + a.width(), b.x() + b.width());
        double bottom = Math.min(a.y() + a.height(), b.y() + b.height());

        double interW = Math.max(0.0, right - left);
        double interH = Math.max(0.0, bottom - top);
        double interArea = interW * interH;
        if (interArea <= 0) {
            return 0.0;
        }
        double union = a.area() + b.area() - interArea;
        return union <= 0 ? 0.0 : interArea / union;
    }
}
