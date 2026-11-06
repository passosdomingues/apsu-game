#!/usr/bin/env python3
"""Valida os arquivos de sprint e imprime o resumo.

Regras: pontuacao Fibonacci ate 8, minimo de 4 criterios de aceite, minimo de 2 tarefas,
soma de pontos igual ao 'Planejado' e menor ou igual a 'Capacidade'.
Uso: python3 .claude/tools/sprint_report.py [--story ID] [--quiet]
"""
import argparse
import glob
import os
import re
import sys

FIB = {1, 2, 3, 5, 8}
HEADER = re.compile(r"^### (APSU-[A-Z]\d+) \| (.+?) \| (\d+) pts \| (.+)$")


def load(root):
    sprints = []
    for path in sorted(glob.glob(os.path.join(root, "sprints", "sprint-*.md"))):
        text = open(path, encoding="utf-8").read()
        title = text.splitlines()[0].lstrip("# ").strip()
        cap = re.search(r"Capacidade: (\d+) pontos\. Planejado: (\d+)\.", text)
        stories, current = [], None
        for line in text.splitlines():
            match = HEADER.match(line)
            if match:
                current = {"id": match.group(1), "title": match.group(2), "pts": int(match.group(3)),
                           "owner": match.group(4), "ac": 0, "tasks": 0, "done": 0, "text": []}
                stories.append(current)
                continue
            if current is None:
                continue
            current["text"].append(line)
            if re.match(r"^- AC\d+:", line):
                current["ac"] += 1
            if re.match(r"^- \[[ x]\] ", line):
                current["tasks"] += 1
                if line.startswith("- [x]"):
                    current["done"] += 1
        sprints.append({"path": path, "title": title, "cap": int(cap.group(1)) if cap else None,
                        "planned": int(cap.group(2)) if cap else None, "stories": stories})
    return sprints


def validate(sprints):
    errors, seen = [], set()
    for sprint in sprints:
        name = os.path.basename(sprint["path"])
        if sprint["cap"] is None:
            errors.append("%s: linha 'Capacidade: N pontos. Planejado: N.' ausente" % name)
            continue
        total = sum(s["pts"] for s in sprint["stories"])
        if total != sprint["planned"]:
            errors.append("%s: soma das stories %d difere de Planejado %d" % (name, total, sprint["planned"]))
        if total > sprint["cap"]:
            errors.append("%s: soma %d excede a capacidade %d" % (name, total, sprint["cap"]))
        for story in sprint["stories"]:
            label = "%s/%s" % (name, story["id"])
            if story["id"] in seen:
                errors.append("%s: id duplicado" % label)
            seen.add(story["id"])
            if story["pts"] not in FIB:
                errors.append("%s: %d pts nao e Fibonacci permitido (1, 2, 3, 5, 8)" % (label, story["pts"]))
            if story["ac"] < 4:
                errors.append("%s: apenas %d criterios de aceite (minimo 4)" % (label, story["ac"]))
            if story["tasks"] < 2:
                errors.append("%s: apenas %d tarefas (minimo 2)" % (label, story["tasks"]))
            body = "\n".join(story["text"])
            if not re.search(r"^Como .+, quero .+, para .+\.$", body, re.M):
                errors.append("%s: falta a frase 'Como ..., quero ..., para ...'" % label)
            if "Complexidade %d" % story["pts"] not in body:
                errors.append("%s: falta a justificativa 'Complexidade %d'" % (label, story["pts"]))
    return errors


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--story")
    parser.add_argument("--quiet", action="store_true")
    args = parser.parse_args()
    root = os.path.join(os.environ.get("CLAUDE_PROJECT_DIR") or os.getcwd(), ".claude")
    if not os.path.isdir(root):
        root = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
    sprints = load(root)
    errors = validate(sprints)
    if args.story:
        for sprint in sprints:
            for story in sprint["stories"]:
                if story["id"] == args.story:
                    print("### %s | %s | %d pts | %s" % (story["id"], story["title"], story["pts"], story["owner"]))
                    print("\n".join(story["text"]).strip())
                    return 0
        print("Story nao encontrada: " + args.story)
        return 1
    if not args.quiet:
        grand = 0
        for sprint in sprints:
            pts = sum(s["pts"] for s in sprint["stories"])
            done = sum(s["pts"] for s in sprint["stories"] if s["tasks"] and s["done"] == s["tasks"])
            grand += pts
            print("%s | %d de %d pts | concluidos %d pts | %d stories" % (sprint["title"], pts, sprint["cap"] or 0, done, len(sprint["stories"])))
            for story in sprint["stories"]:
                mark = "x" if story["tasks"] and story["done"] == story["tasks"] else " "
                print("  [%s] %s  %d pts  %s  (%s)" % (mark, story["id"], story["pts"], story["title"], story["owner"]))
        print("Total: %d pts" % grand)
    for error in errors:
        print("ERRO: " + error)
    return 1 if errors else 0


if __name__ == "__main__":
    sys.exit(main())
