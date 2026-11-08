package com.sharkie.wobblekit;

import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
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
        private final Runnable draw = this::render;

        WEngine() {
            view.editing = false;
            setTouchEventsEnabled(true);
        }

        private void loadSettings() {
            SharedPreferences prefs = getSharedPreferences("MainActivity", 0);
            Bitmap bitmap = null;
            if (prefs.getBoolean("hasImage", false)) {
                bitmap = BitmapFactory.decodeFile(new File(getFilesDir(), MainActivity.IMAGE_FILE).getAbsolutePath());
                if (bitmap == null) Log.w("WobbleWallpaper", "Saved image is unreadable");
            }
            view.load(bitmap);
            String saved = prefs.getString("zones", "");
            for (String item : saved.split(";")) {
                String[] parts = item.split(",");
                if (parts.length == 3) {
                    try {
                        view.zones.add(new WobbleView.Zone(Float.parseFloat(parts[0]), Float.parseFloat(parts[1]), Float.parseFloat(parts[2])));
                    } catch (NumberFormatException ex) {
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
                canvas = getSurfaceHolder().lockCanvas();
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
            super.onDestroy();
        }

        @Override public void onTouchEvent(MotionEvent event) { view.onTouchEvent(event); }
    }
}
