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
        near(WobbleMath.pinchRadius(.1f, 100, 200), .2f);
        near(WobbleMath.pinchRadius(.2f, 100, 200), .4f);
        near(WobbleMath.pinchRadius(.2f, 100, 5), .04f);
        near(WobbleMath.pinchRadius(.4f, 100, 200), .5f);
        near(WobbleMath.pinchRadius(.2f, 0, 100), .2f);
        check(WobbleMath.imageSampleSize(2048, 1024, 2048) == 1, "Small images must not be reduced");
        check(WobbleMath.imageSampleSize(2049, 2048, 2048) == 2, "Oversized images need downsampling");
        check(WobbleMath.imageSampleSize(12000, 8000, 2048) == 8, "Large images must fit the texture limit");
        check(WobbleMath.imageSampleSize(1, Integer.MAX_VALUE, 2048) == 1048576, "Sampling must not overflow");
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
        filter.update(0, WobbleMath.GRAVITY, 0, 1_000_000_000L, 0);
        near(filter.linearX, 0);
        near(filter.linearY, 0);
        near(filter.dt, 0);
        filter.update(4, WobbleMath.GRAVITY, 0, 1_020_000_000L, 0);
        check(filter.linearX > 3.8f, "A shake must produce a substantial motion signal");
        check(WobbleMath.motionImpulse(filter.linearX, .17f, 1, filter.dt) > .035f,
              "A shake must produce a visible spring impulse");
        filter.update(0, WobbleMath.GRAVITY, 0, 1_040_000_000L, 1);
        near(filter.linearX, 0);
        near(filter.linearY, 0);
        near(filter.dt, 0);
        near(filter.gravityX, -WobbleMath.GRAVITY);
        filter.update(0, 0, 0, 2_000_000_000L, 1);
        near(filter.linearX, 0);
        near(filter.dt, 0);
        filter.reset();
        filter.update(4, 3, 0, 3_000_000_000L, 0);
        near(filter.linearX, 0);
        near(filter.dt, 0);
        filter.reset();
        position = velocity = 0;
        float maxDisplacement = 0;
        for (int i = 0; i < 100; i++) {
            float acceleration = i >= 5 && i < 10 ? 5 : 0;
            filter.update(acceleration, 0, 0, 4_000_000_000L + i * 20_000_000L, 0);
            velocity -= WobbleMath.motionImpulse(filter.linearX, .17f, 1, filter.dt);
            float motionTarget = WobbleMath.targetX(filter.gravityX, .17f, 1);
            for (int j = 0; j < 2; j++) {
                velocity = WobbleMath.velocity(position, velocity, motionTarget, 85, 12, .01f);
                position += velocity * .01f;
                maxDisplacement = Math.max(maxDisplacement, Math.abs(position));
            }
        }
        check(maxDisplacement > .005f, "Simulated phone movement must displace the mesh");
        filter.reset();
        filter.update(0, 0, WobbleMath.GRAVITY, 6_000_000_000L, 0);
        filter.update(0, 0, WobbleMath.GRAVITY + 4, 6_020_000_000L, 0);
        check(filter.linearZ > 3.8f, "Forward movement must be detected");
        JellyZone depthZone = new JellyZone(.5f, .5f, .2f);
        depthZone.depthImpulse(filter.linearZ, 1, filter.dt);
        float maxDepthDeformation = 0;
        for (int i = 0; i < 100; i++) {
            depthZone.step(0, 0, 85, 12, .01f);
            maxDepthDeformation = Math.max(maxDepthDeformation,
                    Math.abs(depthZone.meshX(.1f, 0, depthZone.weight(.1f, 0))));
        }
        check(maxDepthDeformation > .0005f,
              "Forward movement must create visible deformation");
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
        JellyZone old = JellyZone.parse(".25,.75,.2");
        near(old.rx, .2f);
        near(old.ry, .2f);
        near(old.angle, 0);
        check(old.shape == 0, "Old circular zones must retain smooth falloff");
        JellyZone shaped = new JellyZone(.3f, .4f, .2f);
        shaped.ry = .1f;
        shaped.angle = 90;
        shaped.shape = 2;
        JellyZone restored = JellyZone.parse(shaped.serialize());
        near(restored.rx, .2f);
        near(restored.ry, .1f);
        near(restored.angle, 90);
        check(restored.shape == 2, "Zone shape must survive saving");
        near(restored.normalizedDistance(0, .1f), .5f);
        near(restored.normalizedDistance(.1f, 0), 1f);
        near(restored.weight(.1f, 0), 0);
        restored.shape = 0;
        float smooth = restored.weight(0, .1f);
        restored.shape = 1;
        float broad = restored.weight(0, .1f);
        restored.shape = 2;
        float pointed = restored.weight(0, .1f);
        check(broad >= smooth && smooth > pointed, "Falloff profiles must be distinct");
        JellyZone jelly = new JellyZone(.5f, .5f, .2f);
        jelly.impulse(.2f, 0);
        float largestSquash = 0;
        for (int i = 0; i < 80; i++) {
            jelly.step(0, 0, 85, 8, .01f);
            float w = jelly.weight(.1f, 0);
            largestSquash = Math.max(largestSquash, Math.abs(jelly.meshX(.1f, 0, w) - w * jelly.dx));
        }
        check(largestSquash > .0005f, "Jelly motion must change the zone shape, not just translate it");
        for (int i = 0; i < 2000; i++) jelly.step(0, 0, 85, 8, .01f);
        near(jelly.dx, 0);
        near(jelly.meshX(.1f, 0, jelly.weight(.1f, 0)), 0);
        jelly = new JellyZone(.5f, .5f, .2f);
        jelly.impulse(.2f, 0);
        float positivePeak = 0, negativePeak = 0, laterMotion = 0;
        for (int i = 0; i < 200; i++) {
            jelly.step(0, 0, 85, 12, .01f);
            positivePeak = Math.max(positivePeak, jelly.dx);
            negativePeak = Math.min(negativePeak, jelly.dx);
            if (i >= 50 && i < 150) laterMotion = Math.max(laterMotion, Math.abs(jelly.dx));
        }
        check(positivePeak > .01f, "A modest impulse must visibly move the jelly");
        check(negativePeak < -.15f * positivePeak, "Default jelly must bounce back with a visible overshoot");
        check(laterMotion > .1f * positivePeak, "Jelly motion must persist after the initial movement");
        for (int spring : new int[] {20, 180}) for (int damping : new int[] {2, 30}) {
            JellyZone extreme = new JellyZone(.5f, .5f, .2f);
            extreme.impulse(1000, -1000);
            for (int i = 0; i < 5000; i++) {
                extreme.step(0, 0, spring, damping, .01f);
                check(Float.isFinite(extreme.dx) && Float.isFinite(extreme.dy), "Jelly must remain finite");
                check(Math.hypot(extreme.dx, extreme.dy) <= .13001f, "Jelly displacement must remain bounded");
            }
            near(extreme.dx, 0);
            near(extreme.dy, 0);
        }
        boolean invalid = false;
        try { JellyZone.parse(".5,.5,NaN"); } catch (IllegalArgumentException ex) { invalid = true; }
        check(invalid, "Non-finite saved zone values must be rejected");
        System.out.println("WobbleMath tests passed");
    }
}
