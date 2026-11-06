#!/usr/bin/env python3
"""Audita triangulos e materiais dos OBJ contra o orcamento do projeto.

Uso: python3 .claude/tools/mesh_budget.py [diretorio] [--only hero|enemy|boss|npc|scenery]...
Saida 0 se todos os modelos filtrados passam, 1 caso contrario.
"""
import argparse
import os
import sys

BUDGET = {
    "hero": (6000, 6),
    "enemy": (4000, 3),
    "boss": (12000, 6),
    "npc": (6000, 4),
    "scenery": (2500, 2),
}


def classify(path):
    parts = path.replace("\\", "/").split("/")
    name = os.path.basename(os.path.dirname(path))
    if "scenery" in parts:
        return "scenery"
    if name.startswith("01_"):
        return "hero"
    if name.startswith("02_"):
        return "boss"
    if name.startswith(("03_", "05_")):
        return "npc"
    return "enemy"


def inspect(path):
    triangles, materials = 0, set()
    with open(path, encoding="utf-8", errors="replace") as handle:
        for line in handle:
            if line.startswith("f "):
                triangles += max(0, len(line.split()) - 3)
            elif line.startswith("usemtl "):
                materials.add(line[7:].strip())
    return triangles, len(materials)


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("root", nargs="?", default="src/main/resources/models")
    parser.add_argument("--only", action="append", choices=sorted(BUDGET))
    args = parser.parse_args()
    if not os.path.isdir(args.root):
        print("Diretorio nao encontrado: " + args.root)
        return 2
    rows, failed = [], 0
    for folder, _, files in os.walk(args.root):
        for name in sorted(files):
            if not name.endswith(".obj"):
                continue
            path = os.path.join(folder, name)
            kind = classify(path)
            if args.only and kind not in args.only:
                continue
            tris, mats = inspect(path)
            max_t, max_m = BUDGET[kind]
            ok = tris <= max_t and mats <= max_m
            failed += 0 if ok else 1
            rows.append((kind, os.path.relpath(path, args.root), tris, max_t, mats, max_m, ok))
    rows.sort(key=lambda r: (r[0], -r[2]))
    print("%-8s %-62s %8s %8s %5s %5s  %s" % ("tipo", "modelo", "tris", "limite", "mats", "limite", "ok"))
    for kind, name, tris, max_t, mats, max_m, ok in rows:
        print("%-8s %-62s %8d %8d %5d %5d  %s" % (kind, name[:62], tris, max_t, mats, max_m, "sim" if ok else "NAO"))
    print("%d modelos, %d acima do orcamento" % (len(rows), failed))
    return 1 if failed else 0


if __name__ == "__main__":
    sys.exit(main())
