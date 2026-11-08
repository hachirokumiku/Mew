# WobbleKit :3
Clean-room Android wobble wallpaper prototype inspired by vintage AndWobble. No original proprietary code or assets included.

## Build
Open this folder in Android Studio with Android SDK 35, then Build > Build APK(s), or run `gradle assembleDebug` with the Android SDK and Gradle available.

## Features
- Android image picker with an app-private copy for the wallpaper
- Place and drag circular wobble regions; drag the dot on a region's edge to resize it
- Toggle between editor and touch- and accelerometer-responsive spring simulation
- Tune strength, springiness and damping for all zones
- Bitmap-mesh deformation
- Live wallpaper service with touch and accelerometer input

## Known limitations
Prototype, not yet tested on physical Galaxy S26. Accelerometer input depends on an available sensor; touch interaction still works without one. Gyroscope, multi-touch, exports, GPU shader and polish are future work.

## Use
Choose an image, tap **+ Zone**, then drag a zone to move it or drag its dot to resize it. Tap **Play/Edit** and tilt, shake or touch the image to test the wobble. **Tune** adjusts strength (0–300%), springiness and damping for all zones; tap **Apply** to save or **Reset** to restore defaults. Tap **Wallpaper** to open the live-wallpaper preview and apply it. Zones, sizes and tuning are saved for the wallpaper.

## Build an APK without Android Studio (GitHub Actions)
1. Create an empty GitHub repository and upload the **contents** of this `WobbleKit` directory to its root (including `.github/workflows/android.yml`).
2. Open **Actions** > **Build WobbleKit APK** > **Run workflow** (or push to `main`).
3. After a successful run, open its **Artifacts** section and download `WobbleKit-S26-debug-APK`.
4. Extract the ZIP to get `app-debug.apk` and install it on your Android device.

This is a debug-signed APK, not a release build. GitHub Actions performs the SDK installation on its own runner; the app has not yet been tested on a Galaxy S26.
