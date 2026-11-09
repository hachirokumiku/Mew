package com.sharkie.wobblekit;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
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
    private float previousX, previousY;
    private JellyZone selected;
    private JellyZone activeZone;
    private Runnable selectionListener;
    private boolean resizing;
    private float handleDirection;
    private int activePointerId = MotionEvent.INVALID_POINTER_ID;
    private int secondaryPointerId = MotionEvent.INVALID_POINTER_ID;
    private float pinchDistance, pinchRx, pinchRy;
    private float downX, downY;
    private boolean moved;
    private float strength = PhysicsDefaults.STRENGTH / 100f, spring = PhysicsDefaults.SPRING, damping = PhysicsDefaults.DAMPING;
    private boolean crop;
    private float zoom = 1f, frameX = .5f, frameY = .5f;
    private float previewAspect;

    public WobbleView(Context context) {
        super(context);
    }

    public void load(Bitmap bitmap) {
        picture = bitmap;
        zones.clear();
        selected = null;
        activeZone = null;
        notifySelectionChanged();
        invalidate();
    }

    void resetClock() { last = System.nanoTime(); }

    public void addZone() {
        RectF viewport = viewport();
        ImageFrame frame = imageFrame(viewport);
        activeZone = new JellyZone(WobbleMath.clamp(frame.imageX(viewport.centerX()), 0, 1),
                WobbleMath.clamp(frame.imageY(viewport.centerY()), 0, 1), .17f);
        zones.add(activeZone);
        notifySelectionChanged();
        invalidate();
    }

    JellyZone activeZone() { return activeZone; }

    void setSelectionListener(Runnable listener) { selectionListener = listener; }

    private void notifySelectionChanged() {
        if (selectionListener != null) selectionListener.run();
    }

    void showZonePreview(WobbleView source, JellyZone zone) {
        picture = source.picture;
        zones.clear();
        zones.addAll(source.zones);
        activeZone = zone;
        crop = source.crop;
        zoom = source.zoom;
        frameX = source.frameX;
        frameY = source.frameY;
        previewAspect = source.previewAspect;
        invalidate();
    }

    void removeActiveZone() {
        if (activeZone != null) zones.remove(activeZone);
        activeZone = null;
        selected = null;
        notifySelectionChanged();
        invalidate();
    }

    private float pointerDistance(MotionEvent event, int first, int second) {
        int a = event.findPointerIndex(first), b = event.findPointerIndex(second);
        if (a < 0 || b < 0) return 0;
        return (float) Math.hypot(event.getX(a) - event.getX(b), event.getY(a) - event.getY(b));
    }

    public void loadPhysics(SharedPreferences prefs) {
        strength = WobbleMath.clamp(prefs.getInt("strength", PhysicsDefaults.STRENGTH), 0, 300) / 100f;
        spring = WobbleMath.clamp(prefs.getInt("spring", PhysicsDefaults.SPRING), 20, 180);
        damping = WobbleMath.clamp(prefs.getInt("damping", PhysicsDefaults.DAMPING), 2, 30);
    }

    void setPreviewAspect(float aspect) { previewAspect = aspect; invalidate(); }

    void loadFrame(SharedPreferences prefs) {
        setFrame(prefs.getBoolean("frameCrop", false), prefs.getInt("frameZoom", 100),
                prefs.getInt("frameX", 50), prefs.getInt("frameY", 50));
    }

    void setFrame(boolean crop, int zoom, int x, int y) {
        this.crop = crop;
        this.zoom = WobbleMath.clamp(zoom, 100, 300) / 100f;
        frameX = WobbleMath.clamp(x, 0, 100) / 100f;
        frameY = WobbleMath.clamp(y, 0, 100) / 100f;
        invalidate();
    }

    private RectF viewport() {
        float width = getWidth(), height = getHeight();
        if (previewAspect > 0 && width > 0 && height > 0) {
            if (width / height > previewAspect) width = height * previewAspect;
            else height = width / previewAspect;
        }
        return new RectF((getWidth() - width) / 2, (getHeight() - height) / 2,
                (getWidth() + width) / 2, (getHeight() + height) / 2);
    }

    private ImageFrame imageFrame(RectF viewport) {
        return new ImageFrame(picture.getWidth(), picture.getHeight(), viewport.left, viewport.top,
                viewport.width(), viewport.height(), crop, zoom, frameX, frameY);
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
        int steps = Math.max(1, (int) Math.ceil(dt / .01f));
        float step = dt / steps;
        for (JellyZone z : zones) {
            for (int i = 0; i < steps; i++) {
                z.step(spring, damping, step);
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
        RectF viewport = viewport();
        if (viewport.width() <= 0 || viewport.height() <= 0) return;
        ImageFrame frame = imageFrame(viewport);
        float iw = frame.width, ih = frame.height;
        float ox = frame.left, oy = frame.top;
        int nx = MESH_WIDTH, ny = MESH_HEIGHT;
        int index = 0;
        for (int y = 0; y <= ny; y++) for (int x = 0; x <= nx; x++) {
            float u = x / (float) nx, v = y / (float) ny;
            float px = u * iw, py = v * ih;
            for (JellyZone z : zones) {
                float zx = u - z.x, zy = (v - z.y) * ih / iw;
                float distance = z.normalizedDistance(zx, zy);
                float weight = z.weightAt(distance);
                if (weight > 0) {
                    px += z.meshAtX(zx, zy, weight, distance) * iw;
                    py += z.meshAtY(zx, zy, weight, distance) * iw;
                }
            }
            vertices[index++] = px;
            vertices[index++] = py;
        }
        canvas.save();
        canvas.clipRect(viewport);
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
                if (z == activeZone) {
                    canvas.drawCircle(z.x * iw, z.y * ih, 3 * density, overlay);
                    overlay.setStyle(Paint.Style.STROKE);
                    canvas.drawLine(z.x * iw - 9 * density, z.y * ih, z.x * iw + 9 * density, z.y * ih, overlay);
                    canvas.drawLine(z.x * iw, z.y * ih - 9 * density, z.x * iw, z.y * ih + 9 * density, overlay);
                }
                overlay.setStyle(Paint.Style.STROKE);
                canvas.restore();
            }
        }
        canvas.restore();
    }

    @Override public boolean onTouchEvent(MotionEvent event) {
        if (picture == null || getWidth() == 0 || getHeight() == 0) return true;
        RectF viewport = viewport();
        if (viewport.width() <= 0 || viewport.height() <= 0) return true;
        ImageFrame frame = imageFrame(viewport);
        float iw = frame.width, ih = frame.height;
        float u = frame.imageX(event.getX());
        float v = frame.imageY(event.getY());
        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                if (!viewport.contains(event.getX(), event.getY())) return true;
                if (getParent() != null) getParent().requestDisallowInterceptTouchEvent(true);
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
                if (editing && selected != null) { activeZone = selected; notifySelectionChanged(); invalidate(); }
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
                u = frame.imageX(event.getX(pointerIndex));
                v = frame.imageY(event.getY(pointerIndex));
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
                        previousX = frame.imageX(event.getX(remaining));
                        previousY = frame.imageY(event.getY(remaining));
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
