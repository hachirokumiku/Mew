package com.sharkie.wobblekit;

public final class WobbleMathTest {
    private static void near(float actual, float expected) {
        if (!Float.isFinite(actual) || Math.abs(actual - expected) > .0001f) {
            throw new AssertionError("Expected " + expected + ", got " + actual);
        }
    }

    private static void check(boolean value, String message) {
        if (!value) throw new AssertionError(message);
    }

    private static void axes(int rotation, float expectedX, float expectedY) {
        float[] result = WobbleMath.screenAxes(2, 3, rotation);
        near(result[0], expectedX);
        near(result[1], expectedY);
    }

    public static void main(String[] args) {
        near(WobbleMath.radius(0), .04f);
        near(WobbleMath.radius(.2f), .2f);
        near(WobbleMath.radius(2), .5f);
        near(WobbleMath.handleX(0, .5f), .5f);
        near(WobbleMath.handleX(1, .5f), .5f);
        near(WobbleMath.distance(.5f, .6f, .5f, .5f, 2), .2f);
        axes(0, 2, 3);
        axes(1, -3, 2);
        axes(2, -2, -3);
        axes(3, 3, -2);
        near(WobbleMath.targetX(2 * WobbleMath.GRAVITY, .2f, 1), -.1f);
        near(WobbleMath.targetY(-WobbleMath.GRAVITY, .2f, .5f, 1), -.05f);
        near(WobbleMath.targetX(WobbleMath.GRAVITY, .2f, 0), 0);
        near(WobbleMath.targetX(WobbleMath.GRAVITY, .2f, 2), -.2f);
        near(WobbleMath.motionImpulse(WobbleMath.GRAVITY, .2f, 0, .02f), 0);
        near(WobbleMath.motionImpulse(WobbleMath.GRAVITY, .2f, 1, .02f), .12f);
        near(WobbleMath.velocity(0, 0, .1f, 180, 12, .01f), .18f);
        near(WobbleMath.velocity(0, 1, 0, 85, 30, .01f), .7f);
        float position = 0, velocity = 0;
        float target = WobbleMath.targetX(WobbleMath.GRAVITY, .2f, 1);
        for (int i = 0; i < 500; i++) {
            velocity = WobbleMath.velocity(position, velocity, target, 85, 12, .016f);
            position += velocity * .016f;
        }
        near(position, target);
        for (int i = 0; i < 500; i++) {
            velocity = WobbleMath.velocity(position, velocity, 0, 85, 12, .016f);
            position += velocity * .016f;
        }
        near(position, 0);
        MotionFilter filter = new MotionFilter();
        filter.update(0, WobbleMath.GRAVITY, 1_000_000_000L, 0);
        near(filter.linearX, 0);
        near(filter.linearY, 0);
        near(filter.dt, 0);
        filter.update(4, WobbleMath.GRAVITY, 1_020_000_000L, 0);
        check(filter.linearX > 3.8f, "A shake must produce a substantial motion signal");
        check(WobbleMath.motionImpulse(filter.linearX, .17f, 1, filter.dt) > .035f,
              "A shake must produce a visible spring impulse");
        filter.update(0, WobbleMath.GRAVITY, 1_040_000_000L, 1);
        near(filter.linearX, 0);
        near(filter.linearY, 0);
        near(filter.dt, 0);
        near(filter.gravityX, -WobbleMath.GRAVITY);
        filter.update(0, 0, 2_000_000_000L, 1);
        near(filter.linearX, 0);
        near(filter.dt, 0);
        filter.reset();
        filter.update(4, 3, 3_000_000_000L, 0);
        near(filter.linearX, 0);
        near(filter.dt, 0);
        filter.reset();
        position = velocity = 0;
        float maxDisplacement = 0;
        for (int i = 0; i < 100; i++) {
            float acceleration = i >= 5 && i < 10 ? 5 : 0;
            filter.update(acceleration, 0, 4_000_000_000L + i * 20_000_000L, 0);
            velocity -= WobbleMath.motionImpulse(filter.linearX, .17f, 1, filter.dt);
            float motionTarget = WobbleMath.targetX(filter.gravityX, .17f, 1);
            for (int j = 0; j < 2; j++) {
                velocity = WobbleMath.velocity(position, velocity, motionTarget, 85, 12, .01f);
                position += velocity * .01f;
                maxDisplacement = Math.max(maxDisplacement, Math.abs(position));
            }
        }
        check(maxDisplacement > .005f, "Simulated phone movement must displace the mesh");
        for (int spring : new int[] {20, 180}) for (int damping : new int[] {2, 30}) {
            position = 0;
            velocity = 0;
            for (int i = 0; i < 4000; i++) {
                velocity = WobbleMath.velocity(position, velocity, .3f, spring, damping, .01f);
                position += velocity * .01f;
                check(Float.isFinite(position) && Math.abs(position) < 1, "Spring must stay stable at tuning limits");
            }
            near(position, .3f);
        }
        System.out.println("WobbleMath tests passed");
    }
}
