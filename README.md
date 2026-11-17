# NOVA Launcher

A native Android home-screen launcher built with Kotlin and Jetpack Compose.

## Current foundation
- Android HOME launcher entry point (select NOVA from the system Home app chooser)
- Live clock and date
- Installed-app discovery and app launching
- Searchable all-apps drawer
- Adaptive Compose UI foundation with a glass-inspired visual style
- GitHub Actions debug APK build

## Build
Open this repository in Android Studio (Hedgehog or newer) and sync Gradle. The project uses JDK 17, Android Gradle Plugin 8.7.3, Gradle 8.9, and compile SDK 35.

From a machine with Gradle 8.9 installed:
```bash
gradle :app:assembleDebug
```
APK: `app/build/outputs/apk/debug/app-debug.apk`

## Install and select as home
Install the debug APK, press the Android Home button, and choose NOVA. Android may ask whether to set it as the default home app.

## Roadmap
1. Launcher foundation and app discovery
2. One UI-inspired home pages, dock, folders and gesture navigation
3. Custom icon packs and launcher settings
4. Real Android widgets and shortcut support
5. Motion polish, performance profiling and release builds

NOVA is an independent project and is not affiliated with Samsung.
