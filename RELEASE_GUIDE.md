# Ashu PayBox - Production Release & Update Guide

## 1. Application Metadata
* **Application ID**: `com.ashupaybox.app`
* **Current Version Name**: `1.0.0`
* **Current Version Code**: `1`
* **GitHub Repository**: `ASHU0098482/soundbox`
* **Minimum Android SDK**: Android 8.0 (API 26)
* **Target Android SDK**: Android 14 (API 34) / Android 15 (API 35 ready)

---

## 2. Release & Versioning Procedure

Every new release **must** strictly increment `versionCode`. The in-app updater compares `versionCode` as an authoritative integer to determine whether a release is newer.

### Step-by-Step Release Workflow:

1. **Implement & Test Features**:
   - Write unit tests for new business logic.
   - Run test suite:
     ```powershell
     .\gradlew.bat testDebugUnitTest
     ```

2. **Increment Version in `app/build.gradle.kts`**:
   - Increment `versionCode` by 1 (e.g. `1` $\rightarrow$ `2`).
   - Bump `versionName` (e.g. `1.0.0` $\rightarrow$ `1.1.0`).

3. **Assemble Release APK**:
   ```powershell
   .\gradlew.bat assembleRelease
   ```

4. **Verify SHA-256 Digest**:
   ```powershell
   Get-FileHash app\build\outputs\apk\release\*.apk -Algorithm SHA256
   ```

5. **Create GitHub Release on `ASHU0098482/soundbox`**:
   - Go to `https://github.com/ASHU0098482/soundbox/releases/new`.
   - **Tag**: `v1.1.0` (matching `versionName`).
   - **Release Title**: `Ashu PayBox v1.1.0`.
   - **Description**: Document What's New and embed the metadata comment:
     ```markdown
     ## What's New
     • Faster payment voice announcements
     • Enhanced SoundBox reliability during Do Not Disturb
     • Offline reconciliation improvements

     <!-- versionCode: 2 -->
     ```
   - **Attach Binary Asset**: Upload `AshuPayBox-v1.1.0.apk` (or `app-release.apk`).
   - **Publish Release**.

6. **Automatic Detection in App**:
   - Installed apps running on merchant devices will check `UpdateConfig.LATEST_RELEASE_API_URL`.
   - The app detects `versionCode: 2 > 1`, displays the **Update Available** popup, downloads the APK, verifies package and signing signatures, and opens the system installer without losing any Room transactions or merchant settings!
