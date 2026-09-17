# Ashu PayBox 🔊🇮🇳

**Ashu PayBox** is a digital merchant payment SoundBox Android application that announces verified customer payments via voice in real-time (English, Hindi, and Hinglish).

---

## 🌟 Key Features

- 🔊 **Voice Announcement Engine**: Indian English, Hindi, and Hinglish voice announcements using local TTS and pleasant POS chime tone.
- 💰 **Paise Precision**: Internal integer arithmetic prevents IEEE-754 floating point calculation issues.
- ⚡ **Real-Time Push Integration (FCM)**: Native Firebase Cloud Messaging integration for instant payment delivery.
- 🛡️ **Duplicate & Idempotency Protection**: Mutex-locked pipeline guarantees zero duplicate transactions or duplicate voice announcements.
- 📱 **GitHub In-App Auto-Updater**: In-app version checker that verifies cryptographic signatures and SHA-256 before upgrading.
- 📊 **Merchant Analytics**: Real vs Test revenue separation, hourly buckets, UPI/Card method breakdowns, and CSV export.
- 🩺 **Hardware Diagnostic Assistant**: 8-point system diagnostic test (Speaker, TTS, Chime, Notifications, DND Access, Volume, Network, Firebase).

---

## 📥 Download APK

- **Release APK**: Available on [GitHub Releases](https://github.com/ASHU0098482/soundbox/releases)
- **Package ID**: `com.ashupaybox.app`
- **Minimum Android Version**: Android 8.0 (API 26)

---

## 🛠️ Build & Development

```bash
# Compile Debug APK
.\gradlew.bat assembleDebug

# Compile Signed Release APK
.\gradlew.bat assembleRelease

# Run All Unit Tests
.\gradlew.bat testDebugUnitTest
```
