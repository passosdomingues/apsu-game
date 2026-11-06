---
description: Executa e compara metricas de performance antes e depois de uma mudanca
allowed-tools: Bash(make run:*), Bash(python3 .claude/tools/:*), Read
---
Compara metricas do APSU_PERF=1 entre o estado atual e o baseline registrado.
Nao modifica nenhum arquivo de codigo.

Para coletar metricas de performance do jogo:
1. APSU_PERF=1 make run
2. Jogue por ao menos 60 segundos na fase alvo da sprint.
3. Use python3 .claude/tools/perf_gate.py para validar contra o orcamento.
4. Registre em docs/perf-sprint-report.md.
