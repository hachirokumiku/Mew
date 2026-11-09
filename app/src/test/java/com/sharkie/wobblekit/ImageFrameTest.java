package com.sharkie.wobblekit;

public final class ImageFrameTest {
    private static void near(float actual, float expected) {
        if (Math.abs(actual - expected) > .0001f) throw new AssertionError(actual + " != " + expected);
    }

    public static void main(String[] args) {
        ImageFrame fit = new ImageFrame(400, 200, 10, 20, 100, 200, false, 1, .5f, .5f);
        near(fit.width, 100); near(fit.height, 50); near(fit.left, 10); near(fit.top, 95);
        near(fit.imageX(60), .5f); near(fit.imageY(120), .5f);
        ImageFrame crop = new ImageFrame(400, 200, 10, 20, 100, 200, true, 1, .5f, .5f);
        near(crop.width, 400); near(crop.height, 200); near(crop.left, -140); near(crop.top, 20);
        near(crop.imageX(60), .5f); near(crop.imageY(120), .5f);
        ImageFrame edge = new ImageFrame(400, 200, 10, 20, 100, 200, true, 2, 1, 0);
        near(edge.width, 800); near(edge.height, 400); near(edge.left, -690); near(edge.top, 20);
        near(edge.imageX(110), 1); near(edge.imageY(20), 0);
        System.out.println("ImageFrame tests passed");
    }
}
