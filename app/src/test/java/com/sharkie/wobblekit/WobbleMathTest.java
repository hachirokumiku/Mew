package com.sharkie.wobblekit;

public final class WobbleMathTest {
    private static void near(float actual, float expected) {
        if (Math.abs(actual - expected) > .0001f) {
            throw new AssertionError("Expected " + expected + ", got " + actual);
        }
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
        near(WobbleMath.targetX(2 * WobbleMath.GRAVITY, .2f), -.07f);
        near(WobbleMath.targetY(-WobbleMath.GRAVITY, .2f, .5f), -.035f);
        float position = 0, velocity = 0;
        float target = WobbleMath.targetX(WobbleMath.GRAVITY, .2f);
        for (int i = 0; i < 500; i++) {
            velocity = WobbleMath.velocity(position, velocity, target, .016f);
            position += velocity * .016f;
        }
        near(position, target);
        for (int i = 0; i < 500; i++) {
            velocity = WobbleMath.velocity(position, velocity, 0, .016f);
            position += velocity * .016f;
        }
        near(position, 0);
        System.out.println("WobbleMath tests passed");
    }
}
