package com.aipichandler.model;

public class PaddingConfig {

    private final double top;
    private final double bottom;
    private final double left;
    private final double right;

    public PaddingConfig(double top, double bottom, double left, double right) {
        this.top = top;
        this.bottom = bottom;
        this.left = left;
        this.right = right;
    }

    public double top() {
        return top;
    }

    public double bottom() {
        return bottom;
    }

    public double left() {
        return left;
    }

    public double right() {
        return right;
    }
}
