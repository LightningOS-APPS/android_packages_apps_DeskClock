# DeskClock: Material 3 Expressive

A redesigned and extended version of the [LineageOS DeskClock](https://github.com/LineageOS/android_packages_apps_DeskClock) app, maintained by **LightningOS**.

It keeps everything the original clock does and adds a full Material 3 Expressive redesign plus a set of new alarm, timer and clock features.

---

## ✨ Features

### Material 3 Expressive design
- Follows the system light/dark mode, with dynamic wallpaper colours
- Scalloped "cookie" clock dial with accent hands and centre dots
- Wavy progress rings on the stopwatch and timer
- Pill-shaped timer keypad with press-to-morph keys
- Floating pill navigation bar and rounded floating snackbars
- Google Sans throughout
- Material clock and date pickers and dialogs
- Expressive analog and digital home-screen widgets

### Alarms
- **Skip next:** skip one occurrence of a repeating alarm without turning it off
- **Dated alarms:** set a one-time alarm for a specific day
- **Wake-up challenges:** solve a sum, shake the phone 30 times, or type a phrase to dismiss
- **Sunrise alarm:** the screen fades from night blue to warm orange while ringing
- **Wake-up stats:** snoozes and wake-up times for the last 30 days
- **Bedtime reminder:** a nightly nudge showing how much sleep you'll get

### Timer and clock
- Editable one-tap timer presets
- Timer names and a live countdown in notifications
- Day/night indicator for world clocks
- Current weather on the Clock tab (via [Open-Meteo](https://open-meteo.com))
- In-app **What's new** changelog

---

## 🔨 Building

You don't need a full Android source tree. The app builds on its own with Gradle.

**Requirements**
- JDK 21 (full JDK, not just the runtime): `sudo apt install openjdk-21-jdk`
- Android SDK command-line tools, with `ANDROID_HOME` set

**Build**
```bash
git clone https://github.com/LightningOS-APPS/android_packages_apps_DeskClock
cd android_packages_apps_DeskClock
./gradlew assembleDebug
```

The APK is written to `build/outputs/apk/debug/`.

### Installing alongside the stock clock
Debug builds use the package name `com.android.deskclock.dev`, so they install next to your phone's existing clock instead of replacing it.

For reliable alarms on a sideloaded build:
- Set the app's battery usage to **Unrestricted**
- On Samsung devices, make sure it isn't listed under **Sleeping apps**

---

## 🙏 Credits

- [LineageOS](https://lineageos.org) and the Android Open Source Project, for the original DeskClock
- [Google Sans](https://fonts.google.com/specimen/Google+Sans), used under the SIL Open Font License
- [Open-Meteo](https://open-meteo.com), for free weather data

This project is not affiliated with or endorsed by Google or LineageOS.

## 📄 License

The app's code is licensed under the [Apache License 2.0](https://www.apache.org/licenses/LICENSE-2.0), like the original DeskClock.
Google Sans is licensed under the SIL Open Font License 1.1; see [`GOOGLE_SANS_OFL.txt`](GOOGLE_SANS_OFL.txt).
