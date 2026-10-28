import os
import posixpath
import time
import socket
import paramiko
import httpx

REMOTE_HOST = "ssh-msgrelay.alwaysdata.net"
FALLBACK_IP = "185.31.41.84"
REMOTE_PORT = 22
REMOTE_USER = "msgrelay"
REMOTE_PASS = "pk!BVuFcN6Lu9jK"
APP_DIR = "/home/msgrelay/backend_app"
DOMAIN = "msgrelay.alwaysdata.net"
BOT_TOKEN = "9091835648:AAHmD_crnMn6PnK9rFeeRAKbS4ppXLoRhCE"
DATABASE_URL = "mysql+pymysql://abubeker:_mGtfzP*fqw56dj@mysql-abubeker.alwaysdata.net/abubeker_message_on_telegram?charset=utf8mb4"

def get_connectable_host():
    try:
        socket.gethostbyname(REMOTE_HOST)
        return REMOTE_HOST
    except Exception:
        print(f"DNS lookup for {REMOTE_HOST} failed. Using direct IP {FALLBACK_IP}...")
        return FALLBACK_IP

def upload_directory(sftp, local_dir, remote_dir):
    try:
        sftp.mkdir(remote_dir)
    except IOError:
        pass

    for item in sorted(os.listdir(local_dir)):
        if item in [".git", "__pycache__", "venv", ".pytest_cache", ".env", "deploy.py"]:
            continue
        local_path = os.path.join(local_dir, item)
        remote_path = posixpath.join(remote_dir, item)

        if os.path.isdir(local_path):
            upload_directory(sftp, local_path, remote_path)
        else:
            print(f"Uploading {item} -> {remote_path}")
            sftp.put(local_path, remote_path)

def main():
    target_host = get_connectable_host()
    print(f"Connecting to {target_host} as {REMOTE_USER}...")
    client = paramiko.SSHClient()
    client.set_missing_host_key_policy(paramiko.AutoAddPolicy())
    client.connect(
        hostname=target_host,
        port=REMOTE_PORT,
        username=REMOTE_USER,
        password=REMOTE_PASS,
        timeout=25
    )

    print("Cleaning up stale files on remote server before deployment...")
    stdin, stdout, stderr = client.exec_command(f"rm -rf {APP_DIR} /home/{REMOTE_USER}/backend /home/{REMOTE_USER}/.env")
    stdout.channel.recv_exit_status()

    sftp = client.open_sftp()
    local_backend_dir = os.path.dirname(os.path.abspath(__file__))

    print("Syncing fresh backend files to remote server...")
    upload_directory(sftp, local_backend_dir, APP_DIR)

    # Write production .env configuration
    print("Writing remote .env configuration...")
    remote_env = f"""# Production Configuration on Alwaysdata
TELEGRAM_BOT_TOKEN={BOT_TOKEN}
DATABASE_URL={DATABASE_URL}
HOST=0.0.0.0
PORT=8000
SECRET_KEY=message_on_telegram_secret_key_prod_2026
"""
    with sftp.file(posixpath.join(APP_DIR, ".env"), "w") as f:
        f.write(remote_env)
    with sftp.file(f"/home/{REMOTE_USER}/.env", "w") as f:
        f.write(remote_env)

    # Write WSGI entrypoint script (executed by Alwaysdata's uWSGI configuration)
    print("Writing uWSGI entrypoint script...")
    wsgi_content = f"""#!/usr/bin/env python3
import os
import sys

BASE_DIR = "{APP_DIR}"
VENV_PACKAGES = f"{{BASE_DIR}}/venv/lib/python3.13/site-packages"

if os.path.exists(VENV_PACKAGES) and VENV_PACKAGES not in sys.path:
    sys.path.insert(0, VENV_PACKAGES)
if BASE_DIR not in sys.path:
    sys.path.insert(0, BASE_DIR)

from dotenv import load_dotenv
load_dotenv(os.path.join(BASE_DIR, ".env"))

from a2wsgi import ASGIMiddleware
from app.main import app

application = ASGIMiddleware(app)
"""
    with sftp.file(f"/home/{REMOTE_USER}/backend", "w") as f:
        f.write(wsgi_content)

    sftp.close()

    print("Setting up virtual environment, installing dependencies & creating tables on MySQL...")
    commands = [
        f"chmod +x /home/{REMOTE_USER}/backend",
        f"python3 -m venv {APP_DIR}/venv",
        f"{APP_DIR}/venv/bin/pip install --upgrade pip",
        f"{APP_DIR}/venv/bin/pip install -r {APP_DIR}/requirements.txt a2wsgi pymysql cryptography",
        f"PYTHONPATH={APP_DIR} {APP_DIR}/venv/bin/python -c 'from app.database import engine, Base; from app.models import TelegramUser, PairingCode, Device; Base.metadata.create_all(bind=engine); print(\"MySQL database tables created/verified successfully!\")'",
        # Register Telegram Webhook directly from server
        f"curl -s -X POST https://api.telegram.org/bot{BOT_TOKEN}/setWebhook -d 'url=https://{DOMAIN}/api/v1/telegram-webhook&drop_pending_updates=false'",
        # Touch WSGI script and restart uWSGI worker
        f"touch /home/{REMOTE_USER}/backend",
        "pkill -f uwsgi || true"
    ]

    for cmd in commands:
        print(f"\n[Executing]: {cmd}")
        stdin, stdout, stderr = client.exec_command(cmd)
        out = stdout.read().decode().strip()
        err = stderr.read().decode().strip()
        if out:
            print(out)
        if err:
            print(f"STDERR: {err}")

    # Health check from the server
    time.sleep(2)
    stdin, stdout, stderr = client.exec_command(f"curl -s -i https://{DOMAIN}/api/v1/health")
    health_resp = stdout.read().decode()
    print("\n[Live Server Health Check]:\n" + health_resp)

    client.close()
    print(f"\n🎉 Deployment to {DOMAIN} completed successfully!")

if __name__ == "__main__":
    main()
