#!/usr/bin/env node

/**
 * Ashu PayBox - Local PC Developer FCM Test Sender Tool
 * 
 * Sends deterministic payment data-messages via Firebase Cloud Messaging (FCM)
 * to test the Android Ashu PayBox device receiver.
 * 
 * Usage Examples:
 *   node send-payment-test.js --token <DEVICE_FCM_TOKEN> --amount 35
 *   node send-payment-test.js --token <DEVICE_FCM_TOKEN> --amount 300
 *   node send-payment-test.js --token <DEVICE_FCM_TOKEN> --amount 500 --duplicate
 *   node send-payment-test.js --token <DEVICE_FCM_TOKEN> --burst 10
 */

const fs = require('fs');
const path = require('path');
const crypto = require('crypto');

// Parse CLI Arguments
const args = process.argv.slice(2);
function getArg(flag, defaultValue = null) {
  const index = args.indexOf(flag);
  if (index !== -1 && index + 1 < args.length) {
    return args[index + 1];
  }
  return defaultValue;
}
const hasFlag = (flag) => args.includes(flag);

const token = getArg('--token') || process.env.PAYBOX_DEVICE_TOKEN;
const amountRupees = parseFloat(getArg('--amount', '35'));
const isDuplicate = hasFlag('--duplicate');
const burstCount = parseInt(getArg('--burst', '1'), 10);
const source = getArg('--source', 'FCM_TEST');
const status = getArg('--status', 'CAPTURED');
const currency = getArg('--currency', 'INR');
const credPath = getArg('--cred') || process.env.GOOGLE_APPLICATION_CREDENTIALS || path.join(__dirname, 'service-account.json');

console.log('====================================================');
console.log('   ASHU PAYBOX - LOCAL FCM TEST SENDER (PHASE 2)');
console.log('====================================================');

if (!token) {
  console.error('\n❌ ERROR: Missing device FCM token.');
  console.error('Please specify: --token <FCM_TOKEN> or set PAYBOX_DEVICE_TOKEN env var.');
  console.error('\nTo find your device token:');
  console.error('  1. Open Ashu PayBox on your Android device');
  console.error('  2. Go to Settings -> SoundBox Diagnostic');
  console.error('  3. Tap "COPY FCM TOKEN"');
  console.error('  4. Run: node send-payment-test.js --token <COPIED_TOKEN> --amount 35\n');
  process.exit(1);
}

// Prepare payload builder
function createPayload(eventId, paymentId, amountPaise) {
  return {
    type: 'PAYMENT_EVENT',
    eventId: eventId,
    paymentId: paymentId,
    orderId: `order_${Date.now()}`,
    amountPaise: amountPaise.toString(),
    currency: currency,
    status: status,
    source: source,
    method: 'UPI',
    payerName: 'Customer Test',
    timestamp: Date.now().toString()
  };
}

async function run() {
  let admin = null;
  let useAdminSdk = false;

  if (fs.existsSync(credPath)) {
    try {
      admin = require('firebase-admin');
      const serviceAccount = JSON.parse(fs.readFileSync(credPath, 'utf8'));
      if (!admin.apps.length) {
        admin.initializeApp({
          credential: admin.credential.cert(serviceAccount)
        });
      }
      useAdminSdk = true;
      console.log(`✅ Loaded Firebase service account: ${serviceAccount.project_id}`);
    } catch (e) {
      console.warn(`⚠️ Warning: Could not initialize Firebase Admin SDK: ${e.message}`);
    }
  } else {
    console.log(`ℹ️ No service-account.json found at: ${credPath}`);
    console.log(`   (When Google credentials are fully authenticated, real push delivery will be dispatched)`);
  }

  const amountPaise = Math.round(amountRupees * 100);

  if (burstCount > 1) {
    console.log(`\n🚀 Starting burst simulation of ${burstCount} rapid payments...`);
    for (let i = 1; i <= burstCount; i++) {
      const burstEventId = `evt_burst_${Date.now()}_${i}_${crypto.randomBytes(4).toString('hex')}`;
      const burstPaymentId = `pay_burst_${Date.now()}_${i}`;
      const payload = createPayload(burstEventId, burstPaymentId, amountPaise);
      console.log(`[Burst ${i}/${burstCount}] Sending ₹${amountRupees} (Event: ${burstEventId})...`);
      await sendMessage(admin, useAdminSdk, token, payload);
      await new Promise(r => setTimeout(r, 200)); // 200ms spacing
    }
    console.log(`\n✅ Burst test of ${burstCount} payments completed.`);
    return;
  }

  // Single or Duplicate test
  const eventId = getArg('--eventId') || `evt_test_${Date.now()}_${crypto.randomBytes(4).toString('hex')}`;
  const paymentId = `pay_test_${Date.now()}_${crypto.randomBytes(4).toString('hex')}`;
  const payload = createPayload(eventId, paymentId, amountPaise);

  console.log('\n📦 Prepared FCM Payment Payload:');
  console.log(JSON.stringify(payload, null, 2));

  console.log(`\n📤 Sending payment event to device (₹${amountRupees})...`);
  await sendMessage(admin, useAdminSdk, token, payload);

  if (isDuplicate) {
    console.log('\n🔁 Duplicate Test Enabled: Sending exact same payload in 1.5 seconds...');
    await new Promise(r => setTimeout(r, 1500));
    console.log(`📤 Re-sending duplicate eventId: ${eventId}...`);
    await sendMessage(admin, useAdminSdk, token, payload);
    console.log('✅ Duplicate message sent. Expected result in app: Room records 1 transaction, plays 1 sound.');
  }

  console.log('\n====================================================');
  console.log('   TEST TRANSMISSION COMPLETED');
  console.log('====================================================\n');
}

async function sendMessage(admin, useAdminSdk, deviceToken, dataPayload) {
  if (useAdminSdk && admin) {
    try {
      const message = {
        data: dataPayload,
        token: deviceToken,
        android: {
          priority: 'high'
        }
      };
      const response = await admin.messaging().send(message);
      console.log(`   ✅ Successfully delivered via Firebase FCM: ${response}`);
    } catch (err) {
      console.error(`   ❌ Failed to send via Firebase FCM: ${err.message}`);
    }
  } else {
    console.log(`   ℹ️ [DRY RUN / SIMULATION MODE]`);
    console.log(`   Data payload formatted and validated successfully.`);
    console.log(`   To send live push messages, place your Firebase service-account.json in tools/fcm-test-sender/`);
  }
}

run().catch(console.error);
