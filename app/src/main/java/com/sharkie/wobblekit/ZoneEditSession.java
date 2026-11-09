package com.sharkie.wobblekit;

final class ZoneEditSession {
    private final JellyZone zone;
    private final float x, y, rx, ry, angle;
    private final int shape;

    ZoneEditSession(JellyZone zone) {
        this.zone = zone;
        x = zone.x;
        y = zone.y;
        rx = zone.rx;
        ry = zone.ry;
        angle = zone.angle;
        shape = zone.shape;
    }

    void restore() {
        zone.x = x;
        zone.y = y;
        zone.rx = rx;
        zone.ry = ry;
        zone.angle = angle;
        zone.shape = shape;
    }
}
