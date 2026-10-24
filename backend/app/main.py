import asyncio
import logging
from contextlib import asynccontextmanager
from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware
from .config import settings
from .database import engine, Base
from .telegram_bot import create_bot_app
from .routers import pair, forward

logging.basicConfig(level=logging.INFO, format="%(asctime)s - %(name)s - %(levelname)s - %(message)s")
logger = logging.getLogger("sms_relay")

bot_application = None

@asynccontextmanager
async def lifespan(app: FastAPI):
    # Startup: Create tables
    Base.metadata.create_all(bind=engine)
    logger.info("Database tables initialized.")

    # Start Telegram Bot polling in background
    global bot_application
    bot_application = create_bot_app()
    if bot_application:
        await bot_application.initialize()
        await bot_application.start()
        await bot_application.updater.start_polling()
        logger.info("Telegram Bot polling started successfully.")

    yield

    # Shutdown: Stop bot
    if bot_application and bot_application.updater:
        await bot_application.updater.stop()
        await bot_application.stop()
        await bot_application.shutdown()
        logger.info("Telegram Bot stopped.")

app = FastAPI(
    title="SMS to Telegram Relay API",
    version="1.0.0",
    description="Stateless SMS forwarding service directly to your Telegram chat.",
    lifespan=lifespan
)

app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

app.include_router(pair.router)
app.include_router(forward.router)

@app.get("/")
def root():
    return {
        "service": "SMS to Telegram Relay",
        "status": "online",
        "docs": "/docs"
    }

@app.get("/api/v1/health")
def health():
    return {
        "status": "healthy",
        "bot_configured": bool(settings.TELEGRAM_BOT_TOKEN)
    }

if __name__ == "__main__":
    import uvicorn
    uvicorn.run("app.main:app", host=settings.HOST, port=settings.PORT, reload=True)
