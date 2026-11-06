#!/usr/bin/env python3
"""PreCompact: guarda um resumo curto para SessionStart(compact) reinjetar."""
from _common import git, read_state, write_state

story = read_state("active-story") or "nenhuma"
stat = git("diff", "--stat", "HEAD") or "sem alteracoes"
branch = git("rev-parse", "--abbrev-ref", "HEAD") or "desconhecida"
write_state(
    "precompact.md",
    "Branch %s | story %s\nAlteracoes:\n%s\nGate de performance: %s"
    % (branch, story, stat[-1500:], read_state("last-perf-gate") or "nao executado"),
)
