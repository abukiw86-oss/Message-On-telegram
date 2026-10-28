import os
from pathlib import Path
from dotenv import load_dotenv
from pydantic_settings import BaseSettings

# Explicitly locate .env relative to the backend directory
BACKEND_DIR = Path(__file__).resolve().parent.parent
ENV_FILE = BACKEND_DIR / ".env"

if ENV_FILE.exists():
    load_dotenv(ENV_FILE)
elif (Path.home() / ".env").exists():
    load_dotenv(Path.home() / ".env")
elif (Path.home() / "backend" / ".env").exists():
    load_dotenv(Path.home() / "backend" / ".env")
elif (Path.home() / "backend_app" / ".env").exists():
    load_dotenv(Path.home() / "backend_app" / ".env")

class Settings(BaseSettings):
    TELEGRAM_BOT_TOKEN: str = os.getenv("TELEGRAM_BOT_TOKEN", "")
    DATABASE_URL: str = os.getenv("DATABASE_URL", "sqlite:///./data.db")
    HOST: str = os.getenv("HOST", "0.0.0.0")
    PORT: int = int(os.getenv("PORT", "8000"))
    SECRET_KEY: str = os.getenv("SECRET_KEY", "default_secret_key_message_on_telegram")
    PAIRING_CODE_EXPIRY_MINUTES: int = 15

    class Config:
        env_file = str(ENV_FILE) if ENV_FILE.exists() else ".env"
        extra = "ignore"

settings = Settings()
