package com.sharkie.wobblekit;

public final class ZoneEditorTest {
    private static void check(boolean value, String message) {
        if (!value) throw new AssertionError(message);
    }

    public static void main(String[] args) {
        JellyZone zone = new JellyZone(.23456f, .87654f, .12345f);
        zone.ry = .09876f;
        zone.angle = -23.456f;
        zone.shape = 1;
        String original = zone.serialize();
        ZoneEditSession edit = new ZoneEditSession(zone);
        zone.x = .001f;
        zone.y = .999f;
        zone.rx = .04f;
        zone.ry = .5f;
        zone.angle = 179.9f;
        zone.shape = 2;
        JellyZone saved = JellyZone.parse(zone.serialize());
        check(saved.x == zone.x && saved.y == zone.y && saved.rx == zone.rx
                && saved.ry == zone.ry && saved.angle == zone.angle && saved.shape == zone.shape,
                "Edited geometry must survive serialization");
        edit.restore();
        check(zone.serialize().equals(original), "Cancel must restore exact original values");

        JellyZone jelly = new JellyZone(.5f, .5f, .17f);
        jelly.impulse(0, jelly.radius() * 2f * PhysicsDefaults.STRENGTH / 100f);
        float positive = 0, negative = 0, late = 0, previous = 0;
        int crossings = 0;
        for (int i = 0; i < 300; i++) {
            jelly.step(PhysicsDefaults.SPRING, PhysicsDefaults.DAMPING, .01f);
            positive = Math.max(positive, jelly.dy);
            negative = Math.min(negative, jelly.dy);
            if (i >= 50 && i < 150) late = Math.max(late, Math.abs(jelly.dy));
            if (i > 0 && jelly.dy * previous < 0) crossings++;
            previous = jelly.dy;
            check(Math.hypot(jelly.dx, jelly.dy) <= .17f * .2201f, "Jelly motion must remain bounded");
        }
        check(positive > .01f && negative < -positive * .2f, "Default jelly must visibly rebound");
        check(crossings >= 4 && late > positive * .1f, "Default jelly must bounce repeatedly");
        for (int i = 0; i < 2500; i++) jelly.step(PhysicsDefaults.SPRING, PhysicsDefaults.DAMPING, .01f);
        check(Math.abs(jelly.dy) < .0001f, "Default jelly must settle");
        System.out.println("Zone editor and jelly defaults tests passed");
    }
}
