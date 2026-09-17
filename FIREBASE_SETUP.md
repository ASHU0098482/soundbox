# Ashu PayBox - Firebase Cloud Messaging (FCM) Integration Setup Guide

## 1. Project & App Overview

- **Android Application ID**: `com.ashupaybox.app` (Uniform package ID across debug and release builds)
- **Firebase Configuration Location**: `app/google-services.json`
- **Enabled Products**:
  - **Firebase Cloud Messaging (FCM)**: Real-time high-priority push notifications
  - **Firebase Installations**: Unique device installation token management
- **Build Plugin**: `com.google.gms.google-services` (v4.4.2)
- **Firebase Android BoM**: `com.google.firebase:firebase-bom:33.7.0`
- **Core Dependency**: `com.google.firebase:firebase-messaging-ktx`

---

## 2. Architecture & Delivery Pipeline

```
Phase 2 Local Test / Phase 3 Backend
               │
               ▼
 Firebase Cloud Messaging (FCM HTTP v1 / Admin SDK)
               │
               ▼ (High-Priority Data Message)
 PayBoxFirebaseMessagingService (onMessageReceived)
               │
               ▼
 FcmPaymentMessageParser (Defensive Schema Validation)
               │
               ▼
 PaymentEventProcessor (Mutex Lock + Room Idempotency Key)
        ┌──────┴──────┐
        ▼             ▼
   Room Database  PaymentAnnouncementManager (TTS Voice + Chime)
        │             │
        ▼             ▼
 Dashboard UI   PaymentNotificationManager (High-Priority Heads-up Alert)
```

---

## 3. Strict FCM Data-Message Schema

Incoming FCM push notifications **MUST** be sent as **data messages** (not notification-only messages) to guarantee execution when the app is in background or screen is locked:

| Key | Type | Description | Example |
| :--- | :--- | :--- | :--- |
| `type` | `String` | Message type contract identifier | `PAYMENT_EVENT` |
| `eventId` | `String` | Idempotency Key (UUID) | `evt_984f4201_7e12` |
| `paymentId` | `String` | Razorpay payment identifier | `pay_Q18d94j102` |
| `orderId` | `String` | Merchant order identifier | `order_482109` |
| `amountPaise` | `String` | Monetary value in integer paise | `3500` (₹35.00), `30000` (₹300.00) |
| `currency` | `String` | 3-character ISO currency code | `INR` |
| `status` | `String` | Payment status | `CAPTURED` |
| `source` | `String` | Event origin flag | `FCM_TEST` (Phase 2), `BACKEND_VERIFIED` (Phase 3) |
| `method` | `String` | Payment instrument | `UPI`, `CARD`, `NETBANKING` |
| `payerName` | `String` | Optional customer name | `Rahul Sharma` |
| `timestamp` | `String` | UTC Milliseconds epoch | `1789665034000` |

---

## 4. Local PC FCM Test Sender Tool

A local Node.js developer CLI tool is provided in `tools/fcm-test-sender/`.

### Installation
```bash
cd tools/fcm-test-sender
npm install
```

### Running Tests
1. **Find Device FCM Token**:
   - Open **Ashu PayBox** on the device.
   - Go to **Settings** → **SoundBox Diagnostic**.
   - Tap **"COPY FCM TOKEN"**.
2. **Send ₹35 Test Payment**:
   ```bash
   node send-payment-test.js --token <COPIED_FCM_TOKEN> --amount 35
   ```
3. **Send ₹300 Test Payment**:
   ```bash
   node send-payment-test.js --token <COPIED_FCM_TOKEN> --amount 300
   ```
4. **Test Idempotency & Duplicate Protection**:
   ```bash
   node send-payment-test.js --token <COPIED_FCM_TOKEN> --amount 35 --duplicate
   ```
   *Expected Result*: The device plays the voice announcement and records the transaction **exactly once**. The second delivery is logged as `DUPLICATE_IGNORED` in the diagnostic ring buffer.
5. **Test Rapid Payment Burst Queue**:
   ```bash
   node send-payment-test.js --token <COPIED_FCM_TOKEN> --burst 10
   ```
   *Expected Result*: All 10 payments update the database immediately, and the voice announcements queue sequentially without cutting each other off or causing ANR/crashes.

---

## 5. Security & Credential Rules

> [!IMPORTANT]
> **Zero-Secret Client Policy**:
> - Never place Firebase Admin service-account private keys inside the Android APK.
> - Never store Razorpay key secrets or webhook secrets in the Android repository.
> - `service-account.json` and `signing.properties` are strictly excluded in `.gitignore`.
