# WobbleKit :3
Clean-room Android wobble wallpaper prototype inspired by vintage AndWobble. No original proprietary code or assets included.

## Build
Open this folder in Android Studio with Android SDK 35, then Build > Build APK(s), or run `gradle assembleDebug` with the Android SDK and Gradle available.

## Features
- Android image picker with an app-private copy for the wallpaper
- Place and drag wobble regions; resize using a handle or pinch, and adjust each zone's width, height, rotation and falloff
- Toggle between editor and tap-, drag- and accelerometer-responsive spring simulation
- Tune strength, springiness and damping for all zones
- Accelerometer-responsive jiggle with elastic squash and bitmap-mesh deformation
- Hardware-accelerated mesh drawing in the editor and live wallpaper
- Live wallpaper service with touch and accelerometer input

## Known limitations
Prototype, not yet tested on physical Galaxy S26. Accelerometer input depends on an available sensor; touch interaction still works without one. Large images are downsampled to fit a 2048-pixel texture when rendered; the original file is retained. The mesh and physics calculations run on the CPU. Camera capture, exports and multiple simultaneous wobble gestures are not yet supported. This is a clean-room approximation of the documented core workflow, not an exact reproduction of AndWobble 2.

## Use
Choose an image, tap **+ Zone**, then drag a zone to move it or pinch it or drag its dot to resize it. Select a zone and tap **Shape** to adjust width and height (radii as a percentage of image width), rotation, and smooth, broad or pointed falloff; **Delete** removes the selected zone. Tap **Preview** and tilt, move, tap or drag the image to test the jelly motion. **Tune** adjusts strength (0–300%), springiness and damping for all zones; tap **Apply** to save or **Reset** to restore defaults. Tap **Wallpaper** to open the live-wallpaper preview and apply it. Zones, shapes and tuning are saved for the wallpaper.

Lower springiness and damping in **Tune** for a slower, longer-lasting wobble. Existing tuning is retained. The accelerometer responds to movement and the image returns to its original shape when the phone is held still.

## Build an APK without Android Studio (GitHub Actions)
1. Create an empty GitHub repository and upload the **contents** of this `WobbleKit` directory to its root (including `.github/workflows/android.yml`).
2. Open **Actions** > **Build WobbleKit APK** > **Run workflow** (or push to `main`).
3. After a successful run, open its **Artifacts** section and download `WobbleKit-S26-debug-APK`.
4. Extract the ZIP to get `app-debug.apk` and install it on your Android device.

This is a debug-signed APK, not a release build. GitHub Actions performs the SDK installation on its own runner; the app has not yet been tested on a Galaxy S26.
