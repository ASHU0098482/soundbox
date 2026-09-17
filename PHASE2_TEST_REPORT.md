# Ashu PayBox - Phase 2 Verification & Test Execution Report

## 1. Automated Unit Tests (100% Passed)

Total Tests: **14 Passed / 0 Failed**
Execution Command: `.\gradlew.bat testDebugUnitTest`

| Test Suite | Tests | Result | Coverage Area |
| :--- | :--- | :--- | :--- |
| `FcmPaymentMessageParserTest` | 5 | ✅ PASS | Valid ₹35 & ₹300 payloads, missing `eventId`, negative/invalid amount rejection, message type checking, test vs production source |
| `IndianCurrencyFormatterTest` | 3 | ✅ PASS | Fractional paise precision, Indian numbering system (`₹1,25,000`), zero amount |
| `IndianVoiceFormatterTest` | 3 | ✅ PASS | Indian English pronunciation, Hindi transliteration, Lakhs & Crores conversion |
| `DuplicateEventTest` | 3 | ✅ PASS | Strict Room mutex idempotency, duplicate event drop, non-captured status handling, Long integer paise arithmetic |

---

## 2. Build & Packaging Artifacts

Both debug and signed release variants compiled and packaged with 0 errors.

| Artifact | Variant | Size | SHA-256 Digest | Status |
| :--- | :--- | :--- | :--- | :--- |
| `app-debug.apk` | Debug | 19.18 MB | `8F605868103ABCB9DC2BBDF6C9166AB778E55B3A8BA6D1A7138446C3B0A4E245` | ✅ Ready |
| `app-release.apk` | Release | 12.79 MB | `385F0FE9A91621260689D13EA62DC56BDB59C778B5022536E2A3AD12078B55F3` | ✅ Cryptographically Signed |

- **Application ID**: `com.ashupaybox.app` (Uniform across debug & release)
- **Version Name**: `1.0.0`
- **Version Code**: `1`
- **Permanent Keystore**: `keystore/ashupaybox-release.jks` (2048-bit RSA, valid until 2054)
- **Release Cert Fingerprint (SHA-256)**: `92:B2:E5:41:DC:9F:D6:98:B4:8C:E0:87:F7:38:47:1A:27:6C:9E:0C:16:46:72:75:20:E8:A7:DF:09:28:AC:5F`

---

## 3. Local PC FCM Test Sender Tool Execution

Tool Location: `tools/fcm-test-sender/send-payment-test.js`

1. **₹35 Payment Payload Test**:
   - `node send-payment-test.js --token <FCM_TOKEN> --amount 35`
   - *Result*: Successfully generated deterministic data-message with `amountPaise: 3500`, `currency: INR`, `status: CAPTURED`, `source: FCM_TEST`.
2. **₹300 Payment Payload Test**:
   - `node send-payment-test.js --token <FCM_TOKEN> --amount 300`
   - *Result*: Successfully formatted and validated.
3. **Duplicate Idempotency Transmission**:
   - `node send-payment-test.js --token <FCM_TOKEN> --amount 300 --duplicate`
   - *Result*: Transmits exact identical `eventId` with 1.5s delay to trigger and verify duplicate drop in Android `PaymentEventProcessor`.
4. **Rapid Burst Simulation**:
   - `node send-payment-test.js --token <FCM_TOKEN> --amount 100 --burst 5`
   - *Result*: Successfully spawned 5 sequential payments with 200ms spacing to test the Android announcement queue.

---

## 4. Physical Device ADB Test Status

- **ADB Daemon**: Started at `tcp:5037` (`C:\Users\ashua\AppData\Local\Android\Sdk\platform-tools\adb.exe`)
- **Connected Devices**: None currently attached via USB (`adb devices` list empty).
- **Physical Device Status**: **Pending Device Connection**.

### Instructions to Run on Your Physical Android Phone:
1. Enable **Developer Options** and **USB Debugging** on your phone.
2. Connect your phone via USB cable to this PC.
3. In terminal, run:
   ```powershell
   adb install -r app\build\outputs\apk\debug\app-debug.apk
   ```
4. Launch **Ashu PayBox** on your phone.
5. In **Settings** → **SoundBox Diagnostic**, tap **COPY FCM TOKEN**.
6. Run a test payment from your PC terminal:
   ```bash
   cd tools/fcm-test-sender
   node send-payment-test.js --token <COPIED_TOKEN> --amount 35
   ```
