package com.sharkie.wobblekit;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.view.MotionEvent;
import android.view.View;
import java.util.ArrayList;

public class WobbleView extends View {
    public static class Zone {
        float x, y, r, dx, dy, vx, vy;
        Zone(float x, float y, float r) { this.x = x; this.y = y; this.r = r; }
    }

    public Bitmap picture;
    public final ArrayList<Zone> zones = new ArrayList<>();
    public boolean editing = true;
    private final Paint paint = new Paint(3);
    private final Paint overlay = new Paint(3);
    private long last = System.nanoTime();
    private float previousX, previousY, tiltX, tiltY;
    private Zone selected;
    private boolean resizing;

    public WobbleView(Context context) {
        super(context);
        setLayerType(View.LAYER_TYPE_SOFTWARE, null);
    }

    public void load(Bitmap bitmap) {
        picture = bitmap;
        zones.clear();
        selected = null;
        invalidate();
    }

    public void addZone() {
        zones.add(new Zone(.5f, .5f, .17f));
        invalidate();
    }

    public void setTilt(float x, float y) {
        tiltX = x;
        tiltY = y;
    }

    public void tick() {
        long now = System.nanoTime();
        float dt = Math.min(.04f, (now - last) / 1e9f);
        last = now;
        float aspect = picture == null ? 1f : (float) picture.getWidth() / picture.getHeight();
        for (Zone z : zones) {
            float targetX = editing ? 0 : WobbleMath.targetX(tiltX, z.r);
            float targetY = editing ? 0 : WobbleMath.targetY(tiltY, z.r, aspect);
            z.vx = WobbleMath.velocity(z.dx, z.vx, targetX, dt);
            z.vy = WobbleMath.velocity(z.dy, z.vy, targetY, dt);
            z.dx += z.vx * dt;
            z.dy += z.vy * dt;
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
        int nx = 40, ny = 60;
        float[] vertices = new float[(nx + 1) * (ny + 1) * 2];
        int index = 0;
        for (int y = 0; y <= ny; y++) for (int x = 0; x <= nx; x++) {
            float u = x / (float) nx, v = y / (float) ny;
            float px = u * iw, py = v * ih;
            for (Zone z : zones) {
                float dist = WobbleMath.distance(u, v, z.x, z.y, ih / iw);
                if (dist < z.r) {
                    float f = 1 - dist / z.r;
                    px += z.dx * iw * f * f;
                    py += z.dy * ih * f * f;
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
            for (Zone z : zones) {
                canvas.drawCircle(z.x * iw, z.y * ih, z.r * iw, overlay);
                overlay.setStyle(Paint.Style.FILL);
                canvas.drawCircle(WobbleMath.handleX(z.x, z.r) * iw, z.y * ih, 6 * density, overlay);
                overlay.setStyle(Paint.Style.STROKE);
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
                selected = null;
                resizing = false;
                float best = Float.MAX_VALUE;
                if (editing) {
                    float hitRadius = 24 * getResources().getDisplayMetrics().density / iw;
                    for (int i = zones.size() - 1; i >= 0; i--) {
                        Zone z = zones.get(i);
                        float d = WobbleMath.distance(u, v, WobbleMath.handleX(z.x, z.r), z.y, ih / iw);
                        float centerDistance = WobbleMath.distance(u, v, z.x, z.y, ih / iw);
                        if (d <= hitRadius && d < centerDistance && d < best) { selected = z; best = d; resizing = true; }
                    }
                }
                if (selected == null) {
                    best = Float.MAX_VALUE;
                    for (int i = zones.size() - 1; i >= 0; i--) {
                        Zone z = zones.get(i);
                        float d = WobbleMath.distance(u, v, z.x, z.y, ih / iw);
                        if (d < z.r && d < best) { selected = z; best = d; }
                    }
                }
                previousX = u;
                previousY = v;
                return true;
            case MotionEvent.ACTION_MOVE:
                if (selected != null) {
                    float dx = u - previousX, dy = v - previousY;
                    if (resizing) {
                        selected.r = WobbleMath.radius(WobbleMath.distance(u, v, selected.x, selected.y, ih / iw));
                    } else if (editing) {
                        selected.x = WobbleMath.clamp(selected.x + dx, 0, 1);
                        selected.y = WobbleMath.clamp(selected.y + dy, 0, 1);
                    } else {
                        selected.vx += dx * 75;
                        selected.vy += dy * 75;
                    }
                }
                previousX = u;
                previousY = v;
                invalidate();
                return true;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                selected = null;
                resizing = false;
                return true;
            default:
                return true;
        }
    }
}
