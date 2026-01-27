# DropletManager

DropletManager is an Android app (Kotlin + Jetpack Compose) for managing DigitalOcean droplets from your phone.

Short status
- Language: Kotlin
- UI: Jetpack Compose + Material3
- Min SDK: 27 · Target SDK: 36
- Uses Firebase (Analytics, Crashlytics, App Check)

Key screens
- Droplet list
- Droplet details (reboot, usage, etc.)
- Create droplet

Table of contents
- Features
- Requirements
- Quick start (build & run)
- Firebase setup (required for full functionality)
- Local development setup (sensitive files)
- Release build & signing
- Release build & debugging
- Firebase Crashlytics integration notes
- Troubleshooting (R8 file lock, unresolved dependency, ProGuard rules)
- Contributing
- License
- [Complete Setup Guide →](SETUP.md)


Features
- List and manage droplets
- Create droplets with options
- Compose-based UI

Requirements
- Android Studio (recommended latest stable)
- JDK 17
- Android SDK with API 36 installed
- Gradle wrapper (project includes gradlew)
- Device or emulator with Play Services (for Firebase features)

Quick start (development / debug)
1. Open the project in Android Studio.
2. Let Gradle sync.
3. Run the app using the `app` Run configuration (Debug / Run). The debug build is configured by default and should work without extra steps.

> **For detailed setup, build, and export instructions, see [SETUP.md](SETUP.md)**

Commands (terminal)
- Build debug APK: `./gradlew assembleDebug`
- Install debug APK to a device: `./gradlew installDebug`
- Clean build: `./gradlew clean` (or `./gradlew --stop` to stop background daemons)

Firebase setup (required for full functionality)
The app uses Firebase for analytics and crash reporting. To set up Firebase locally:

1. Create your own Firebase project:
   - Go to [Firebase Console](https://console.firebase.google.com/)
   - Create a new project or use an existing one
   - Register your Android app with package name: `com.omiddd.dropletmanager`
   - Download the `google-services.json` file

2. Place the file in your project:
   - Copy the downloaded `google-services.json` to `app/google-services.json` (overwrite the template)
   - **Do NOT commit this file to Git** — it contains sensitive API keys and is already in `.gitignore`

3. Or use the template for development:
   - A template file `app/google-services.json.template` is provided
   - Copy it: `cp app/google-services.json.template app/google-services.json`
   - Replace placeholder values with your Firebase project details
   - For local development, some Firebase features may be limited without proper configuration

---

> **Beta testing — help wanted**
>
> The app provides a simple, clean interface to manage your DigitalOcean droplets from your phone. I'm looking for a few beta testers to help get the app ready for a full release on the Google Play Store.
>
> If you'd like to help test:
> - Join the Google Group: https://groups.google.com/g/droplet-manager-app
> - Install the app from Play Store: https://play.google.com/store/apps/details?id=com.omiddd.dropletmanager
---

Local development setup (sensitive files)
The following files are **required locally for development** but should **never be committed to Git**:

- `app/google-services.json` - Firebase configuration (excluded from git by `.gitignore`)
- `local.properties` - Local Android SDK path (excluded from git by `.gitignore`)
- `*.jks` / `*.keystore` - Signing keystores for release builds (excluded from git by `.gitignore`)
- `key.properties` - Keystore signing configuration (excluded from git by `.gitignore`)

These files are in `.gitignore` by default. If you create or modify them locally, they will not be tracked by Git, which keeps your secrets safe.

---

Release build & signing
- The project already contains a `release` buildType in `app/build.gradle.kts`.
- Build release AAB: `./gradlew bundleRelease`
- Build release APK: `./gradlew assembleRelease`

Make a release debuggable (temporary troubleshooting only)
> Never ship an APK with `isDebuggable = true` to production. Use this only to attach a debugger when diagnosing a release-only problem.

In `app/build.gradle.kts` under `buildTypes { release { ... } }` add:
```
// temporarily for debugging a release-only issue
isDebuggable = true
```
Then build & install the APK and use Android Studio's "Attach debugger to Android process".

Attach debugger to a running app
- Install the (temporary) debuggable release APK.
- In Android Studio: Run > Attach debugger to Android process and pick the app process.


Firebase Crashlytics (integration notes)
- Use the Firebase Android BoM to manage versions. Example (module-level `build.gradle.kts`):
  ```kotlin
  implementation(platform("com.google.firebase:firebase-bom:34.5.0"))
  implementation("com.google.firebase:firebase-crashlytics-ktx")
  implementation("com.google.firebase:firebase-analytics")
  ```
- Add the Crashlytics Gradle plugin to the project-level `build.gradle.kts` inside `plugins { ... }`:
  ```kotlin
  plugins {
    id("com.google.firebase.crashlytics") version "3.0.6" apply false
    id("com.google.gms.google-services") version "4.4.4" apply false
    // ... other plugin declarations
  }
  ```
- Then apply the plugin in `app/build.gradle.kts` (plugins block):
  ```kotlin
  plugins {
    id("com.android.application")
    id("com.google.firebase.crashlytics")
    id("com.google.gms.google-services")
    // other plugins (or use version catalog aliases)
  }
  ```
- If you manage plugins and dependencies via a version catalog (`libs.versions.toml`) make sure crashlytics entries exist there (or use the explicit plugin coordinates above).

Force a test crash (to confirm Crashlytics is working)
- Put a temporary button in MainActivity that throws a RuntimeException on click, run the app, press it, then restart the app to allow the crash report to be sent. See Firebase docs for details.


Common issues & fixes

1) "Unresolved reference: firebaseCrashlytics" or "Failed to resolve: com.google.firebase:firebase-crashlytics-ktx"
- Confirm you are importing the BoM (platform) before `firebase-crashlytics-ktx` implementation.
- Ensure the `maven { url "https://maven.google.com" }` and `mavenCentral()` repos are present in the project-level `settings.gradle.kts` or `build.gradle.kts`.
- If using a version catalog, add the crashlytics artifact to the catalog or use the explicit coordinates.

