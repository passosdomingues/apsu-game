#!/usr/bin/env python3
"""Wrapper para chamar tools/harness/hooks/apsu_hook.py a partir de .claude/hooks/"""
import os
import sys

HERE = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.dirname(os.path.dirname(HERE))
TARGET = os.path.join(ROOT, "tools", "harness", "hooks", "apsu_hook.py")

if __name__ == "__main__":
    if not os.path.isfile(TARGET):
        sys.stderr.write("apsu_hook.py alvo nao encontrado: %s\n" % TARGET)
        sys.exit(1)
    os.execv(sys.executable, [sys.executable, TARGET] + sys.argv[1:])
