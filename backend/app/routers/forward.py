from datetime import datetime
from fastapi import APIRouter, Depends, HTTPException
from sqlalchemy.orm import Session
from ..database import get_db
from ..models import Device
from ..schemas import ForwardMessageRequest, ForwardMessageResponse
from ..routers.pair import get_current_device
from ..telegram_bot import send_sms_to_telegram

router = APIRouter(prefix="/api/v1", tags=["Forwarding"])

@router.post("/forward", response_model=ForwardMessageResponse)
async def forward_sms(
    req: ForwardMessageRequest,
    device: Device = Depends(get_current_device),
    db: Session = Depends(get_db)
):
    if not device.user:
        raise HTTPException(status_code=404, detail="User not found for this device.")

    # Update last seen timestamp
    device.last_seen_at = datetime.utcnow()
    db.commit()

    # Forward immediately to Telegram (Zero-storage: body never saved to database)
    try:
        msg_id = await send_sms_to_telegram(
            chat_id=device.user.telegram_chat_id,
            sender=req.sender,
            body=req.body,
            received_at=req.received_at,
            sim_slot=req.sim_slot
        )

        return ForwardMessageResponse(
            status="delivered",
            message="SMS successfully forwarded to Telegram.",
            telegram_message_id=msg_id
        )
    except Exception as e:
        raise HTTPException(status_code=502, detail=f"Telegram API delivery failed: {str(e)}")
