#!/usr/bin/env python3
"""SessionStart: injeta o contexto minimo para retomar o trabalho."""
import os

from _common import PROJECT, git, read_payload, read_state

payload = read_payload()
lines = ["[apsu-harness] Contexto da sessao"]
branch = git("rev-parse", "--abbrev-ref", "HEAD") or "desconhecida"
lines.append("Branch: " + branch)
story = read_state("active-story")
lines.append("Story ativa: " + (story if story else "nenhuma (use /story-start <ID>)"))
gate = read_state("last-perf-gate")
if gate:
    lines.append("Ultimo perf gate: " + gate)
dirty = git("status", "--porcelain")
count = len([l for l in dirty.splitlines() if l.strip()]) if dirty else 0
lines.append("Arquivos modificados: %d" % count)
if payload.get("source") == "compact":
    snapshot = read_state("precompact.md")
    if snapshot:
        lines.append("Resumo salvo antes da compactacao:")
        lines.append(snapshot)
lines.append(
    "Prioridade: jogabilidade fluida. Leia .claude/docs/diagnosis.md antes de mexer em "
    "movimento, camera, render ou tiro. Sprints em .claude/sprints/."
)
print("\n".join(lines))
