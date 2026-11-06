---
description: Roteiro para confirmar o par release/press de teclas no X11 no seu ambiente
allowed-tools: Read, Grep, Bash(python3 .claude/tools/:*)
---
Explique ao usuário em poucos passos como confirmar a causa C1: rodar com `APSU_INPUT_TRACE=1` (criada na APSU-F00), segurar a seta direita por 3 s, e contar as linhas `RELEASE` seguidas de `PRESS` com menos de 5 ms de diferença. Se o trace ainda não existir, mostre o diff mínimo de `InputManager` que o adiciona e peça confirmação antes de editar. Leia `.claude/tools/input_probe_notes.md`.
