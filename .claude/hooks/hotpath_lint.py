#!/usr/bin/env python3
"""PostToolUse: acusa alocacao e chamadas caras nas linhas alteradas de metodos quentes (regra 11)."""
import os
import re

from _common import PROJECT, block, git, read_payload, rel

payload = read_payload()
path = rel((payload.get("tool_input") or {}).get("file_path", ""))
if not path.endswith(".java") or not path.startswith("src/main/"):
    raise SystemExit(0)
absolute = os.path.join(PROJECT, path)
try:
    with open(absolute, encoding="utf-8") as handle:
        lines = handle.read().splitlines()
except OSError:
    raise SystemExit(0)

HOT = re.compile(
    r"^\s*(?:public|private|protected|static|final|synchronized|\s)*[\w<>\[\], ?]+\s+"
    r"(update\w*|render\w*|draw\w*|paint\w*|tick|step|handle|physics\w*|updatePhysics|applyEnvironmentalForces|checkEnemyCollisions|checkSceneryCollisions)\s*\([^;]*\)\s*(?:throws [\w, .]+)?\s*\{"
)
BANNED = [
    (re.compile(r"\bnew\s+[A-Z]\w*(?:<[^>]*>)?\s*\("), "alocacao com new"),
    (re.compile(r"String\.format\s*\("), "String.format"),
    (re.compile(r"\bFont\.font\s*\("), "Font.font"),
    (re.compile(r"\bColor\.(web|rgb|color|hsb)\s*\("), "criacao de Color"),
    (re.compile(r"\.deriveColor\s*\("), "deriveColor"),
    (re.compile(r"\.stream\s*\("), "stream"),
    (re.compile(r"->"), "lambda"),
    (re.compile(r"System\.(out|err)\.print"), "print no loop"),
    (re.compile(r"Thread\.sleep"), "Thread.sleep"),
    (re.compile(r"getResource(AsStream)?\s*\("), "carga de recurso"),
    (re.compile(r"\bnew\s+(?:int|double|float|long)\s*\["), "alocacao de array"),
]
IGNORE = re.compile(r"//\s*hot-alloc-ok")

hot_lines = set()
index = 0
while index < len(lines):
    if HOT.match(lines[index]) and not lines[index].strip().startswith(("//", "*")):
        depth = 0
        started = False
        end = index
        for cursor in range(index, len(lines)):
            depth += lines[cursor].count("{") - lines[cursor].count("}")
            started = started or "{" in lines[cursor]
            if started and depth <= 0:
                end = cursor
                break
        else:
            end = len(lines) - 1
        for cursor in range(index + 1, end):
            hot_lines.add(cursor + 1)
        index = end
    index += 1

changed = None
diff = git("diff", "HEAD", "-U0", "--", path)
if diff:
    changed = set()
    for header in re.finditer(r"^@@ -\d+(?:,\d+)? \+(\d+)(?:,(\d+))? @@", diff, re.M):
        start = int(header.group(1))
        count = int(header.group(2) or "1")
        changed.update(range(start, start + count))

issues = []
for number in sorted(hot_lines):
    if changed is not None and number not in changed:
        continue
    text = lines[number - 1]
    stripped = text.strip()
    if stripped.startswith(("//", "*", "/*")) or IGNORE.search(text):
        continue
    is_switch_arrow = re.match(r"(case|default)\b", stripped) is not None
    for regex, label in BANNED:
        if label == "lambda" and is_switch_arrow:
            continue
        if regex.search(text):
            if label == "alocacao com new" and re.search(r"new\s+(IllegalStateException|IllegalArgumentException|UnsupportedOperationException)", text):
                continue
            issues.append("  %s:%d  %s  ->  %s" % (path, number, label, stripped[:100]))
            break
if issues:
    block(
        "[hotpath_lint] Regra 11: o loop de 60 Hz nao aloca. Corrija ou, se for inevitavel e raro, marque a linha com '// hot-alloc-ok' e justifique.\n"
        + "\n".join(issues[:15])
        + "\nSolucao usual: campo reutilizavel, cache estatico, pool de objetos ou pre-calculo na carga."
    )
raise SystemExit(0)
