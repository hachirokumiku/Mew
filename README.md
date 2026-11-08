# WobbleKit :3
Clean-room Android wobble wallpaper prototype inspired by vintage AndWobble. No original proprietary code or assets included.

## Build
Open this folder in Android Studio with Android SDK 35, then Build > Build APK(s), or run `gradle assembleDebug` with the Android SDK and Gradle available.

## Features
- Android image document picker with persisted read permission
- Place and drag circular wobble regions
- Toggle between editor and touch-responsive spring simulation
- Bitmap-mesh deformation
- Live wallpaper service with touch events

## Known limitations
Prototype, not yet tested on physical Galaxy S26. No compiled APK in this archive because the build environment lacks Android SDK/Gradle and cannot download them. Zone sizes are fixed; gyroscope, multi-touch, exports, GPU shader and polish are future work. Wallpaper image access can vary across document providers; test on device.

## Build an APK without Android Studio (GitHub Actions)
1. Create an empty GitHub repository and upload the **contents** of this `WobbleKit` directory to its root (including `.github/workflows/android.yml`).
2. Open **Actions** > **Build WobbleKit APK** > **Run workflow** (or push to `main`).
3. After a successful run, open its **Artifacts** section and download `WobbleKit-S26-debug-APK`.
4. Extract the ZIP to get `app-debug.apk` and install it on your Android device.

This is an unsigned-for-release but debug-signed APK. GitHub Actions performs the SDK installation on its own runner. Builds have not yet been verified on a GitHub runner or a Galaxy S26.
