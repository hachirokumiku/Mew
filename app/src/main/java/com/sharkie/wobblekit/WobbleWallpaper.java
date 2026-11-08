package com.sharkie.wobblekit;

import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.os.Handler;
import android.service.wallpaper.WallpaperService;
import android.util.Log;
import android.view.MotionEvent;
import java.io.File;

public class WobbleWallpaper extends WallpaperService {
    @Override public Engine onCreateEngine() { return new WEngine(); }

    class WEngine extends Engine {
        private final Handler handler = new Handler();
        private final WobbleView view = new WobbleView(WobbleWallpaper.this);
        private final WobbleMotion motion = new WobbleMotion(WobbleWallpaper.this, view);
        private boolean visible;
        private final SharedPreferences preferences = getSharedPreferences("MainActivity", 0);
        private final SharedPreferences.OnSharedPreferenceChangeListener physicsListener = (prefs, key) -> {
            if ("strength".equals(key) || "spring".equals(key) || "damping".equals(key)) view.loadPhysics(prefs);
            if ("zones".equals(key) && visible) handler.post(() -> { if (visible) loadZones(); });
        };
        private final Runnable draw = this::render;

        WEngine() {
            view.editing = false;
            preferences.registerOnSharedPreferenceChangeListener(physicsListener);
            setTouchEventsEnabled(true);
        }

        private void loadSettings() {
            SharedPreferences prefs = preferences;
            view.loadPhysics(prefs);
            Bitmap bitmap = null;
            if (prefs.getBoolean("hasImage", false)) {
                bitmap = ImageLoader.load(new File(getFilesDir(), MainActivity.IMAGE_FILE));
                if (bitmap == null) Log.w("WobbleWallpaper", "Saved image is unreadable");
            }
            view.load(bitmap);
            loadZones();
        }

        private void loadZones() {
            view.zones.clear();
            String saved = preferences.getString("zones", "");
            for (String item : saved.split(";")) {
                if (!item.isEmpty()) {
                    try {
                        view.zones.add(JellyZone.parse(item));
                    } catch (IllegalArgumentException ex) {
                        Log.w("WobbleWallpaper", "Invalid saved zone", ex);
                    }
                }
            }
        }

        private void render() {
            handler.removeCallbacks(draw);
            if (!visible) return;
            Canvas canvas = null;
            try {
                canvas = getSurfaceHolder().lockHardwareCanvas();
                if (canvas != null) {
                    view.layout(0, 0, canvas.getWidth(), canvas.getHeight());
                    view.tick();
                    view.draw(canvas);
                }
            } catch (IllegalArgumentException | IllegalStateException ex) {
                Log.w("WobbleWallpaper", "Unable to draw wallpaper", ex);
            } finally {
                if (canvas != null) getSurfaceHolder().unlockCanvasAndPost(canvas);
            }
            handler.postDelayed(draw, 33);
        }

        @Override public void onVisibilityChanged(boolean visible) {
            this.visible = visible;
            if (visible) {
                loadSettings();
                motion.start();
                view.resetClock();
                render();
            } else {
                motion.stop();
                handler.removeCallbacks(draw);
            }
        }

        @Override public void onDestroy() {
            visible = false;
            motion.stop();
            handler.removeCallbacks(draw);
            preferences.unregisterOnSharedPreferenceChangeListener(physicsListener);
            super.onDestroy();
        }

        @Override public void onTouchEvent(MotionEvent event) { view.onTouchEvent(event); }
    }
}
