# Ashu PayBox - Release Signing Setup Guide

## 1. Application Identity
* **Permanent Application ID**: `com.ashupaybox.app`
* **Default Package Name**: `com.ashupaybox.app`

> [!CRITICAL]
> All future APK updates for Ashu PayBox **MUST** be signed with the exact same cryptographic key and keystore certificate. If the signing key changes, Android's `PackageManager` will reject in-app updates with an `INSTALL_FAILED_UPDATE_INCOMPATIBLE` error, requiring merchants to uninstall the app and lose locally stored offline data.

---

## 2. Generating a Permanent Release Keystore

To generate a production release keystore locally on your computer:

```powershell
keytool -genkey -v -keystore ashu-paybox-release.jks -keyalg RSA -keysize 2048 -validity 10000 -alias ashupaybox
```

You will be prompted for:
1. Keystore password (choose a secure 16+ character passphrase)
2. Merchant/Organization details (e.g., Ashu PayBox, India)
3. Key password (keep it identical to the keystore password for simplicity)

---

## 3. Configuring Gradle Signing Credentials Safely

**NEVER commit your `.jks` keystore or passwords to Git or GitHub.**

The root `.gitignore` is preconfigured to ignore `*.jks`, `*.keystore`, and `signing.properties`.

Add the signing configuration securely to `local.properties` (which is never committed to Git):

```properties
KEYSTORE_PATH=C:/Users/ashua/Desktop/soundbox/ashu-paybox-release.jks
KEYSTORE_PASSWORD=your_keystore_password_here
KEY_ALIAS=ashupaybox
KEY_PASSWORD=your_key_password_here
```

Alternatively, you can set them as environment variables:
* `RELEASE_KEYSTORE_PATH`
* `RELEASE_KEYSTORE_PASSWORD`
* `RELEASE_KEY_ALIAS`
* `RELEASE_KEY_PASSWORD`

---

## 4. Building the Signed Release APK

```powershell
.\gradlew.bat assembleRelease
```

The output signed APK will be located at:
```
app/build/outputs/apk/release/AshuPayBox-v1.0.0.apk
```

---

## 5. Verifying APK Signing & Hash

Verify the APK signature using `apksigner`:
```powershell
& "C:\Users\ashua\AppData\Local\Android\Sdk\build-tools\34.0.0\apksigner.bat" verify --verbose app\build\outputs\apk\release\app-release.apk
```

Calculate SHA-256 for release metadata:
```powershell
Get-FileHash app\build\outputs\apk\release\app-release.apk -Algorithm SHA256
```
