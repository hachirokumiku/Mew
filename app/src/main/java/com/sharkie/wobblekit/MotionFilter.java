package com.sharkie.wobblekit;

final class MotionFilter {
    float gravityX, gravityY, gravityZ, linearX, linearY, linearZ, dt;
    private long timestamp;
    private int rotation = -1;

    void reset() {
        timestamp = 0;
        rotation = -1;
        gravityX = gravityY = gravityZ = linearX = linearY = linearZ = dt = 0;
    }

    void update(float x, float y, float z, long time, int currentRotation) {
        float[] axes = WobbleMath.screenAxes(x, y, currentRotation);
        float elapsed = (time - timestamp) / 1_000_000_000f;
        if (timestamp == 0 || rotation != currentRotation || elapsed <= 0 || elapsed > .25f) {
            gravityX = axes[0];
            gravityY = axes[1];
            gravityZ = z;
            linearX = linearY = linearZ = dt = 0;
        } else {
            dt = Math.min(elapsed, .05f);
            float blend = dt / (.12f + dt);
            gravityX += blend * (axes[0] - gravityX);
            gravityY += blend * (axes[1] - gravityY);
            gravityZ += blend * (z - gravityZ);
            linearX = axes[0] - gravityX;
            linearY = axes[1] - gravityY;
            linearZ = z - gravityZ;
        }
        timestamp = time;
        rotation = currentRotation;
    }
}
