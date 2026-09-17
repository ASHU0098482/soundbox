# Ashu PayBox - Permanent Release Signing Key Backup & Security Guide

> [!CAUTION]
> **CRITICAL PRODUCTION WARNING**:
> All future Ashu PayBox Android APK updates rely on cryptographic signature verification (`UpdateSecurityValidator`). Android OS and the Ashu PayBox updater will **refuse to install any APK update** if the signing certificate does not match the currently installed app!
> **If you lose this keystore file, you will NOT be able to push online APK updates to your existing users.**

---

## 1. Keystore File Details

- **Keystore Location**: `keystore/ashupaybox-release.jks`
- **Key Alias**: `ashupaybox`
- **Key Algorithm**: RSA 2048-bit (SHA384withRSA)
- **Validity**: 10,000 days (~27 years until 2054)
- **Certificate SHA-1**: `AB:3D:B2:40:63:66:B2:DA:A3:78:20:50:76:03:46:22:61:D7:F0:08`
- **Certificate SHA-256**: `92:B2:E5:41:DC:9F:D6:98:B4:8C:E0:87:F7:38:47:1A:27:6C:9E:0C:16:46:72:75:20:E8:A7:DF:09:28:AC:5F`

---

## 2. Immediate Backup Instructions

1. **Copy the Keystore to a Safe Location**:
   - Backup `keystore/ashupaybox-release.jks` to a secure offline storage drive (USB drive, hardware vault) and/or a company password manager / encrypted cloud vault (e.g. 1Password, Bitwarden, AWS Secrets Manager).
2. **Never Commit to Git**:
   - `keystore/` and `*.jks` are strictly excluded in `.gitignore`.
   - Never push `ashupaybox-release.jks` or `local.properties` to GitHub or public repositories.
3. **Password Preservation**:
   - Keep the keystore password and key password backed up in your secure password vault.

---

## 3. How Android Signing Works During Gradle Builds

The build system in `app/build.gradle.kts` automatically loads signing credentials from:
1. Environment variables (`KEYSTORE_PATH`, `KEYSTORE_PASSWORD`, `KEY_ALIAS`, `KEY_PASSWORD`) if present (useful in CI/CD like GitHub Actions).
2. `local.properties` in the project root.
3. If not specified, Gradle falls back to the debug keystore for development safety without breaking compilation.

When building a release APK for distribution:
```bash
.\gradlew.bat assembleRelease
```
The output APK `app/build/outputs/apk/release/app-release.apk` will be cryptographically signed with this permanent release key.
