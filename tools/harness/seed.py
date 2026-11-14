#!/usr/bin/env python3
"""Semeador ("Dandelion Seed") do Harness APSU.

Implementa a replicacao de Carl Sagan / Persistence of Memory:
O harness funciona como uma semente: pequena, completa e capaz de germinar
em outro repositorio/projeto sem depender do projeto de origem.

Capacidades:
  1. Exporta o nucleo do harness (FSM, hooks, verificador, templates, scripts)
     para um pacote semente autocontido ou diretamente para um diretorio alvo.
  2. Registra o lineage log com a arvore genealogica da semente e checksums.
  3. Pode inicializar novos projetos com Make, regras de zero-allocation e
     maquina de estados deterministica.
"""
import argparse
import hashlib
import json
import os
import shutil
import sys
import time
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
SEED_MANIFEST = [
    "AGENTS.md",
    "tools/harness/harness.mk",
    "tools/harness/verify.py",
    "tools/harness/backlog.py",
    "tools/harness/fsm.py",
    "tools/harness/seed.py",
    "tools/harness/install.py",
    "tools/harness/hooks/apsu_hook.py",
    "tools/harness/templates/CLAUDE.block.md",
    "tools/harness/templates/settings.json",
    "tools/harness/templates/DEVELOPMENT.block.md",
]


def compute_sha(filepath):
    p = Path(filepath)
    if not p.exists():
        return None
    return hashlib.sha256(p.read_bytes()).hexdigest()


def seed_to_target(target_dir):
    target_root = Path(target_dir).resolve()
    print(f"[SEED] Semeando harness em: {target_root}")
    
    if not target_root.exists():
        target_root.mkdir(parents=True, exist_ok=True)

    manifest_records = []
    
    for rel_path in SEED_MANIFEST:
        src = ROOT / rel_path
        dst = target_root / rel_path
        if not src.exists():
            print(f"[WARN] Arquivo fonte nao encontrado: {rel_path}")
            continue
        dst.parent.mkdir(parents=True, exist_ok=True)
        shutil.copy2(src, dst)
        sha = compute_sha(dst)
        manifest_records.append({"path": rel_path, "sha256": sha})
        print(f"  + {rel_path} ({sha[:8]}...)")

    # Gera lineage log
    lineage_file = target_root / "tools" / "harness" / "seed_lineage.json"
    lineage_data = {
        "origin_repository": "apsu-game",
        "seed_version": "1.0.0-fsm",
        "germinated_at": time.strftime("%Y-%m-%dT%H:%M:%SZ"),
        "files_transferred": len(manifest_records),
        "manifest": manifest_records,
    }
    lineage_file.parent.mkdir(parents=True, exist_ok=True)
    lineage_file.write_text(json.dumps(lineage_data, indent=2), encoding="utf-8")
    print(f"[SEED] Linhagem gravada em {lineage_file.relative_to(target_root)}")

    # Gera os checksums locais no destino
    checksums_file = target_root / "tools" / "harness" / "core_checksums.sha256"
    lines = ["# SHA-256 do nucleo protegido do harness APSU germinado"]
    for item in manifest_records:
        if item["path"] in ("AGENTS.md", "tools/harness/verify.py", "tools/harness/hooks/apsu_hook.py", "tools/harness/fsm.py"):
            lines.append(f"{item['sha256']}  {item['path']}")
    checksums_file.write_text("\n".join(lines) + "\n", encoding="utf-8")
    print(f"[SEED] Checksums protegidos gerados em {checksums_file.relative_to(target_root)}")
    print("\n[SEED] Semeadura concluida! No destino, rode: python3 tools/harness/install.py")


def main():
    parser = argparse.ArgumentParser(description="APSU Harness Dandelion Seed Replicator")
    parser.add_argument("target_path", help="Caminho do diretorio destino onde a semente ira germinar")
    args = parser.parse_args()

    seed_to_target(args.target_path)
    return 0


if __name__ == "__main__":
    sys.exit(main())
