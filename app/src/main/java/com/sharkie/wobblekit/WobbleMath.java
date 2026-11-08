package com.sharkie.wobblekit;

final class WobbleMath {
    static final float GRAVITY = 9.80665f;

    static float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }

    static float radius(float value) {
        return clamp(value, .04f, .5f);
    }

    static float handleX(float centerX, float radius) {
        return centerX <= .5f ? centerX + radius : centerX - radius;
    }

    static float distance(float x, float y, float centerX, float centerY, float heightOverWidth) {
        float dx = x - centerX, dy = (y - centerY) * heightOverWidth;
        return (float) Math.sqrt(dx * dx + dy * dy);
    }

    // Rotation values correspond to Surface.ROTATION_0, 90, 180 and 270.
    static float[] screenAxes(float x, float y, int rotation) {
        switch (rotation) {
            case 1: return new float[] { -y, x };
            case 2: return new float[] { -x, -y };
            case 3: return new float[] { y, -x };
            default: return new float[] { x, y };
        }
    }

    static float targetX(float tiltX, float radius) {
        return -clamp(tiltX / GRAVITY, -1, 1) * radius * .35f;
    }

    static float targetY(float tiltY, float radius, float widthOverHeight) {
        return clamp(tiltY / GRAVITY, -1, 1) * radius * .35f * widthOverHeight;
    }

    static float velocity(float position, float velocity, float target, float dt) {
        return velocity + (85f * (target - position) - 12f * velocity) * dt;
    }
}
