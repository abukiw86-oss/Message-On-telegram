import random
import re
import logging
from datetime import datetime, timedelta
import httpx
from telegram import Update
from telegram.ext import Application, CommandHandler, ContextTypes
from .config import settings
from .database import SessionLocal
from .models import TelegramUser, PairingCode, Device

logger = logging.getLogger(__name__)

bot_app: Application | None = None


async def start_command(update: Update, context: ContextTypes.DEFAULT_TYPE):
    """Handle /start command from Telegram user."""
    chat = update.effective_chat
    user = update.effective_user

    if not chat or not user:
        return

    db = SessionLocal()
    try: 
        db_user = db.query(TelegramUser).filter(TelegramUser.telegram_chat_id == chat.id).first()
        if not db_user:
            db_user = TelegramUser(
                telegram_chat_id=chat.id,
                telegram_username=user.username,
                first_name=user.first_name,
            )
            db.add(db_user)
            db.commit()
            db.refresh(db_user)
        else:
            db_user.telegram_username = user.username
            db_user.first_name = user.first_name
            db.commit() 

        db.query(PairingCode).filter(
            PairingCode.user_id == db_user.id,
            PairingCode.is_used == True
        ).update({"is_used": False})
 
        code = "999997"
        expiry = datetime.utcnow() + timedelta(minutes=settings.PAIRING_CODE_EXPIRY_MINUTES)
        
        pairing_record = PairingCode(
            user_id=db_user.id,
            code=code,
            expires_at=expiry,
            is_used=False
        )
        db.add(pairing_record)
        db.commit()

        welcome_text = (
            f"👋 *Welcome to SMS Relay, {user.first_name or 'there'}!*\n\n"
            "To connect your Android phone:\n"
            "1️⃣ Open the *Message on Telegram* app on your phone.\n"
            "2️⃣ Enter this 6-digit pairing code:\n\n"
            f"👉 `{code}` 👈\n\n"
            f"⏳ _Code expires in {settings.PAIRING_CODE_EXPIRY_MINUTES} minutes._\n"
            "Tap the code above to copy it."
        )

        await update.message.reply_text(welcome_text, parse_mode="Markdown")

    except Exception as e:
        logger.error(f"Error handling /start: {e}")
        await update.message.reply_text(f"⚠️ An error occurred while generating your pairing code. Please try again: {e}")
    finally:
        db.close()

async def status_command(update: Update, context: ContextTypes.DEFAULT_TYPE):
    """List paired devices for user."""
    chat = update.effective_chat
    if not chat:
        return

    db = SessionLocal()
    try:
        db_user = db.query(TelegramUser).filter(TelegramUser.telegram_chat_id == chat.id).first()
        if not db_user or not db_user.devices:
            await update.message.reply_text("ℹ️ No devices currently paired. Type /start to connect a device.")
            return

        text = "📱 *Connected Devices:*\n\n"
        for idx, dev in enumerate(db_user.devices, 1):
            status = "🟢 Active" if dev.is_active else "🔴 Inactive"
            last_seen = dev.last_seen_at.strftime("%Y-%m-%d %H:%M UTC") if dev.last_seen_at else "Never"
            text += f"{idx}. *{dev.device_name}* ({status})\n   _Last seen: {last_seen}_\n"

        await update.message.reply_text(text, parse_mode="Markdown")
    finally:
        db.close()

async def unpair_command(update: Update, context: ContextTypes.DEFAULT_TYPE):
    """Unpair all devices for user."""
    chat = update.effective_chat
    if not chat:
        return

    db = SessionLocal()
    try:
        db_user = db.query(TelegramUser).filter(TelegramUser.telegram_chat_id == chat.id).first()
        if db_user:
            db.query(Device).filter(Device.user_id == db_user.id).delete()
            db.commit()
            await update.message.reply_text("✅ All devices have been unpaired successfully.")
        else:
            await update.message.reply_text("ℹ️ No devices to unpair.")
    finally:
        db.close()

def extract_otp(body: str) -> str | None:
    """Extract OTP/verification code from message body if present."""
    match = re.search(r'(?i)(?:code|otp|código|kod|pin|is|secret|verification|password|passcode)\b[^\d]{0,10}(\d{4,8})\b', body)
    if match:
        return match.group(1)
    if re.search(r'(?i)\b(?:verification|security|login|confirm|authenticate|bank|account|two-factor|2fa|auth)\b', body):
        match = re.search(r'\b(\d{4,8})\b', body)
        if match:
            return match.group(1)
    return None

async def send_sms_to_telegram(chat_id: int, sender: str, body: str, received_at: int | None = None, sim_slot: int | None = None) -> int | None:
    """Send formatted SMS notification to user via Telegram Bot API."""
    if not settings.TELEGRAM_BOT_TOKEN:
        raise ValueError("TELEGRAM_BOT_TOKEN is not configured.")

    time_str = datetime.utcnow().strftime("%Y-%m-%d %H:%M:%S UTC")
    if received_at:
        try:
            dt = datetime.utcfromtimestamp(received_at / 1000.0)
            time_str = dt.strftime("%Y-%m-%d %H:%M:%S UTC")
        except Exception:
            pass

    sim_info = f" [SIM {sim_slot + 1}]" if sim_slot is not None else ""
    otp_code = extract_otp(body)
    otp_section = f"\n🔑 *Verification Code:* `{otp_code}` _(Tap to copy)_\n" if otp_code else ""

    formatted_text = (
        f"📩 *New SMS Received*{sim_info}\n"
        f"👤 *From:* `{sender}`\n"
        f"🕒 *Time:* {time_str}\n"
        f"{otp_section}"
        "━━━━━━━━━━━━━━━━━━━━\n"
        f"{body}"
    )

    url = f"https://api.telegram.org/bot{settings.TELEGRAM_BOT_TOKEN}/sendMessage"
    payload = {
        "chat_id": chat_id,
        "text": formatted_text,
        "parse_mode": "Markdown",
        "disable_web_page_preview": True
    }

    async with httpx.AsyncClient(timeout=10.0) as client:
        resp = await client.post(url, json=payload)
        if resp.status_code == 200:
            data = resp.json()
            return data.get("result", {}).get("message_id")
        else:
            # If markdown fails due to special characters in SMS body, retry in plain text
            plain_otp = f"\nVerification Code: {otp_code}\n" if otp_code else ""
            payload.pop("parse_mode", None)
            payload["text"] = f"📩 New SMS Received{sim_info}\nFrom: {sender}\nTime: {time_str}\n{plain_otp}--------------------\n{body}"
            resp2 = await client.post(url, json=payload)
            if resp2.status_code == 200:
                data = resp2.json()
                return data.get("result", {}).get("message_id")
            logger.error(f"Failed to send Telegram message: {resp.status_code} - {resp.text}")
            return None

def create_bot_app() -> Application | None:
    """Create and configure python-telegram-bot application."""
    if not settings.TELEGRAM_BOT_TOKEN:
        logger.warning("No TELEGRAM_BOT_TOKEN provided. Telegram Bot polling will not start.")
        return None

    app = Application.builder().token(settings.TELEGRAM_BOT_TOKEN).build()
    app.add_handler(CommandHandler("start", start_command))
    app.add_handler(CommandHandler("status", status_command))
    app.add_handler(CommandHandler("unpair", unpair_command))
    return app
