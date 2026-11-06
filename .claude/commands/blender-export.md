---
description: Plano e comandos para regenerar e exportar um modelo do Blender para o runtime
argument-hint: <nome do modelo, por exemplo 01_adapa_heroi>
allowed-tools: Read, Grep, Glob, Task
---
Modelo: $ARGUMENTS

Delegue ao subagente `blender-technical-artist` para: localizar o script em `personagens/scripts/`, listar materiais e polígonos atuais, propor o atlas e a decimação para caber no orçamento (`CLAUDE.md`), e montar a sequência de comandos `make` (`make export-runtime-models`, `make refresh-model`). Os comandos `make` de assets pedem confirmação do usuário. Termine com a checagem `/mesh-budget`.
