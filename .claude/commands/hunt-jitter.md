---
description: Investiga tremor de movimento, câmera ou fundo com o subagente jitter-hunter
argument-hint: <sintoma observado>
allowed-tools: Task, Read, Grep, Glob, Bash(python3 .claude/tools/:*), Bash(make test)
---
Sintoma: $ARGUMENTS

Delegue ao subagente `jitter-hunter` com o sintoma e peça: causa em arquivo e linha, teste que reproduz, correção mínima. Antes, leia as causas C1 a C5b de `.claude/docs/diagnosis.md` para não reinvestigar o que já está mapeado. Devolva ao usuário o diagnóstico e a story correspondente.
