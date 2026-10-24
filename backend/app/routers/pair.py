import secrets
from datetime import datetime
from fastapi import APIRouter, Depends, HTTPException, Header
from sqlalchemy.orm import Session
from ..database import get_db
from ..models import PairingCode, Device, TelegramUser
from ..schemas import PairRequest, PairResponse, TelegramUserInfo, DeviceStatusResponse
from ..telegram_bot import send_sms_to_telegram

router = APIRouter(prefix="/api/v1", tags=["Pairing"])

def get_current_device(
    authorization: str = Header(..., description="Bearer <device_token>"),
    db: Session = Depends(get_db)
) -> Device:
    if not authorization.startswith("Bearer "):
        raise HTTPException(status_code=401, detail="Invalid authorization header format. Use 'Bearer <token>'.")
    token = authorization.replace("Bearer ", "").strip()
    device = db.query(Device).filter(Device.device_token == token, Device.is_active == True).first()
    if not device:
        raise HTTPException(status_code=401, detail="Device not found or deactivated.")
    return device

@router.post("/pair", response_model=PairResponse)
async def pair_device(req: PairRequest, db: Session = Depends(get_db)):
    clean_code = req.pairing_code.replace("-", "").strip()

    pairing = db.query(PairingCode).filter(
        PairingCode.code == clean_code, 
    ).first()

    if not pairing:
        raise HTTPException(status_code=400, detail="Invalid pairing code.")
    
    user = pairing.user
    if not user:
        raise HTTPException(status_code=404, detail="Associated Telegram user not found.")
 
    token = secrets.token_hex(32)
    device = Device(
        user_id=user.id,
        device_token=token,
        device_name=req.device_name or "Android Phone",
        is_active=True,
        last_seen_at=datetime.utcnow()
    )
    db.add(device)
    pairing.is_used = True
    db.commit() 

    try:
        notify_text = (
            f"🎉 *Device Connected Successfully!*\n\n"
            f"📱 *Device:* `{device.device_name}`\n"
            f"⏰ *Connected at:* {datetime.utcnow().strftime('%Y-%m-%d %H:%M UTC')}\n\n"
            "Any SMS received on this phone will now be securely forwarded here."
        ) 
        
        from ..config import settings
        import httpx
        if settings.TELEGRAM_BOT_TOKEN:
            async with httpx.AsyncClient(timeout=5.0) as client:
                await client.post(
                    f"https://api.telegram.org/bot{settings.TELEGRAM_BOT_TOKEN}/sendMessage",
                    json={"chat_id": user.telegram_chat_id, "text": notify_text, "parse_mode": "Markdown"}
                )
    except Exception:
        pass

    return PairResponse(
        status="success",
        device_token=token,
        user=TelegramUserInfo(
            first_name=user.first_name,
            username=user.telegram_username
        )
    )

@router.get("/device/status", response_model=DeviceStatusResponse)
async def get_device_status(device: Device = Depends(get_current_device)):
    return DeviceStatusResponse(
        device_name=device.device_name,
        is_active=device.is_active,
        user=TelegramUserInfo(
            first_name=device.user.first_name,
            username=device.user.telegram_username
        )
    )
