from typing import Optional
from pydantic import BaseModel

class PairRequest(BaseModel):
    pairing_code: str
    device_name: Optional[str] = "Android Phone"

class TelegramUserInfo(BaseModel):
    first_name: Optional[str] = None
    username: Optional[str] = None

class PairResponse(BaseModel):
    status: str
    device_token: str
    user: TelegramUserInfo

class ForwardMessageRequest(BaseModel):
    sender: str
    body: str
    received_at: Optional[int] = None
    sim_slot: Optional[int] = None

class ForwardMessageResponse(BaseModel):
    status: str
    message: str
    telegram_message_id: Optional[int] = None

class DeviceStatusResponse(BaseModel):
    device_name: str
    is_active: bool
    user: TelegramUserInfo
