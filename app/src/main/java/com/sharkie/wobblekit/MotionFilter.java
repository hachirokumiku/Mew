package com.sharkie.wobblekit;

final class MotionFilter {
    float gravityX, gravityY, linearX, linearY, dt;
    private long timestamp;
    private int rotation = -1;

    void reset() {
        timestamp = 0;
        rotation = -1;
        gravityX = gravityY = linearX = linearY = dt = 0;
    }

    void update(float x, float y, long time, int currentRotation) {
        float[] axes = WobbleMath.screenAxes(x, y, currentRotation);
        float elapsed = (time - timestamp) / 1_000_000_000f;
        if (timestamp == 0 || rotation != currentRotation || elapsed <= 0 || elapsed > .25f) {
            gravityX = axes[0];
            gravityY = axes[1];
            linearX = linearY = dt = 0;
        } else {
            dt = Math.min(elapsed, .05f);
            float blend = dt / (.8f + dt);
            gravityX += blend * (axes[0] - gravityX);
            gravityY += blend * (axes[1] - gravityY);
            linearX = axes[0] - gravityX;
            linearY = axes[1] - gravityY;
        }
        timestamp = time;
        rotation = currentRotation;
    }
}
