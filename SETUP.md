# DropletManager - Complete Setup Guide

This guide covers everything you need to know to set up, build, and export the DropletManager app.

## Table of Contents
1. [Environment Setup](#environment-setup)
2. [Project Setup](#project-setup)
3. [Firebase Configuration](#firebase-configuration)
4. [Building the App](#building-the-app)
5. [Testing Your Build](#testing-your-build)
6. [Troubleshooting](#troubleshooting)

---

## Environment Setup

### Prerequisites
- **Android Studio**: Latest stable version (recommended 2024.1+)
- **JDK**: JDK 17 or later
- **Android SDK**: API level 36 installed via SDK Manager
- **Git**: For version control
- **Gradle**: Included in the project (gradlew)

### Installation Steps

1. **Install Android Studio**
   - Download from [developer.android.com](https://developer.android.com/studio)
   - Run the installer and follow the setup wizard
   - Install Android SDK 36 when prompted

2. **Verify JDK**
   ```bash
   java -version  # Should show JDK 17+
   ```

3. **Clone the repository**
   ```bash
   git clone https://github.com/yourusername/DropletManager.git
   cd DropletManager
   ```

4. **Open in Android Studio**
   - File → Open → Select the DropletManager folder
   - Android Studio will automatically run Gradle sync
   - Wait for the sync to complete (this may take a few minutes on first run)

---

## Project Setup

### Step 1: Verify Dependencies
After opening the project, Android Studio will run Gradle sync automatically. Wait for it to complete.

To manually sync:
```bash
./gradlew clean
./gradlew build --dry-run  # Verify all dependencies resolve correctly
```

### Step 2: Configure SDK Path (if needed)
If you see an error about Android SDK, Android Studio will prompt you to configure it:
1. File → Settings → Appearance & Behavior → System Settings → Android SDK
2. Ensure SDK path points to your Android SDK installation
3. Click "Apply" and "OK"

A `local.properties` file will be created automatically with your SDK path. **This file is already in `.gitignore` and should never be committed.**

### Step 3: Run a Debug Build
To verify everything is set up correctly:
```bash
./gradlew assembleDebug
```

You should see: `BUILD SUCCESSFUL`

---

## Firebase Configuration

### What is google-services.json?
The `google-services.json` file contains your Firebase project configuration. It includes:
- Firebase project ID
- API keys
- Analytics configuration
- Crash reporting settings

**Important:** This file contains sensitive data and should **never be committed to Git**.

### Setting Up Firebase

#### Option A: Use Your Own Firebase Project (Recommended for Development)

1. **Create a Firebase Project**
   - Go to [Firebase Console](https://console.firebase.google.com/)
   - Click "Create a project" or select an existing project
   - Enter a project name (e.g., "DropletManager-Dev")
   - Accept the terms and create the project

2. **Register Your Android App**
   - In the Firebase Console, click "Add app" → "Android"
   - Package name: `com.omiddd.dropletmanager`
   - Debug SHA-1 (optional but recommended):
     - Get your debug keystore SHA-1:
       ```bash
       keytool -list -v -keystore ~/.android/debug.keystore -alias androiddebugkey -storepass android -keypass android
       ```
     - Copy the SHA-1 value and paste it in Firebase Console
   - Download the `google-services.json` file

3. **Add to Your Project**
   - Copy the downloaded `google-services.json` to `app/google-services.json`
   - This file will be used in debug and release builds
   - Remember: **Do not commit this file to Git** (it's in `.gitignore`)

4. **Enable Firebase Features** (in Firebase Console)
   - Go to Build → Crashlytics
   - Enable Crashlytics (if not already enabled)
   - Go to Engage → Analytics
   - Analytics is typically enabled by default

#### Option B: Use Template (Minimal Setup)

If you just want to build the app locally without full Firebase features:

1. Copy the template:
   ```bash
   cp app/google-services.json.template app/google-services.json
   ```

2. Replace placeholder values with your Firebase project details (optional for development)

3. Some Firebase features (Crashlytics, detailed Analytics) will be limited without proper configuration

---

## Building the App

### Debug Build (For Development & Testing)

**Via Command Line:**
```bash
./gradlew assembleDebug
```

Output: `app/build/outputs/apk/debug/app-debug.apk`

**Via Android Studio:**
1. Run → Run 'app' (or press Shift+F10)
2. Select your emulator or connected device
3. App will build, install, and launch

### Release Build (For Play Store / Distribution)

**Prerequisites:**
1. You must have a signing keystore (`.jks` or `.keystore` file)
2. A `key.properties` file with keystore credentials

**First-time keystore setup:**

If you don't have a keystore, create one:

```bash
keytool -genkey -v -keystore app/release/release.keystore -keyalg RSA -keysize 2048 -validity 10000 -alias droplet-key
```

You'll be prompted for:
- Keystore password
- Key alias password (can be same as keystore)
- Your name, organization, city, state, country code

Create `key.properties` in the project root:
```ini
storeFile=app/release/release.keystore
storePassword=YOUR_KEYSTORE_PASSWORD
keyAlias=droplet-key
keyPassword=YOUR_KEY_PASSWORD
```

**Important:** The `key.properties` file is in `.gitignore` and should never be committed.

**Build Release APK:**
```bash
./gradlew assembleRelease
```

Output: `app/build/outputs/apk/release/app-release.apk`

> **Note:** Release builds are for testing purposes only. Users should not publish this app to the Play Store or distribute it commercially.

---


## Testing Your Build

### Test Debug APK
```bash
./gradlew installDebug
```

Launches the app on connected device/emulator.

### Test Release APK
```bash
./gradlew installRelease
```

### Verify Crashlytics is Working

1. In the app, add a test button that crashes (temporarily):
   ```kotlin
   Button(onClick = { throw RuntimeException("Test crash") }) {
       Text("Test Crash")
   }
   ```

2. Build and run the release APK
3. Tap the "Test Crash" button
4. Restart the app (crash reports are sent on next launch)
5. Check Firebase Console → Crashlytics to see the crash report

---

## Troubleshooting

### Gradle Sync Failed
**Error:** `Failed to resolve: com.google.firebase:firebase-crashlytics-ktx`

**Solution:**
```bash
./gradlew --stop
./gradlew clean
./gradlew sync
```

### Keystore Password Issues
**Error:** `Could not decrypt keystore`

**Solution:**
- Verify `key.properties` has correct password
- Recreate the keystore if password is lost

### R8 / Minification Errors
**Error:** `java.nio.file.FileSystemException: ... classes.dex is locked`

**Solution:**
```bash
./gradlew --stop
./gradlew clean assembleRelease
```

### Build Takes Too Long
**Speed up builds:**
```bash
./gradlew assembleDebug --parallel --build-cache
```

### App Crashes on Release But Not Debug
This usually indicates ProGuard/R8 removed needed classes.

**Solution:**
1. Check `app/proguard-rules.pro` has necessary keep rules:
   ```proguard
   -keep class com.omiddd.dropletmanager.** { *; }
   ```

2. Temporarily disable R8:
   ```kotlin
   // In app/build.gradle.kts, under buildTypes.release
   isMinifyEnabled = false
   ```

3. Rebuild and test
4. Re-enable R8 and add more keep rules as needed

---

## Quick Reference

### Common Gradle Commands
```bash
./gradlew assembleDebug          # Build debug APK
./gradlew assembleRelease        # Build release APK (testing only)
./gradlew installDebug           # Install debug APK on device
./gradlew clean                  # Clean build files
./gradlew --stop                 # Stop Gradle daemon
```

### File Locations
- **Debug APK**: `app/build/outputs/apk/debug/app-debug.apk`
- **Release APK**: `app/build/outputs/apk/release/app-release.apk`
- **Google Services**: `app/google-services.json`
- **Keystore**: `app/release/release.keystore`
- **Key Properties**: `key.properties` (in project root)

### Important Notes
- Never commit: `google-services.json`, `key.properties`, `*.keystore`, `local.properties`
- All these files are in `.gitignore` by default
- Keep backups of your keystore file (losing it means you can't update your app on Play Store)

---

## Getting Help

- **Firebase Issues**: Check Firebase Console → Logs for detailed error messages
- **Build Issues**: Run with `--stacktrace` flag: `./gradlew assembleRelease --stacktrace`
- **GitHub Issues**: Open an issue in the repository with build logs and error messages

