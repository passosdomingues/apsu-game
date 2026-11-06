"""Utilitarios compartilhados pelos hooks. Somente biblioteca padrao."""
import json
import os
import subprocess
import sys

PROJECT = os.environ.get("CLAUDE_PROJECT_DIR") or os.getcwd()
STATE = os.path.join(PROJECT, ".claude", "state")


def read_payload():
    try:
        raw = sys.stdin.read()
        return json.loads(raw) if raw.strip() else {}
    except (ValueError, OSError):
        return {}


def block(message):
    """Bloqueia a acao: stderr volta para o Claude, codigo 2."""
    sys.stderr.write(message.rstrip() + "\n")
    sys.exit(2)


def read_state(name, default=""):
    try:
        with open(os.path.join(STATE, name), encoding="utf-8") as handle:
            return handle.read().strip()
    except OSError:
        return default


def write_state(name, value):
    os.makedirs(STATE, exist_ok=True)
    with open(os.path.join(STATE, name), "w", encoding="utf-8") as handle:
        handle.write(value)


def git(*args, timeout=8):
    try:
        out = subprocess.run(
            ["git", *args], cwd=PROJECT, capture_output=True, text=True, timeout=timeout
        )
        return out.stdout.strip() if out.returncode == 0 else ""
    except (OSError, subprocess.SubprocessError):
        return ""


def rel(path):
    if not path:
        return ""
    absolute = os.path.abspath(path if os.path.isabs(path) else os.path.join(PROJECT, path))
    try:
        return os.path.relpath(absolute, PROJECT).replace(os.sep, "/")
    except ValueError:
        return absolute
