#!/usr/bin/env python3
"""Instala o harness APSU de forma idempotente e sem sobrescrever o que existe.

Somente biblioteca padrao. Uso: python3 tools/harness/install.py

Faz, nesta ordem:
  1. torna os hooks executaveis;
  2. cria ou mescla .claude/settings.json (uniao de permissoes e hooks);
  3. insere o bloco gerenciado em CLAUDE.md (cria o arquivo se nao existir);
  4. insere o bloco gerenciado em DEVELOPMENT.md (regra 09 do AGENTS.md);
  5. acrescenta entradas em .gitignore (regra 07);
  6. acrescenta uma unica linha -include no fim do Makefile.
"""
import json
import os
import stat
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
HARNESS = ROOT / "tools" / "harness"
TEMPLATES = HARNESS / "templates"
changed = []


def mark(label, did):
    print(("alterado " if did else "sem mudanca ") + label)
    if did:
        changed.append(label)


def write_if_different(path, text):
    old = path.read_text(encoding="utf-8") if path.exists() else None
    if old == text:
        return False
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(text, encoding="utf-8", newline="\n")
    return True


def chmod_hooks():
    did = False
    for f in (HARNESS / "hooks").glob("*.py"):
        mode = f.stat().st_mode
        if not mode & stat.S_IXUSR:
            f.chmod(mode | stat.S_IXUSR | stat.S_IXGRP | stat.S_IXOTH)
            did = True
    for f in HARNESS.glob("*.py"):
        mode = f.stat().st_mode
        if not mode & stat.S_IXUSR:
            f.chmod(mode | stat.S_IXUSR | stat.S_IXGRP | stat.S_IXOTH)
            did = True
    mark("permissoes de execucao", did)


def merge_unique(dst, src):
    for item in src:
        if item not in dst:
            dst.append(item)


def merge_settings():
    target = ROOT / ".claude" / "settings.json"
    new = json.loads((TEMPLATES / "settings.json").read_text(encoding="utf-8"))
    if target.exists():
        cur = json.loads(target.read_text(encoding="utf-8"))
    else:
        cur = {}
    cur.setdefault("$schema", new["$schema"])
    perms = cur.setdefault("permissions", {})
    for key in ("allow", "ask", "deny"):
        merge_unique(perms.setdefault(key, []), new["permissions"][key])
    hooks = cur.setdefault("hooks", {})
    for event, groups in new["hooks"].items():
        cur_groups = hooks.setdefault(event, [])
        for group in groups:
            match = next((g for g in cur_groups if g.get("matcher") == group.get("matcher")), None)
            if match is None:
                cur_groups.append(group)
                continue
            existing = {h.get("command") for h in match.get("hooks", [])}
            for h in group["hooks"]:
                if h["command"] not in existing:
                    match.setdefault("hooks", []).append(h)
    text = json.dumps(cur, indent=2, ensure_ascii=False) + "\n"
    mark(".claude/settings.json", write_if_different(target, text))


def upsert_block(path, marker, body, create_header=""):
    begin, end = f"<!-- {marker}:begin -->", f"<!-- {marker}:end -->"
    block = f"{begin}\n{body.rstrip()}\n{end}\n"
    if path.exists():
        text = path.read_text(encoding="utf-8")
        if begin in text and end in text:
            head, rest = text.split(begin, 1)
            _, tail = rest.split(end, 1)
            new = head + block.rstrip("\n") + tail
        else:
            sep = "" if text.endswith("\n\n") else ("\n" if text.endswith("\n") else "\n\n")
            new = text + sep + block
    else:
        new = create_header + block
    return write_if_different(path, new)


def ensure_lines(path, lines):
    text = path.read_text(encoding="utf-8") if path.exists() else ""
    have = set(text.splitlines())
    missing = [l for l in lines if l not in have]
    if not missing:
        return False
    sep = "" if (not text or text.endswith("\n")) else "\n"
    path.write_text(text + sep + "\n".join(missing) + "\n", encoding="utf-8", newline="\n")
    return True


def main():
    chmod_hooks()
    merge_settings()
    claude_body = (TEMPLATES / "CLAUDE.block.md").read_text(encoding="utf-8")
    mark("CLAUDE.md", upsert_block(ROOT / "CLAUDE.md", "apsu-harness", claude_body))
    dev = ROOT / "DEVELOPMENT.md"
    dev_body = (TEMPLATES / "DEVELOPMENT.block.md").read_text(encoding="utf-8")
    mark("DEVELOPMENT.md", upsert_block(dev, "apsu-harness", dev_body, "# Desenvolvimento\n\n"))
    mark(".gitignore", ensure_lines(ROOT / ".gitignore", [".claude/state/", ".claude/settings.local.json"]))
    makefile = ROOT / "Makefile"
    if makefile.exists():
        mark("Makefile", ensure_lines(makefile, ["", "# Alvos auxiliares do harness APSU (tools/harness/harness.mk)", "-include tools/harness/harness.mk"])
             if "-include tools/harness/harness.mk" not in makefile.read_text(encoding="utf-8") else False)
    else:
        print("Makefile nao encontrado: nenhum alvo auxiliar incluido")
    print(f"{len(changed)} arquivo(s) alterado(s). Execute: python3 tools/harness/verify.py")
    return 0


if __name__ == "__main__":
    sys.exit(main())
