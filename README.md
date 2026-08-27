# Horizon Launcher

A sleek, console-inspired home menu launcher for Android devices built with **Kotlin** and **Jetpack Compose**. Designed for TV, mobile, and handheld console setups with full **Gamepad / D-Pad** navigation support.

## 🌟 Key Features

- **Console-Style Horizontal Carousel**: Displays installed apps and games in a responsive, focusable grid with smooth scale animations.
- **Full Gamepad / Controller Integration**:
  - **D-Pad Left / Right**: Navigate smoothly through installed applications with auto-scrolling carousel.
  - **D-Pad Up / Down**: Seamlessly switch focus between Status Bar, App Carousel, and Action Bar.
  - **Button A / Enter**: Launch selected application.
  - **Button Y**: Cycle category filters (*All*, *Games*, *Apps*).
  - **Button X**: Toggle between Light and Dark themes.
- **Smart Game Tagging**: Automatically detects installed Android games (`ApplicationInfo.CATEGORY_GAME`) and displays a distinct badge.
- **Theme Modes**: Supports both Basic Black (Dark Theme) and Basic White (Light Theme).
- **Status & Navigation**: Digital real-time clock, Wi-Fi status, battery level indicator, and customizable user profile avatar.
- **Android Home Screen Replacement**: Built as a native Android Launcher with `android.intent.category.HOME` support.

## 🛠 Tech Stack

- **Language**: Kotlin 2.1
- **UI Framework**: Jetpack Compose (Material3)
- **Minimum SDK**: Android 8.0 (API 26)
- **Target SDK**: Android 15 (API 35)
- **Build System**: Gradle 8.11 with Version Catalogs

## 🚀 Building & Running

### Prerequisites
- JDK 17 or higher (JDK 21 recommended)
- Android SDK 35

### Commands

Compile Debug APK:
```bash
./gradlew assembleDebug
```

Install via ADB:
```bash
adb install app/build/outputs/apk/debug/app-debug.apk
```

## 📄 License

MIT License
