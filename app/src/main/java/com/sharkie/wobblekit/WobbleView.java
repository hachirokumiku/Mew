package com.sharkie.wobblekit;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.view.MotionEvent;
import android.view.View;
import java.util.ArrayList;

public class WobbleView extends View {
    public Bitmap picture;
    public final ArrayList<JellyZone> zones = new ArrayList<>();
    public boolean editing = true;
    private final Paint paint = new Paint(3);
    private final Paint overlay = new Paint(3);
    private static final int MESH_WIDTH = 80, MESH_HEIGHT = 120;
    private final float[] vertices = new float[(MESH_WIDTH + 1) * (MESH_HEIGHT + 1) * 2];
    private long last = System.nanoTime();
    private float previousX, previousY, tiltX, tiltY;
    private JellyZone selected;
    private JellyZone activeZone;
    private boolean resizing;
    private float handleDirection;
    private int activePointerId = MotionEvent.INVALID_POINTER_ID;
    private int secondaryPointerId = MotionEvent.INVALID_POINTER_ID;
    private float pinchDistance, pinchRx, pinchRy;
    private float downX, downY;
    private boolean moved;
    private float strength = 1f, spring = 85f, damping = 12f;

    public WobbleView(Context context) {
        super(context);
    }

    public void load(Bitmap bitmap) {
        picture = bitmap;
        zones.clear();
        selected = null;
        activeZone = null;
        invalidate();
    }

    void resetClock() { last = System.nanoTime(); }

    public void addZone() {
        activeZone = new JellyZone(.5f, .5f, .17f);
        zones.add(activeZone);
        invalidate();
    }

    JellyZone activeZone() { return activeZone; }

    void removeActiveZone() {
        if (activeZone != null) zones.remove(activeZone);
        activeZone = null;
        selected = null;
        invalidate();
    }

    private float pointerDistance(MotionEvent event, int first, int second) {
        int a = event.findPointerIndex(first), b = event.findPointerIndex(second);
        if (a < 0 || b < 0) return 0;
        return (float) Math.hypot(event.getX(a) - event.getX(b), event.getY(a) - event.getY(b));
    }

    public void setTilt(float x, float y) {
        tiltX = x;
        tiltY = y;
    }

    public void loadPhysics(SharedPreferences prefs) {
        strength = WobbleMath.clamp(prefs.getInt("strength", 100), 0, 300) / 100f;
        spring = WobbleMath.clamp(prefs.getInt("spring", 85), 20, 180);
        damping = WobbleMath.clamp(prefs.getInt("damping", 12), 2, 30);
    }

    public void applyMotion(float x, float y, float depth, float dt) {
        if (editing || picture == null) return;
        for (JellyZone z : zones) {
            z.impulse(-WobbleMath.motionImpulse(x, z.radius(), strength, dt),
                    WobbleMath.motionImpulse(y, z.radius(), strength, dt));
            z.depthImpulse(depth, strength, dt);
        }
    }

    public void tick() {
        long now = System.nanoTime();
        float dt = Math.min(.04f, (now - last) / 1e9f);
        last = now;
        float aspect = picture == null ? 1f : (float) picture.getWidth() / picture.getHeight();
        int steps = Math.max(1, (int) Math.ceil(dt / .01f));
        float step = dt / steps;
        for (JellyZone z : zones) {
            float targetX = editing ? 0 : WobbleMath.targetX(tiltX, z.radius(), strength);
            float targetY = editing ? 0 : WobbleMath.targetY(tiltY, z.radius(), aspect, strength) / aspect;
            for (int i = 0; i < steps; i++) {
                z.step(targetX, targetY, spring, damping, step);
            }
        }
        invalidate();
    }

