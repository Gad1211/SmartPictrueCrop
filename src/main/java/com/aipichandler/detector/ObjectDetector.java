package com.aipichandler.detector;

import com.aipichandler.model.DetectionResult;

import java.awt.image.BufferedImage;
import java.util.List;

public interface ObjectDetector {
    List<DetectionResult> detect(BufferedImage image) throws Exception;
}
