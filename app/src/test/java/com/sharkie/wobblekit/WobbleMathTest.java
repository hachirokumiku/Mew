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

    private static float simulatePhoneJiggle(int sampleEverySteps, float strength) {
        JellyZone zone = new JellyZone(.5f, .5f, .17f);
        MotionFilter filter = new MotionFilter();
        float max = 0;
        for (int i = 0; i < 300; i++) {
            if (i % sampleEverySteps == 0) {
                float movement = i >= 10 && i < 20 ? 4 : 0;
                filter.update(movement, 0, WobbleMath.GRAVITY,
                        10_000_000_000L + i * 10_000_000L, 0);
                zone.impulse(-WobbleMath.motionImpulse(filter.linearX, zone.radius(), strength, filter.dt),
                        WobbleMath.motionImpulse(filter.linearY, zone.radius(), strength, filter.dt));
                zone.depthImpulse(filter.linearZ, strength, filter.dt);
            }
            zone.step(WobbleMath.targetX(filter.gravityX, zone.radius(), strength),
                    WobbleMath.targetY(filter.gravityY, zone.radius(), 1, strength),
                    85, 12, .01f);
            max = Math.max(max, Math.abs(zone.dx));
            check(Math.abs(zone.dx) <= zone.radius() * .2201f, "Motion must remain compact");
        }
        check(Math.abs(zone.dx) < .001f, "Jiggle must settle after phone movement");
        return max;
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
        near(WobbleMath.targetX(2 * WobbleMath.GRAVITY, .2f, 1), -.036f);
        near(WobbleMath.targetY(-WobbleMath.GRAVITY, .2f, .5f, 1), -.018f);
        near(WobbleMath.targetX(WobbleMath.GRAVITY, .2f, 0), 0);
        near(WobbleMath.targetX(WobbleMath.GRAVITY, .2f, 2), -.072f);
        near(WobbleMath.motionImpulse(WobbleMath.GRAVITY, .2f, 0, .02f), 0);
        near(WobbleMath.motionImpulse(WobbleMath.GRAVITY, .2f, 1, .02f),
                .08f * (WobbleMath.GRAVITY - .15f) / WobbleMath.GRAVITY);
        near(WobbleMath.motionImpulse(.1f, .2f, 1, .02f), 0);
        near(WobbleMath.motionImpulse(-.1f, .2f, 1, .02f), 0);
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
        check(WobbleMath.motionImpulse(filter.linearX, .17f, 1, filter.dt) > .02f,
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
        check(maxDepthDeformation > .0003f,
              "Forward movement must create visible deformation");
        float jiggle50 = simulatePhoneJiggle(2, 1);
        float jiggle100 = simulatePhoneJiggle(1, 1);
        check(jiggle50 > .005f && jiggle100 > .005f, "Phone movement must produce visible jiggle");
        check(jiggle50 / jiggle100 > .7f && jiggle50 / jiggle100 < 1.3f,
                "Jiggle must be consistent across sensor sampling rates");
        near(simulatePhoneJiggle(2, 0), 0);
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
        check(negativePeak < -.1f * positivePeak, "Default jelly must bounce back with a visible overshoot");
        check(laterMotion > .1f * positivePeak, "Jelly motion must persist after the initial movement");
        JellyZone bounce = new JellyZone(.5f, .5f, .17f);
        bounce.impulse(0, bounce.radius() * 2);
        float weight = bounce.weight(0, .085f);
        float previousCenter = 0, maxLag = 0, peak = 0, oppositePeak = 0;
        int crossings = 0;
        for (int i = 0; i < 300; i++) {
            bounce.step(0, 0, 85, 12, .01f);
            float center = bounce.meshY(0, 0, 1);
            float rim = bounce.meshY(0, .085f, weight) / weight;
            maxLag = Math.max(maxLag, Math.abs(center - rim));
            peak = Math.max(peak, center);
            oppositePeak = Math.min(oppositePeak, center);
            if (i > 0 && center * previousCenter < 0) crossings++;
            previousCenter = center;
        }
        check(peak > .01f, "Tap must produce visible displacement");
        check(oppositePeak < -peak * .15f, "Jelly must rebound noticeably");
        check(crossings >= 4, "Jelly must bounce repeatedly before settling");
        check(maxLag > .0005f, "Interior and outer region must move out of phase");
        JellyZone held = new JellyZone(.5f, .5f, .17f);
        float heldTarget = WobbleMath.targetX(WobbleMath.GRAVITY * .5f, held.radius(), 1);
        float minHeld = 0;
        for (int i = 0; i < 3000; i++) {
            held.step(heldTarget, 0, 85, 12, .01f);
            minHeld = Math.min(minHeld, held.dx);
        }
        check(minHeld < heldTarget * 1.1f, "Tilting must bounce past its resting position");
        near(held.dx, heldTarget);
        near(held.meshX(.085f, 0, held.weight(.085f, 0)),
                heldTarget * held.weight(.085f, 0));
        JellyZone noInput = new JellyZone(.5f, .5f, .17f);
        noInput.impulse(0, 0);
        noInput.depthImpulse(5, 0, .02f);
        for (int i = 0; i < 100; i++) noInput.step(0, 0, 85, 12, .01f);
        near(noInput.meshX(.085f, 0, noInput.weight(.085f, 0)), 0);
        near(noInput.meshY(0, .085f, noInput.weight(0, .085f)), 0);
        JellyZone narrow = new JellyZone(.5f, .5f, .4f);
        narrow.ry = .04f;
        narrow.angle = 45;
        narrow.impulse(50, -50);
        for (int i = 0; i < 300; i++) {
            narrow.step(0, 0, 85, 12, .01f);
            check(Math.hypot(narrow.dx, narrow.dy) <= .00881,
                    "Narrow zones must stay bounded by their shortest dimension");
        }
        for (int spring : new int[] {20, 180}) for (int damping : new int[] {2, 30}) {
            JellyZone extreme = new JellyZone(.5f, .5f, .2f);
            extreme.impulse(1000, -1000);
            for (int i = 0; i < 5000; i++) {
                extreme.step(0, 0, spring, damping, .01f);
                check(Float.isFinite(extreme.dx) && Float.isFinite(extreme.dy), "Jelly must remain finite");
                check(Math.hypot(extreme.dx, extreme.dy) <= .04401f, "Jelly displacement must remain bounded");
                float meshX = extreme.meshX(.1f, 0, extreme.weight(.1f, 0));
                float meshY = extreme.meshY(.1f, 0, extreme.weight(.1f, 0));
                check(Float.isFinite(meshX) && Float.isFinite(meshY)
                        && Math.hypot(meshX, meshY) < .3f, "Mesh deformation must remain bounded");
            }
            near(extreme.dx, 0);
            near(extreme.dy, 0);
            near(extreme.meshX(.1f, 0, extreme.weight(.1f, 0)), 0);
            near(extreme.meshY(.1f, 0, extreme.weight(.1f, 0)), 0);
        }
        boolean invalid = false;
        try { JellyZone.parse(".5,.5,NaN"); } catch (IllegalArgumentException ex) { invalid = true; }
        check(invalid, "Non-finite saved zone values must be rejected");
        System.out.println("WobbleMath tests passed");
    }
}