2) R8 / minifyRelease file locked error
```
Execution failed for task ':app:minifyReleaseWithR8'. > java.nio.file.FileSystemException: ... classes.dex: The process cannot access the file because it is being used by another process
```
- Common causes: another process is holding the file (editor, device manager, antivirus, or Gradle daemon). Fixes:
  - Run `./gradlew --stop` and then `./gradlew clean assembleRelease`.
  - Close Android Studio, then run the build from terminal (or restart your machine if necessary).
  - Temporarily disable `isMinifyEnabled` / R8 while diagnosing (in `app/build.gradle.kts` set `isMinifyEnabled = false` for release). The project currently has `isMinifyEnabled = false`.

3) ProGuard/R8 and missing classes at runtime (resource or class removal)
- If you observe crashes only in release builds (missing classes, NoClassDefFoundError), add `-keep` rules to `app/proguard-rules.pro` for your package. For example:
  ```proguard
  -keep class com.omiddd.dropletmanager.** { *; }
  ```
- Also ensure third-party libraries' keep rules are included (most ships with consumer-proguard-rules).

4) Missing UI elements in release but present in debug
- Causes include code removed by R8, different resource merging, or logic guarded by `BuildConfig.DEBUG`.
- Steps:
  - Temporarily set `isDebuggable = true` for release to attach debugger and log statements.
  - Add `-keep` rules for classes referenced via reflection or Compose-generated classes.
  - Check ProGuard rules and verify resource IDs.

5) menuAnchor() deprecation warning in Compose
- Warnings like `Modifier.menuAnchor()` being deprecated are benign for compilation but should be updated to the new overload that takes `MenuAnchorType` and `enabled` when time permits.

Versioning, compression & Play Store notes
- `versionCode` and `versionName` live in `app/build.gradle.kts` under `defaultConfig`. Increment `versionCode` for every Play Store upload.
- App bundle compression: Play Console will handle distribution optimizations. You can enable resource shrinking and R8 minification for smaller artifacts, but ensure you thoroughly test release builds and add necessary ProGuard rules.

Useful Gradle commands
- Clean, stop daemons: `./gradlew --stop; ./gradlew clean`
- Build debug: `./gradlew assembleDebug`
- Build release: `./gradlew assembleRelease` or `./gradlew bundleRelease`
- Install onto connected device: `./gradlew installDebug` or use `adb install -r <apk-path>`

Troubleshooting checklist for release-only crashes
- Ensure Crashlytics plugin and runtime dependency are added and synced.
- Ensure `proguard-rules.pro` contains keep rules for app classes used reflectively.
- Stop Gradle daemons and retry if R8 file lock occurs.
- Temporarily make release debuggable for attaching a debugger.
- Check logcat and send crash reports to Firebase.

Contributing
We welcome contributions! Whether it's bug fixes, new features, or documentation improvements, your help is appreciated.

**How to contribute:**
1. Fork the repository
2. Create a feature branch: `git checkout -b feature/your-feature-name`
3. Make your changes and test locally
4. Commit with clear messages: `git commit -m "feat: add new feature"`
5. Push to your fork and create a Pull Request
6. Ensure all checks pass and respond to review feedback

**Before submitting a PR:**
- Run a local debug build: `./gradlew assembleDebug`
- Test on an emulator or device
- Follow Kotlin style conventions (use Android Studio's formatter)
- Do not commit sensitive files (`google-services.json`, keystores, etc.)
- Include a clear description of what your PR does

**Setting up your environment:**
- See [SETUP.md](SETUP.md) for detailed setup instructions
- See [Local development setup (sensitive files)](#local-development-setup-sensitive-files) for handling Firebase and signing configs

**Code style:**
- Use Kotlin best practices
- Follow the existing project structure
- Add null-safety checks where appropriate
- Use meaningful variable and function names

License

**DropletManager** is provided under a **Non-Commercial Use License**.

### Terms & Conditions

This software is provided for **personal, educational, and non-commercial use only**. You are **NOT permitted** to:

- ✗ Use this code or any derivative for commercial purposes
- ✗ Sell, license, or distribute this software or modified versions commercially
- ✗ Fork and publish this repository as your own project (commercial or non-commercial)
- ✗ Use this code as the basis for a competing product or service
- ✗ Remove or modify the license or copyright notice

### What You CAN Do

- ✓ Use the app personally
- ✓ Study the code for learning purposes
- ✓ Contribute improvements via Pull Requests (which remain non-commercial)
- ✓ Run the app on your devices
- ✓ Report bugs and suggest features

### For Commercial Use

If you are interested in using, modifying, or distributing this software for commercial purposes, please contact:
- **Email:** gcwuxsrlh@mozmail.com

---

Contact
- Email: gcwuxsrlh@mozmail.com
