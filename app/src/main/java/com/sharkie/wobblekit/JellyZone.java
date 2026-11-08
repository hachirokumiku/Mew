package com.sharkie.wobblekit;

final class JellyZone {
    float x, y, rx, ry, angle;
    int shape;
    float dx, dy, vx, vy;
    private float stretch, stretchVelocity;
    private float cosAngle, sinAngle, cachedAngle = Float.NaN;

    JellyZone(float x, float y, float radius) {
        this.x = x;
        this.y = y;
        rx = ry = radius;
    }

    float radius() { return (float) Math.sqrt(rx * ry); }

    float handleDirection(float heightOverWidth) {
        updateAngle();
        float rightX = x + rx * cosAngle, rightY = y + rx * sinAngle / heightOverWidth;
        if (rightX >= 0 && rightX <= 1 && rightY >= 0 && rightY <= 1) return 1;
        float leftX = x - rx * cosAngle, leftY = y - rx * sinAngle / heightOverWidth;
        if (leftX >= 0 && leftX <= 1 && leftY >= 0 && leftY <= 1) return -1;
        return 1;
    }

    private void updateAngle() {
        if (cachedAngle != angle) {
            double radians = Math.toRadians(angle);
            cosAngle = (float) Math.cos(radians);
            sinAngle = (float) Math.sin(radians);
            cachedAngle = angle;
        }
    }

    float localX(float x, float y) {
        updateAngle();
        return x * cosAngle + y * sinAngle;
    }

    float localY(float x, float y) {
        updateAngle();
        return -x * sinAngle + y * cosAngle;
    }

    float normalizedDistance(float x, float y) {
        float localX = localX(x, y) / rx;
        float localY = localY(x, y) / ry;
        return (float) Math.sqrt(localX * localX + localY * localY);
    }

    float weight(float x, float y) {
        float distance = normalizedDistance(x, y);
        if (distance >= 1) return 0;
        float remaining = 1 - distance;
        if (shape == 1) return remaining;
        if (shape == 2) return remaining * remaining * remaining;
        return remaining * remaining * (1 + 2 * distance);
    }

    void impulse(float x, float y) {
        vx += x;
        vy += y;
        float amount = Math.min(.25f, (float) Math.hypot(x, y) / radius() * .15f);
        stretchVelocity = WobbleMath.clamp(stretchVelocity + amount, -2f, 2f);
    }

    void step(float targetX, float targetY, float spring, float damping, float dt) {
        vx = WobbleMath.velocity(dx, vx, targetX, spring, damping, dt);
        vy = WobbleMath.velocity(dy, vy, targetY, spring, damping, dt);
        dx += vx * dt;
        dy += vy * dt;
        float speed = (float) Math.hypot(vx, vy) / radius();
        float stretchTarget = Math.min(.25f, speed * .07f);
        stretchVelocity = WobbleMath.velocity(stretch, stretchVelocity, stretchTarget,
                spring * .65f, damping * .65f, dt);
        stretch += stretchVelocity * dt;
        if (stretch > .3f) { stretch = .3f; stretchVelocity = Math.min(0, stretchVelocity); }
        if (stretch < -.3f) { stretch = -.3f; stretchVelocity = Math.max(0, stretchVelocity); }
    }

    float meshX(float x, float y, float weight) {
        float localX = localX(x, y), localY = localY(x, y);
        float changeX = localX * stretch, changeY = -localY * stretch;
        return weight * (dx + changeX * cosAngle - changeY * sinAngle);
    }

    float meshY(float x, float y, float weight) {
        float localX = localX(x, y), localY = localY(x, y);
        float changeX = localX * stretch, changeY = -localY * stretch;
        return weight * (dy + changeX * sinAngle + changeY * cosAngle);
    }

    String serialize() {
        return x + "," + y + "," + rx + "," + ry + "," + angle + "," + shape;
    }

    static JellyZone parse(String value) {
        String[] parts = value.split(",");
        if (parts.length != 3 && parts.length != 6) throw new IllegalArgumentException("Invalid zone");
        float x = finite(parts[0]), y = finite(parts[1]);
        float rx = finite(parts[2]);
        JellyZone zone = new JellyZone(WobbleMath.clamp(x, 0, 1), WobbleMath.clamp(y, 0, 1), WobbleMath.radius(rx));
        if (parts.length == 6) {
            zone.ry = WobbleMath.radius(finite(parts[3]));
            zone.angle = WobbleMath.clamp(finite(parts[4]), -180, 180);
            zone.shape = Integer.parseInt(parts[5]);
            if (zone.shape < 0 || zone.shape > 2) throw new IllegalArgumentException("Invalid shape");
        }
        return zone;
    }

    private static float finite(String value) {
        float number = Float.parseFloat(value);
        if (Float.isNaN(number) || Float.isInfinite(number)) throw new IllegalArgumentException("Invalid zone number");
        return number;
    }
}
