import asyncio
import logging
from contextlib import asynccontextmanager
from fastapi import FastAPI, Request
from fastapi.middleware.cors import CORSMiddleware
from telegram import Update
from .config import settings
from .database import engine, Base
from .telegram_bot import create_bot_app
from .routers import pair, forward

logging.basicConfig(level=logging.INFO, format="%(asctime)s - %(name)s - %(levelname)s - %(message)s")
logger = logging.getLogger("sms_relay")

# Ensure database tables exist upon module loading (works under ASGI & uWSGI)
try:
    Base.metadata.create_all(bind=engine)
    logger.info("Database tables verified/initialized successfully.")
except Exception as e:
    logger.error(f"Error initializing database tables: {e}")

bot_application = None

async def get_or_create_bot_app():
    global bot_application
    if bot_application is None:
        bot_application = create_bot_app()
        if bot_application:
            await bot_application.initialize()
            await bot_application.start()
    return bot_application

@asynccontextmanager
async def lifespan(app: FastAPI):
    # Startup
    bot_app = await get_or_create_bot_app()
    if bot_app and hasattr(bot_app, "updater") and bot_app.updater:
        try:
            await bot_app.updater.start_polling()
            logger.info("Telegram Bot polling started.")
        except Exception as e:
            logger.warning(f"Telegram polling startup skipped (webhook mode likely active): {e}")

    yield

    # Shutdown
    global bot_application
    if bot_application:
        try:
            if hasattr(bot_application, "updater") and bot_application.updater:
                await bot_application.updater.stop()
            await bot_application.stop()
            await bot_application.shutdown()
            logger.info("Telegram Bot stopped cleanly.")
        except Exception as e:
            logger.warning(f"Error during bot shutdown: {e}")

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
        "bot_configured": bool(settings.TELEGRAM_BOT_TOKEN),
        "database": "connected"
    }

@app.post("/api/v1/telegram-webhook")
async def telegram_webhook(request: Request):
    """Stateless webhook handler for Telegram bot updates (perfect for uWSGI/Alwaysdata)."""
    bot_app = await get_or_create_bot_app()
    if not bot_app:
        return {"status": "error", "message": "Bot not configured"}

    try:
        data = await request.json()
        update = Update.de_json(data, bot_app.bot)
        await bot_app.process_update(update)
        return {"status": "ok"}
    except Exception as e:
        logger.error(f"Error in telegram webhook processing: {e}")
        return {"status": "error", "message": str(e)}

if __name__ == "__main__":
    import uvicorn
    uvicorn.run("app.main:app", host=settings.HOST, port=settings.PORT, reload=True)
