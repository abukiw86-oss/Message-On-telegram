import os
import posixpath
import paramiko
import httpx

REMOTE_HOST = "ssh-abubeker.alwaysdata.net"
REMOTE_USER = "abubeker"
REMOTE_PASS = "_mGtfzP*fqw56dj"
REMOTE_DIR = "/home/abubeker/backend"
DOMAIN = "abubeker.alwaysdata.net"
BOT_TOKEN = "8498137205:AAFrRRZ-TcyQENK15iDfwvCm4z7C1qqBCu0"
MYSQL_URL = f"mysql+pymysql://{REMOTE_USER}:{REMOTE_PASS}@mysql-{REMOTE_USER}.alwaysdata.net/{REMOTE_USER}_message_on_telegram?charset=utf8mb4"

def upload_directory(sftp, local_dir, remote_dir):
    try:
        sftp.mkdir(remote_dir)
    except IOError:
        pass

    for item in os.listdir(local_dir):
        if item in [".git", "__pycache__", "venv", ".pytest_cache", ".env"]:
            continue
        local_path = os.path.join(local_dir, item)
        remote_path = posixpath.join(remote_dir, item)

        if os.path.isdir(local_path):
            upload_directory(sftp, local_path, remote_path)
        else:
            print(f"Uploading {item} -> {remote_path}")
            sftp.put(local_path, remote_path)

def main():
    print(f"Connecting to {REMOTE_HOST}...")
    client = paramiko.SSHClient()
    client.set_missing_host_key_policy(paramiko.AutoAddPolicy())
    client.connect(
        hostname=REMOTE_HOST,
        username=REMOTE_USER,
        password=REMOTE_PASS,
        timeout=20
    )

    sftp = client.open_sftp()
    local_backend_dir = os.path.dirname(os.path.abspath(__file__))

    print("Syncing backend files...")
    upload_directory(sftp, local_backend_dir, REMOTE_DIR)

    # Write remote production .env with MySQL configuration
    print("Configuring production .env on remote server...")
    remote_env_content = f"""# Production Configuration on Alwaysdata
TELEGRAM_BOT_TOKEN={BOT_TOKEN}
DATABASE_URL={MYSQL_URL}
HOST=0.0.0.0
PORT=8000
SECRET_KEY=message_on_telegram_secret_key_prod_2026
"""
    remote_env_path = posixpath.join(REMOTE_DIR, ".env")
    with sftp.file(remote_env_path, "w") as f:
        f.write(remote_env_content)
    print("Remote .env file created successfully!")

    sftp.close()

    print("Setting up virtual environment & installing dependencies (including pymysql)...")
    commands = [
        f"python3 -m venv {REMOTE_DIR}/venv",
        f"{REMOTE_DIR}/venv/bin/pip install --upgrade pip",
        f"{REMOTE_DIR}/venv/bin/pip install -r {REMOTE_DIR}/requirements.txt a2wsgi pymysql cryptography",
        # Verify and initialize MySQL tables
        f"PYTHONPATH={REMOTE_DIR} {REMOTE_DIR}/venv/bin/python -c 'from app.database import engine, Base; from app.models import TelegramUser, PairingCode, Device; Base.metadata.create_all(bind=engine); print(\"MySQL database tables created/verified successfully!\")'",
        # Reload WSGI app by touching wsgi.py
        "cp /home/abubeker/backend/.env /home/abubeker/www/.env 2>/dev/null || true",
        "touch /home/abubeker/www/wsgi.py",
    ]

    for cmd in commands:
        print(f"\n[Executing]: {cmd}")
        stdin, stdout, stderr = client.exec_command(cmd)
        out = stdout.read().decode()
        err = stderr.read().decode()
        if out:
            print(out.strip())
        if err:
            print(f"STDERR: {err.strip()}")

    client.close()

    # Configure Telegram Webhook
    print("\nRegistering Telegram Webhook with Alwaysdata...")
    webhook_url = f"https://{DOMAIN}/api/v1/telegram-webhook"
    set_webhook_url = f"https://api.telegram.org/bot{BOT_TOKEN}/setWebhook"
    try:
        with httpx.Client(timeout=10.0) as http_client:
            resp = http_client.post(set_webhook_url, json={"url": webhook_url, "drop_pending_updates": False})
            print(f"Telegram Webhook Registration Response: {resp.status_code} - {resp.text}")
    except Exception as e:
        print(f"Error registering Telegram Webhook: {e}")

    # Verify Health Endpoint
    print("\nVerifying live server health...")
    health_url = f"https://{DOMAIN}/api/v1/health"
    try:
        with httpx.Client(timeout=10.0) as http_client:
            resp = http_client.get(health_url)
            print(f"Server Health check ({health_url}): {resp.status_code} - {resp.text}")
    except Exception as e:
        print(f"Error checking health endpoint: {e}")

    print("\n🎉 Deployment completed successfully!")

if __name__ == "__main__":
    main()
