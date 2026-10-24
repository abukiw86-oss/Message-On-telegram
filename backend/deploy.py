import os
import posixpath
import paramiko

REMOTE_HOST = "ssh-abubeker.alwaysdata.net"
REMOTE_USER = "abubeker"
REMOTE_PASS = "_mGtfzP*fqw56dj"
REMOTE_DIR = "/home/abubeker/backend"

def upload_directory(sftp, local_dir, remote_dir):
    try:
        sftp.mkdir(remote_dir)
    except IOError:
        pass

    for item in os.listdir(local_dir):
        if item in [".git", "__pycache__", "venv", ".pytest_cache"]:
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
        timeout=15
    )

    sftp = client.open_sftp()
    local_backend_dir = os.path.dirname(os.path.abspath(__file__))

    print("Syncing backend files...")
    upload_directory(sftp, local_backend_dir, REMOTE_DIR)
    sftp.close()

    print("Setting up virtual environment and dependencies on remote server...")
    commands = [
        f"python3 -m venv {REMOTE_DIR}/venv",
        f"{REMOTE_DIR}/venv/bin/pip install --upgrade pip",
        f"{REMOTE_DIR}/venv/bin/pip install -r {REMOTE_DIR}/requirements.txt a2wsgi",
    ]

    for cmd in commands:
        print(f"Executing: {cmd}")
        stdin, stdout, stderr = client.exec_command(cmd)
        out = stdout.read().decode()
        err = stderr.read().decode()
        if out:
            print(out)
        if err:
            print(f"ERR: {err}")

    client.close()
    print("Deployment completed successfully!")

if __name__ == "__main__":
    main()
