#!/usr/bin/env python3
"""UserPromptSubmit: lembra a regra certa quando o pedido toca a area sensivel."""
import re

from _common import read_payload, read_state

payload = read_payload()
prompt = (payload.get("prompt") or "").lower()
notes = []

if re.search(r"trem|jitter|tremor|trepid|suavi|camera|câmera|seta|movimento|movimenta", prompt):
    notes.append(
        "Movimento e camera: ver causas C1 a C5 em .claude/docs/diagnosis.md; "
        "usar dt fixo, interpolacao no render e debounce de release no InputManager."
    )
if re.search(r"lento|lenta|fps|performance|desempenho|otimiz|travando|gargalo", prompt):
    notes.append(
        "Performance: medir antes e depois com APSU_PERF=1 e python3 .claude/tools/perf_gate.py; "
        "orcamento no CLAUDE.md."
    )
if re.search(r"tiro|shoot|bolha|projetil|projétil|aura|ataque|anima", prompt):
    notes.append(
        "Tiro, aura e ataque: ver causas C6 a C8 em .claude/docs/diagnosis.md e as stories "
        "APSU-G01 a APSU-G04."
    )
if re.search(r"blender|modelo|malha|obj|textura|bake|atlas", prompt):
    notes.append(
        "Modelos: limite de 6 materiais e 6.000 triangulos para o heroi; usar a skill blender-runtime-bake."
    )
story = read_state("active-story")
if story and re.search(r"pronto|terminei|conclu|done|finaliz", prompt):
    notes.append("Story ativa %s: rode /story-done %s para validar a Definicao de Pronto." % (story.split()[0], story.split()[0]))

if notes:
    print("[apsu-harness] " + " ".join(notes))
