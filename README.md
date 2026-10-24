# Message on Telegram (SMS to Telegram Relay)

A privacy-focused, offline-first Android application and lightweight Python backend that automatically and reliably forwards incoming phone messages (SMS/OTPs) directly to your private Telegram chat in real-time.

---

## 🌟 Key Features

- **One-Tap Frictionless Pairing:** No complex API keys for users. The Telegram bot gives the user a 6-digit code to type into the app.
- **Zero-Storage Privacy:** Messages are passed through RAM in real-time and delivered directly to your Telegram chat. The backend server **never** stores message content.
- **Offline-First Durability:** If the phone is offline, in airplane mode, or network drops, messages are safely queued locally in an internal SQLite database and automatically retried by `WorkManager` when connectivity returns.
- **Multi-Part SMS Assembly:** Automatically groups and stitches multi-segment SMS messages into a single clean message.
- **Battery Efficient:** Uses stateless HTTPS REST requests instead of battery-draining continuous background WebSockets. The radio sleeps immediately after sending.
- **Designed for F-Droid & GitHub:** Free & Open Source, zero trackers, full native Android background receiver support.

---

## 🏗️ Architecture

```
                                      +------------------------+
                                      | Telegram Cloud Servers |
                                      +-----------^------------+
                                                  |
                                                  | Telegram Bot API (/sendMessage)
                                                  |
+---------------------+   HTTPS POST /forward    +------------------------+
|   Android Phone     | -----------------------> | Python FastAPI Backend |
| (SmsReceiver + Room)|                          | (Stateless Pass-thru)  |
+---------------------+                          +------------------------+
```

---

## 🚀 Setup Guide

### 1. Create your Telegram Bot
1. Open Telegram and search for `@BotFather`.
2. Send `/newbot` and follow the instructions to choose a name and username (e.g. `@MySmsRelayBot`).
3. Copy the **HTTP API Token** provided by BotFather (e.g., `123456789:ABCdefGhIJKlmNoPQRsTUVwxyZ`).

### 2. Configure the Backend Server
The backend is already deployed and live on your server (`https://abubeker.alwaysdata.net`).

To activate the Telegram Bot:
1. SSH into the server:
   ```bash
   ssh abubeker@ssh-abubeker.alwaysdata.net
   ```
2. Create or edit `/home/abubeker/backend/.env`:
   ```bash
   nano /home/abubeker/backend/.env
   ```
3. Set your Bot Token:
   ```env
   TELEGRAM_BOT_TOKEN="YOUR_BOT_TOKEN_HERE"
   DATABASE_URL="sqlite:////home/abubeker/backend/data.db"
   ```
4. Start/Restart the backend background worker or site.

### 3. Connect the Android App
1. Open the Telegram app and start your bot by tapping **START** (or sending `/start`).
2. The bot will reply with a 6-digit pairing code (e.g. `492180`).
3. Open the **Message on Telegram** app on your phone.
4. Enter the 6-digit code and tap **Pair & Connect**.
5. Done! Any incoming SMS will now instantly arrive in your Telegram chat!

---

## 📁 Repository Structure

```
├── app/                                # Android Application (Kotlin + Jetpack Compose)
│   ├── src/main/java/com/example/messageontelegram/
│   │   ├── core/
│   │   │   ├── database/LocalDatabase.kt   # SQLite offline message queue
│   │   │   ├── network/RelayApiClient.kt   # Authenticated REST client
│   │   │   └── preferences/                # Preferences & Token storage
│   │   ├── presentation/ui/
│   │   │   ├── PairingScreen.kt            # 6-digit code pairing UI
│   │   │   └── DashboardScreen.kt          # Sync status, switch, and message logs
│   │   ├── receiver/
│   │   │   ├── SmsReceiver.kt              # Intercepts incoming SMS
│   │   │   └── BootReceiver.kt             # Resumes sync on device reboot
│   │   └── worker/SyncWorker.kt            # WorkManager offline retry queue
│   └── AndroidManifest.xml
│
└── backend/                            # Python FastAPI Backend
    ├── app/
    │   ├── main.py                     # FastAPI application & lifespan
    │   ├── telegram_bot.py             # Bot handlers (/start, /status, /unpair)
    │   ├── models.py                   # SQLAlchemy models (Users, Codes, Devices)
    │   ├── schemas.py                  # Pydantic schemas
    │   └── routers/
    │       ├── pair.py                 # POST /api/v1/pair
    │       └── forward.py              # POST /api/v1/forward
    ├── deploy.py                       # One-command deployment script to VPS
    └── requirements.txt
```

---

## 🔒 Security & Privacy

- **No Stored Messages:** The backend database contains zero message bodies or message logs. Once delivered to Telegram, the message is immediately garbage-collected from RAM.
- **Encrypted Communication:** All communication between phone, backend, and Telegram is encrypted via HTTPS (TLS 1.3).
- **Device Authentication:** Each paired phone receives a cryptographically secure 64-character token (`Bearer <token>`) required to forward messages.
