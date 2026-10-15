# CycleTrack 🚴‍♂️
**Smart Cycling Tracker Android App**

CycleTrack is a native Android application built with **Kotlin**, **Jetpack Compose**, **Room Database**, **OSMDroid (OpenStreetMap)**, and **Hilt DI** following **Clean Architecture + MVVM**.

---

## 🚀 How to Generate the APK

### Method 1: Using Android Studio (Recommended locally)
1. Download & Install [Android Studio](https://developer.android.com/studio) (if not already installed).
2. Open Android Studio and select **Open**, then choose the folder:
   `c:\Users\kumar\OneDrive\Desktop\Cycle Track`
3. Wait for the initial Gradle sync to complete (Android Studio will automatically download the Android 34 SDK and dependencies).
4. In the top menu, go to:
   **Build** > **Build Bundle(s) / APK(s)** > **Build APK(s)**.
5. Once built, a popup will appear at the bottom right. Click **locate** or find your APK at:
   `app/build/outputs/apk/debug/app-debug.apk`

---

### Method 2: Cloud Build via GitHub Actions (Zero Setup on your PC)
This repository includes an automated workflow in [`.github/workflows/build-apk.yml`](.github/workflows/build-apk.yml).
1. Create a repository on [GitHub](https://github.com).
2. Push this project to your repository.
3. Go to the **Actions** tab on your GitHub repository.
4. The workflow will run automatically and compile the app using cloud runners with JDK 17 and Android SDK 34.
5. Under the completed workflow run, download the **CycleTrack-debug-apk** artifact!

---

## 📱 Features

- **Live GPS Tracking**: Fused Location Provider with accuracy filters and jump noise reduction.
- **Offline OpenStreetMap**: Real-time map powered by OSMDroid (no API key required).
- **Background Foreground Service**: Continued tracking and persistent notification with pause/resume controls.
- **Calorie Estimation**: Formula based on MET (Metabolic Equivalent of Task), body weight, duration, and intensity.
- **Detailed History & Route Details**: Complete route polyline on map, start/finish pins, and pace breakdown.
- **Statistics & Trends**: Custom canvas bar charts for daily distance, weekly progress, PRs (Personal Records), and intensity breakdown.
- **Preferences**: km / mi toggle, daily goals, profile adjustments, and dark theme support.
