---
description: Guia a captura do baseline de performance e analisa os logs
argument-hint: [fase P1..P5 ou caminho de um log]
allowed-tools: Bash(make system-info), Bash(python3 .claude/tools/perf_gate.py:*), Read, Grep
---
Alvo: $ARGUMENTS

Se o argumento for um caminho de log existente, rode `python3 .claude/tools/perf_gate.py <log> --warmup 10` e interprete o resultado: gargalo em update ou render, judder (`sim/frame` com espalhamento), GC, carga de modelo. Aponte a causa provável usando `.claude/docs/diagnosis.md` e a story que corrige.

Caso contrário, mostre ao usuário o roteiro de captura (o Claude não consegue abrir a janela do jogo):
1. `make system-info` e anotar o renderer.
2. `APSU_AUDIO=0 APSU_PERF=1 make run 2>&1 | tee .claude/perf/baseline-<fase>.log`.
3. Jogar a fase por 70 s com movimento contínuo, tiro e passagem perto de inimigos.
4. Fechar o jogo e voltar com o caminho do log.