    @Override protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        canvas.drawColor(Color.rgb(18, 19, 26));
        if (picture == null) {
            paint.setColor(Color.WHITE);
            paint.setTextSize(42);
            canvas.drawText("Pick an image :3", 25, getHeight() / 2f, paint);
            return;
        }
        float scale = Math.min((float) getWidth() / picture.getWidth(), (float) getHeight() / picture.getHeight());
        float iw = picture.getWidth() * scale, ih = picture.getHeight() * scale;
        float ox = (getWidth() - iw) / 2, oy = (getHeight() - ih) / 2;
        int nx = MESH_WIDTH, ny = MESH_HEIGHT;
        int index = 0;
        for (int y = 0; y <= ny; y++) for (int x = 0; x <= nx; x++) {
            float u = x / (float) nx, v = y / (float) ny;
            float px = u * iw, py = v * ih;
            for (JellyZone z : zones) {
                float zx = u - z.x, zy = (v - z.y) * ih / iw;
                float weight = z.weight(zx, zy);
                if (weight > 0) {
                    px += z.meshX(zx, zy, weight) * iw;
                    py += z.meshY(zx, zy, weight) * iw;
                }
            }
            vertices[index++] = px;
            vertices[index++] = py;
        }
        canvas.save();
        canvas.translate(ox, oy);
        paint.setColor(Color.WHITE);
        canvas.drawBitmapMesh(picture, nx, ny, vertices, 0, null, 0, paint);
        if (editing) {
            float density = getResources().getDisplayMetrics().density;
            overlay.setColor(0xff73e7ff);
            overlay.setStyle(Paint.Style.STROKE);
            overlay.setStrokeWidth(2 * density);
            for (JellyZone z : zones) {
                overlay.setColor(z == activeZone ? Color.WHITE : 0xff73e7ff);
                canvas.save();
                canvas.rotate(z.angle, z.x * iw, z.y * ih);
                canvas.drawOval((z.x-z.rx)*iw, z.y*ih-z.ry*iw, (z.x+z.rx)*iw, z.y*ih+z.ry*iw, overlay);
                overlay.setStyle(Paint.Style.FILL);
                float handle = z.handleDirection(ih / iw) * z.rx;
                canvas.drawCircle((z.x+handle)*iw, z.y*ih, 6 * density, overlay);
                overlay.setStyle(Paint.Style.STROKE);
                canvas.restore();
            }
        }
        canvas.restore();
    }

    @Override public boolean onTouchEvent(MotionEvent event) {
        if (picture == null || getWidth() == 0 || getHeight() == 0) return true;
        float scale = Math.min((float) getWidth() / picture.getWidth(), (float) getHeight() / picture.getHeight());
        float iw = picture.getWidth() * scale, ih = picture.getHeight() * scale;
        float u = (event.getX() - (getWidth() - iw) / 2) / iw;
        float v = (event.getY() - (getHeight() - ih) / 2) / ih;
        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                activePointerId = event.getPointerId(0);
                secondaryPointerId = MotionEvent.INVALID_POINTER_ID;
                downX = event.getX();
                downY = event.getY();
                moved = false;
                selected = null;
                resizing = false;
                float best = Float.MAX_VALUE;
                if (editing) {
                    float hitRadius = 24 * getResources().getDisplayMetrics().density / iw;
                    for (int i = zones.size() - 1; i >= 0; i--) {
                        JellyZone z = zones.get(i);
                        float zx = u-z.x, zy = (v-z.y)*ih/iw;
                        float handle = z.handleDirection(ih / iw) * z.rx;
                        float hx = z.localX(zx, zy)-handle, hy = z.localY(zx, zy);
                        float d = (float)Math.hypot(hx, hy);
                        float centerDistance = (float)Math.hypot(zx, zy);
                        if (d <= hitRadius && d < centerDistance && d < best) { selected = z; best = d; resizing = true; handleDirection = Math.signum(handle); }
                    }
                }
                if (selected == null) {
                    best = Float.MAX_VALUE;
                    for (int i = zones.size() - 1; i >= 0; i--) {
                        JellyZone z = zones.get(i);
                        float d = z.normalizedDistance(u-z.x, (v-z.y)*ih/iw);
                        if (d < 1 && d < best) { selected = z; best = d; }
                    }
                }
                previousX = u;
                previousY = v;
                if (editing && selected != null) { activeZone = selected; invalidate(); }
                return true;
            case MotionEvent.ACTION_POINTER_DOWN:
                if (editing && selected != null && secondaryPointerId == MotionEvent.INVALID_POINTER_ID) {
                    secondaryPointerId = event.getPointerId(event.getActionIndex());
                    pinchDistance = pointerDistance(event, activePointerId, secondaryPointerId);
                    pinchRx = selected.rx;
                    pinchRy = selected.ry;
                    resizing = false;
                }
                return true;
            case MotionEvent.ACTION_MOVE:
                int pointerIndex = event.findPointerIndex(activePointerId);
                if (pointerIndex < 0) return true;
                if (Math.hypot(event.getX(pointerIndex) - downX, event.getY(pointerIndex) - downY)
                        > 8 * getResources().getDisplayMetrics().density) moved = true;
                u = (event.getX(pointerIndex) - (getWidth() - iw) / 2) / iw;
                v = (event.getY(pointerIndex) - (getHeight() - ih) / 2) / ih;
                if (selected != null) {
                    float dx = u - previousX, dy = v - previousY;
                    if (editing && secondaryPointerId != MotionEvent.INVALID_POINTER_ID && pinchDistance > 0) {
                        float distance = pointerDistance(event, activePointerId, secondaryPointerId);
                        selected.rx = WobbleMath.pinchRadius(pinchRx, pinchDistance, distance);
                        selected.ry = WobbleMath.pinchRadius(pinchRy, pinchDistance, distance);
                    } else if (resizing) {
                        float local = selected.localX(u-selected.x, (v-selected.y)*ih/iw) * handleDirection;
                        float newRadius = WobbleMath.radius(local);
                        selected.ry = WobbleMath.radius(selected.ry * newRadius / selected.rx);
                        selected.rx = newRadius;
                    } else if (editing) {
                        selected.x = WobbleMath.clamp(selected.x + dx, 0, 1);
                        selected.y = WobbleMath.clamp(selected.y + dy, 0, 1);
                    } else {
                        selected.impulse(dx * 35 * strength, dy * ih / iw * 35 * strength);
                    }
                }
                previousX = u;
                previousY = v;
                invalidate();
                return true;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                if (event.getActionMasked() == MotionEvent.ACTION_UP && !editing && selected != null && !moved) {
                    selected.impulse(0, selected.radius() * strength * 2f);
                }
                selected = null;
                resizing = false;
                activePointerId = MotionEvent.INVALID_POINTER_ID;
                secondaryPointerId = MotionEvent.INVALID_POINTER_ID;
                return true;
            case MotionEvent.ACTION_POINTER_UP:
                int lifted = event.getPointerId(event.getActionIndex());
                if (lifted == secondaryPointerId) {
                    secondaryPointerId = MotionEvent.INVALID_POINTER_ID;
                } else if (lifted == activePointerId && secondaryPointerId != MotionEvent.INVALID_POINTER_ID) {
                    activePointerId = secondaryPointerId;
                    secondaryPointerId = MotionEvent.INVALID_POINTER_ID;
                    int remaining = event.findPointerIndex(activePointerId);
                    if (remaining >= 0) {
                        previousX = (event.getX(remaining) - (getWidth() - iw) / 2) / iw;
                        previousY = (event.getY(remaining) - (getHeight() - ih) / 2) / ih;
                    }
                } else if (lifted == activePointerId) {
                    selected = null;
                    activePointerId = MotionEvent.INVALID_POINTER_ID;
                }
                return true;
            default:
                return true;
        }
    }
}
