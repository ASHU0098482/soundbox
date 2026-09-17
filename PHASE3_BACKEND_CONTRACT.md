# Ashu PayBox - Phase 3 Backend & Razorpay Webhook Contract

## 1. System Architecture Target

```
[ Customer ]
     │
     ▼ (Makes Payment on Merchant Website)
[ Razorpay Checkout ]
     │
     ▼ (Payment Authorized & Captured)
[ Razorpay Gateway ]
     │
     ▼ HTTPS POST (X-Razorpay-Signature, Raw Body)
[ Secure Merchant Backend ]
     │ 1. Cryptographic HMAC-SHA256 Signature Verification
     │ 2. Idempotency Check via x-razorpay-event-id
     │ 3. Save to Authoritative PostgreSQL / MySQL Database
     ▼ 4. Issue FCM High-Priority Push Notification via Firebase Admin SDK
[ Firebase Cloud Messaging (FCM) ]
     │
     ▼ High-Priority Data Payload
[ Ashu PayBox Android Device ]
     │ 1. Validate payload via FcmPaymentMessageParser
     │ 2. Deduplicate against Room processed_events (eventId)
     │ 3. Save to local Room transactions
     │ 4. Issue heads-up notification
     ▼ 5. Voice Announcement: "Payment received. Thirty-five rupees."
[ Merchant SoundBox ]
```

---

## 2. Razorpay Webhook Configuration Placeholder

```ini
# Secure Backend Environment Configuration (DO NOT EMBED IN APK)
RAZORPAY_KEY_ID=rzp_live_xxxxxxxxxxxxxx
RAZORPAY_KEY_SECRET=xxxxxxxxxxxxxxxxxxxxxxxx
RAZORPAY_WEBHOOK_SECRET=xxxxxxxxxxxxxxxxxxxxxxxx
RAZORPAY_WEBHOOK_URL=<PROVIDED_IN_PHASE_3>
```

> [!CAUTION]
> The Android APK does **NOT** receive webhooks directly from Razorpay. The webhook endpoint is hosted on your secure server backend. This prevents exposing Razorpay API secrets on client devices.

---

## 3. Server Webhook Verification Requirements

The Phase 3 backend webhook handler must implement the following security checklist:

1. **Raw Body Retention**: Do not parse the JSON before validating the signature. Compute HMAC-SHA256 over the raw UTF-8 request byte buffer.
2. **Signature Header Verification**:
   ```javascript
   const crypto = require('crypto');
   const expectedSignature = crypto
     .createHmac('sha256', process.env.RAZORPAY_WEBHOOK_SECRET)
     .update(rawBody)
     .digest('hex');

   if (req.headers['x-razorpay-signature'] !== expectedSignature) {
     return res.status(400).send('Invalid signature');
   }
   ```
3. **Idempotency**: Check header `X-Razorpay-Event-Id` against the backend database. Drop duplicate webhook calls.
4. **State Precedence**:
   - `payment.captured`: Final successful state. Issue FCM payment alert.
   - `payment.failed`: Record failure, ₹0 revenue impact, do not announce.
   - Never downgrade a `CAPTURED` transaction to `AUTHORIZED` due to out-of-order webhook delivery.
5. **Authoritative Amount**: Extract `amount` (in paise) directly from the verified Razorpay webhook payload (`event.payload.payment.entity.amount`).

---

## 4. Backend REST API Endpoints for Ashu PayBox

### A. Device Registration
```http
POST /api/device/register
Content-Type: application/json
```
**Request Body**:
```json
{
  "installationId": "8f3a921d-9104-4e12-b912-32a1048b2019",
  "fcmToken": "c8k3-49dKJa:APA91bF8...",
  "platform": "ANDROID",
  "appVersion": "1.0.0",
  "versionCode": 1,
  "deviceModel": "Samsung SM-A536B",
  "androidVersion": 34
}
```
**Response**: `200 OK`
```json
{
  "status": "REGISTERED",
  "registeredAt": 1789665034000
}
```

---

### B. Offline Missed-Transaction Reconciliation Sync
```http
GET /api/device/transactions?since=<TIMESTAMP_MILLIS>
Authorization: Bearer <DEVICE_TOKEN>
```
**Response**: `200 OK`
```json
{
  "transactions": [
    {
      "eventId": "evt_984f4201_7e12",
      "paymentId": "pay_Q18d94j102",
      "orderId": "order_482109",
      "amountPaise": 3500,
      "currency": "INR",
      "status": "CAPTURED",
      "source": "BACKEND_VERIFIED",
      "method": "UPI",
      "payerName": "Rahul Sharma",
      "timestamp": 1789665034000
    }
  ],
  "hasMore": false,
  "cursor": 1789665034000
}
```

---

### C. Server-to-FCM Push Dispatch
When the backend verifies a `payment.captured` webhook:
```javascript
const message = {
  token: registeredDeviceToken,
  android: {
    priority: 'high'
  },
  data: {
    type: 'PAYMENT_EVENT',
    eventId: `evt_${event.id}`,
    paymentId: event.payload.payment.entity.id,
    orderId: event.payload.payment.entity.order_id || '',
    amountPaise: event.payload.payment.entity.amount.toString(),
    currency: event.payload.payment.entity.currency || 'INR',
    status: 'CAPTURED',
    source: 'BACKEND_VERIFIED',
    method: event.payload.payment.entity.method || 'UPI',
    payerName: event.payload.payment.entity.vpa || 'Customer',
    timestamp: Date.now().toString()
  }
};

await admin.messaging().send(message);
```
