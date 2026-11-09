package com.sharkie.wobblekit;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.util.Log;
import android.view.WindowManager;

final class WobbleMotion implements SensorEventListener {
    private final SensorManager sensors;
    private final Sensor accelerometer;
    private final WindowManager windows;
    private final WobbleView view;
    private boolean listening;
    private final MotionFilter filter = new MotionFilter();

    WobbleMotion(Context context, WobbleView view) {
        sensors = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
        accelerometer = sensors == null ? null : sensors.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        windows = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        this.view = view;
    }

    boolean start() {
        if (listening) return true;
        if (accelerometer == null || windows == null) {
            Log.w("WobbleMotion", "Accelerometer or display unavailable");
            return false;
        }
        listening = sensors.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_GAME);
        if (!listening) Log.w("WobbleMotion", "Unable to register accelerometer listener");
        return listening;
    }

    void stop() {
        if (listening) sensors.unregisterListener(this);
        listening = false;
        filter.reset();
        view.setTilt(0, 0);
    }

    @Override public void onSensorChanged(SensorEvent event) {
        if (!listening || event.sensor.getType() != Sensor.TYPE_ACCELEROMETER) return;
        filter.update(event.values[0], event.values[1], event.values[2], event.timestamp, windows.getDefaultDisplay().getRotation());
        view.setTilt(filter.gravityX, filter.gravityY);
        view.applyMotion(filter.linearX, filter.linearY, filter.linearZ, filter.dt);
    }

    @Override public void onAccuracyChanged(Sensor sensor, int accuracy) {}
}
