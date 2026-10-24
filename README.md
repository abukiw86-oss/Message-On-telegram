# Message on Telegram (SMS to Telegram Relay)

A privacy-focused, zero-storage, bulletproof offline-first Android application and Python FastAPI backend that automatically and reliably forwards incoming phone messages (SMS/OTPs) directly to your private Telegram chat in real-time.

---

## 🌟 Key Features

- **Zero-Friction 6-Digit Pairing:** No complicated Bot API tokens for end-users. The Telegram bot issues a 6-digit code with a 15-minute TTL; enter the code in the app to pair instantly.
- **Real-Time Sub-30s Offline Sync (One-Click, Always-On):**
  - **Zero-Loss Local Queue:** If the phone is offline, in airplane mode, or has poor connectivity, messages are buffered in SQLite with status `PENDING`. They are **never dropped or abandoned** due to network disconnects.
  - **Sub-30s Active Retry Loop:** `SyncForegroundService` automatically runs when pending messages exist, retrying every **15 seconds** until all messages are delivered. As soon as all are delivered, the service stops.
  - **Sub-Second Network Recovery:** Listens for network restoration via `NetworkChangeReceiver` and Android's `ConnectivityManager.NetworkCallback`—the instant WiFi or mobile data reconnects, all pending messages are automatically dispatched in **< 1 second** without opening the app.
  - **Exact AlarmManager Backup:** Schedules exact alarms via `AlarmManager.setExactAndAllowWhileIdle()` every 15-20 seconds to guarantee wake-up even from Android Deep Doze.
  - **Expedited WorkManager:** 10-second linear backoff with `DATA_SYNC` foreground capability.
  - **15-Minute Periodic Safety Net:** Background safety net to ensure no unforwarded message is ever left behind.
  - **Battery Saver Exemption:** Includes one-tap prompt for ignoring battery optimizations so aggressive OEM killers (Samsung, Xiaomi/MIUI, Huawei, OnePlus) do not kill background delivery.
- **Dual SIM Slot Detection:** Automatically detects whether incoming SMS arrived on SIM 1, SIM 2, etc., and tags the Telegram message and dashboard log accordingly.
- **Smart OTP Extraction:** Automatically recognizes verification codes / OTPs and presents them in a distinct, tap-to-copy code block in Telegram.
- **Zero-Storage Server Privacy:** The backend server is a stateless pass-through; SMS body content is delivered straight to the Telegram Bot API in RAM and **never** stored in the backend database.
- **MySQL / MariaDB Production Backend:** Fully configured with Alwaysdata MySQL database (`abubeker_message_on_telegram`) and Telegram Webhook integration.

---

## 🏗️ Architecture

```
[Phone receives SMS (Online or Offline)]
              │
              ▼
       [SmsReceiver]
              │
       (Save to SQLite queue)
              │
              ├──► Phone Online: Immediate dispatch via goAsync()
              │
              └──► Phone Offline: Queued in SQLite
                        │
                        ▼ (Triggered by Network Reconnect / Expedited Worker / 15-min Periodic)
             [SyncWorker / NetworkCallback]
                        │
                        ▼ HTTPS POST /api/v1/forward (Zero-storage pass-thru)
             [FastAPI Backend (Alwaysdata + MySQL)]
                        │
                        ▼ Telegram Bot API (/sendMessage)
             [Telegram Cloud & User Chat]
```

---

## 🚀 Setup & Deployment

### Backend (Already Live on Alwaysdata)
- **Domain:** `https://abubeker.alwaysdata.net`
- **Database:** MariaDB/MySQL (`mysql-abubeker.alwaysdata.net` / `abubeker_message_on_telegram`)
- **Telegram Bot:** `@dsjfbsbfkbot`
- **Deployment Script:** Run `python3 backend/deploy.py` to deploy updates and sync remote `.env`.

### Android App
1. Build the APK:
   ```bash
   ./gradlew assembleDebug
   ```
2. Output APK:
   `app/build/outputs/apk/debug/app-debug.apk`
3. Launch the app, tap **Open Bot in Telegram**, send `/start` to `@dsjfbsbfkbot`, copy the 6-digit code, and tap **Pair & Connect**.
4. Allow SMS permissions and unrestricted background sync. Done!
