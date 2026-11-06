---
description: Compara um log de performance com o orçamento do projeto
argument-hint: <caminho do log>
allowed-tools: Bash(python3 .claude/tools/perf_gate.py:*), Read
---
Rode `python3 .claude/tools/perf_gate.py $ARGUMENTS --save` e mostre o veredito. Em caso de reprovação, explique cada falha em uma linha e indique a story do backlog que ataca a causa.
