#!/usr/bin/env python3
"""PreToolUse(Edit|Write|MultiEdit): protege binarios, controla crescimento e estilo."""
import os
import re

from _common import PROJECT, block, read_payload, rel

payload = read_payload()
tool = payload.get("tool_name", "")
data = payload.get("tool_input") or {}
path = rel(data.get("file_path", ""))
if not path:
    raise SystemExit(0)

BINARY = (".png", ".jpg", ".jpeg", ".gif", ".wav", ".ogg", ".mp3", ".mp4", ".blend", ".blend1", ".xlsx", ".pdf", ".jar", ".class", ".zip", ".jfr")
if path.lower().endswith(BINARY):
    block("[guard_edit] %s e binario. Gere por make (render-sprites, export-runtime-models, assets) ou pelo Blender, nao por Edit/Write." % path)
if path.startswith(("target/", ".git/")):
    block("[guard_edit] %s e area gerada ou interna do git." % path)
if path.startswith("src/main/resources/audio/"):
    block("[guard_edit] Audio versionado e somente leitura neste harness.")

new_chunks = []
old_chunks = []
if tool == "Write":
    new_chunks.append(data.get("content", ""))
elif tool == "Edit":
    new_chunks.append(data.get("new_string", ""))
    old_chunks.append(data.get("old_string", ""))
elif tool == "MultiEdit":
    for item in data.get("edits", []) or []:
        new_chunks.append(item.get("new_string", ""))
        old_chunks.append(item.get("old_string", ""))
new_text = "\n".join(new_chunks)
old_text = "\n".join(old_chunks)

EMOJI = re.compile("[\U0001F300-\U0001FAFF\u2600-\u27BF\u2B50\u2B55\uFE0F]")
if EMOJI.search(new_text) and not EMOJI.search(old_text):
    block("[guard_edit] Emojis nao sao permitidos em nenhuma entrega deste projeto. Remova-os.")

if path.endswith((".md", ".txt")) and new_text.count("\u2014") > old_text.count("\u2014"):
    block("[guard_edit] Evite travessao longo em textos. Use virgula, dois pontos ou ponto.")

# Regra 10: nao engordar os God objects
LIMITS = {"src/main/java/br/apsu/core/GameContext.java": 40, "src/main/java/br/apsu/graphics/RenderEngine.java": 40}
if path in LIMITS:
    growth = new_text.count("\n") - old_text.count("\n")
    if tool == "Write":
        absolute = os.path.join(PROJECT, path)
        try:
            with open(absolute, encoding="utf-8") as handle:
                growth = new_text.count("\n") - handle.read().count("\n")
        except OSError:
            pass
    if growth > LIMITS[path]:
        block(
            "[guard_edit] %s ja e grande demais. Esta edicao adiciona %d linhas liquidas (limite %d). "
            "Extraia uma classe nova (AGENTS.md regra 10) e deixe aqui somente a chamada." % (path, growth, LIMITS[path])
        )

# Nao permitir marcadores de pendencia em arquivos do harness e do codigo
if re.search(r"\b(TODO|FIXME|TBD|A DEFINIR|PENDENTE)\b", new_text) and not re.search(r"\b(TODO|FIXME|TBD|A DEFINIR|PENDENTE)\b", old_text):
    if path.startswith((".claude/", "src/")):
        block("[guard_edit] Entregas sem pendencias: resolva a decisao agora e registre em .claude/docs/decisions.md em vez de deixar marcador.")
raise SystemExit(0)
