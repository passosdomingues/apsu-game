#!/usr/bin/env python3
"""Linha de status: branch, story ativa e ultimo perf gate."""
from _common import git, read_state

branch = git("rev-parse", "--abbrev-ref", "HEAD") or "-"
active = read_state("active-story")
story = active.split(" ")[0] if active else "sem story"
gate = read_state("last-perf-gate") or "gate nao rodou"
print("apsu | %s | %s | %s" % (branch, story, gate[:60]))
