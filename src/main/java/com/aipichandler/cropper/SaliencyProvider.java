package com.aipichandler.cropper;

import com.aipichandler.model.BoundingBox;

import java.awt.image.BufferedImage;
import java.util.List;

/**
 * Extension point for saliency / face / attention models.
 */
public interface SaliencyProvider {
    List<BoundingBox> inferSalientRegions(BufferedImage image);
}
