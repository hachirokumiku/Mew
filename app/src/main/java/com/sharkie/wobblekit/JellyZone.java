package com.sharkie.wobblekit;

final class JellyZone {
    float x, y, rx, ry, angle;
    int shape;
    float dx, dy, vx, vy;
    private float rimX, rimY, rimVx, rimVy;
    private float pulse, pulseVelocity;
    private float squashX, squashY, squashVx, squashVy;
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
        return weightAt(normalizedDistance(x, y));
    }

    float weightAt(float distance) {
        if (distance >= 1) return 0;
        float remaining = 1 - distance;
        if (shape == 1) return remaining;
        if (shape == 2) return remaining * remaining * remaining;
        return remaining * remaining * (1 + 2 * distance);
    }

    void impulse(float x, float y) {
        float shortSide = Math.min(rx, ry);
        squashVx = WobbleMath.clamp(squashVx + x / shortSide * .7f, -2f, 2f);
        squashVy = WobbleMath.clamp(squashVy + y / shortSide * .7f, -2f, 2f);
        vx += x;
        vy += y;
        float speed = (float) Math.hypot(vx, vy);
        float limit = shortSide * 3f;
        if (speed > limit) {
            vx *= limit / speed;
            vy *= limit / speed;
        }
    }

    void depthImpulse(float acceleration, float strength, float dt) {
        float force = WobbleMath.clamp(WobbleMath.sensorMotion(acceleration) / WobbleMath.GRAVITY, -2f, 2f);
        pulseVelocity = WobbleMath.clamp(pulseVelocity + force * strength * dt * 16f, -1.5f, 1.5f);
    }

    void step(float spring, float damping, float dt) {
        float stiffness = spring * .9f;
        float drag = damping * .45f;
        float limit = Math.min(rx, ry) * .22f;
        vx = WobbleMath.velocity(dx, vx, 0, stiffness, drag, dt);
        vy = WobbleMath.velocity(dy, vy, 0, stiffness, drag, dt);
        dx += vx * dt;
        dy += vy * dt;
        float displacement = (float) Math.hypot(dx, dy);
        if (displacement > limit) {
            dx *= limit / displacement;
            dy *= limit / displacement;
            float outwardVelocity = (vx * dx + vy * dy) / limit;
            if (outwardVelocity > 0) {
                vx -= outwardVelocity * dx / limit;
                vy -= outwardVelocity * dy / limit;
            }
        }
        rimVx = WobbleMath.velocity(rimX, rimVx, dx, stiffness * .9f, drag * 1.2f, dt);
        rimVy = WobbleMath.velocity(rimY, rimVy, dy, stiffness * .9f, drag * 1.2f, dt);
        rimX += rimVx * dt;
        rimY += rimVy * dt;
        float rimLength = (float) Math.hypot(rimX, rimY);
        if (rimLength > limit) {
            rimX *= limit / rimLength;
            rimY *= limit / rimLength;
            float outwardVelocity = (rimVx * rimX + rimVy * rimY) / limit;
            if (outwardVelocity > 0) {
                rimVx -= outwardVelocity * rimX / limit;
                rimVy -= outwardVelocity * rimY / limit;
            }
        }
        pulseVelocity = WobbleMath.velocity(pulse, pulseVelocity, 0, stiffness * .65f, drag * .8f, dt);
        pulse += pulseVelocity * dt;
        if (pulse > .12f) { pulse = .12f; pulseVelocity = Math.min(0, pulseVelocity); }
        if (pulse < -.12f) { pulse = -.12f; pulseVelocity = Math.max(0, pulseVelocity); }
        squashVx = WobbleMath.velocity(squashX, squashVx, 0, stiffness * 1.4f, drag, dt);
        squashVy = WobbleMath.velocity(squashY, squashVy, 0, stiffness * 1.4f, drag, dt);
        squashX += squashVx * dt;
        squashY += squashVy * dt;
        float squashLength = (float) Math.hypot(squashX, squashY);
        if (squashLength > .12f) {
            squashX *= .12f / squashLength;
            squashY *= .12f / squashLength;
            float outward = (squashVx * squashX + squashVy * squashY) / .12f;
            if (outward > 0) {
                squashVx -= outward * squashX / .12f;
                squashVy -= outward * squashY / .12f;
            }
        }
    }

    float meshX(float x, float y, float weight) {
        return meshAtX(x, y, weight, normalizedDistance(x, y));
    }

    float meshAtX(float x, float y, float weight, float distance) {
        return weight * (dx + .25f * distance * (rimX - dx) + x * (pulse + squashX) + y * squashY);
    }

    float meshY(float x, float y, float weight) {
        return meshAtY(x, y, weight, normalizedDistance(x, y));
    }

    float meshAtY(float x, float y, float weight, float distance) {
        return weight * (dy + .25f * distance * (rimY - dy) + y * (pulse - squashX) + x * squashY);
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
