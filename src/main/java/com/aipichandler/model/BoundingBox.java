package com.aipichandler.model;

import java.awt.Rectangle;

/**
 * Pixel-space bounding box.
 */
public class BoundingBox {

    private final double x;
    private final double y;
    private final double width;
    private final double height;

    public BoundingBox(double x, double y, double width, double height) {
        this.x = x;
        this.y = y;
        this.width = Math.max(1.0, width);
        this.height = Math.max(1.0, height);
    }

    public double x() {
        return x;
    }

    public double y() {
        return y;
    }

    public double width() {
        return width;
    }

    public double height() {
        return height;
    }

    public double centerX() {
        return x + width / 2.0;
    }

    public double centerY() {
        return y + height / 2.0;
    }

    public double area() {
        return width * height;
    }

    public BoundingBox union(BoundingBox other) {
        double left = Math.min(this.x, other.x);
        double top = Math.min(this.y, other.y);
        double right = Math.max(this.x + this.width, other.x + other.width);
        double bottom = Math.max(this.y + this.height, other.y + other.height);
        return new BoundingBox(left, top, right - left, bottom - top);
    }

    public BoundingBox expanded(double leftRatio, double topRatio, double rightRatio, double bottomRatio) {
        double newX = x - width * leftRatio;
        double newY = y - height * topRatio;
        double newWidth = width * (1.0 + leftRatio + rightRatio);
        double newHeight = height * (1.0 + topRatio + bottomRatio);
        return new BoundingBox(newX, newY, newWidth, newHeight);
    }

    public BoundingBox clamp(int imageWidth, int imageHeight) {
        double left = Math.max(0, x);
        double top = Math.max(0, y);
        double right = Math.min(imageWidth, x + width);
        double bottom = Math.min(imageHeight, y + height);
        return new BoundingBox(left, top, Math.max(1.0, right - left), Math.max(1.0, bottom - top));
    }

    public Rectangle toRectangle() {
        return new Rectangle(
                (int) Math.round(x),
                (int) Math.round(y),
                (int) Math.max(1, Math.round(width)),
                (int) Math.max(1, Math.round(height))
        );
    }
}
