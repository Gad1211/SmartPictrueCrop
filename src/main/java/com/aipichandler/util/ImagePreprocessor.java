package com.aipichandler.util;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;

public final class ImagePreprocessor {

    private ImagePreprocessor() {
    }

    public static PreprocessResult letterbox(BufferedImage source, int targetWidth, int targetHeight) {
        double scale = Math.min((double) targetWidth / source.getWidth(), (double) targetHeight / source.getHeight());
        int resizedWidth = Math.max(1, (int) Math.round(source.getWidth() * scale));
        int resizedHeight = Math.max(1, (int) Math.round(source.getHeight() * scale));
        int padX = (targetWidth - resizedWidth) / 2;
        int padY = (targetHeight - resizedHeight) / 2;

        BufferedImage canvas = new BufferedImage(targetWidth, targetHeight, BufferedImage.TYPE_3BYTE_BGR);
        Graphics2D g2d = canvas.createGraphics();
        g2d.setColor(new Color(114, 114, 114));
        g2d.fillRect(0, 0, targetWidth, targetHeight);
        g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g2d.drawImage(source, padX, padY, resizedWidth, resizedHeight, null);
        g2d.dispose();

        float[] chw = toNormalizedChw(canvas);
        return new PreprocessResult(chw, source.getWidth(), source.getHeight(), targetWidth, targetHeight, scale, padX, padY);
    }

    private static float[] toNormalizedChw(BufferedImage image) {
        int width = image.getWidth();
        int height = image.getHeight();
        float[] data = new float[3 * width * height];

        int[] rgb = image.getRGB(0, 0, width, height, null, 0, width);
        int hw = width * height;
        for (int i = 0; i < rgb.length; i++) {
            int pixel = rgb[i];
            float r = ((pixel >> 16) & 0xFF) / 255f;
            float g = ((pixel >> 8) & 0xFF) / 255f;
            float b = (pixel & 0xFF) / 255f;
            data[i] = r;
            data[hw + i] = g;
            data[2 * hw + i] = b;
        }
        return data;
    }

    public record PreprocessResult(
            float[] chwData,
            int originalWidth,
            int originalHeight,
            int inputWidth,
            int inputHeight,
            double scale,
            int padX,
            int padY
    ) {
    }
}
