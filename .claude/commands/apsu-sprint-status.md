---
description: Mostra o andamento da sprint ativa e as próximas stories desbloqueadas
allowed-tools: Read, Bash(python3 tools/harness/backlog.py:*), Bash(git log:*), Bash(git status:*)
---
Relatório do backlog:
!`python3 tools/harness/backlog.py report`

Próximas stories da sprint ativa:
!`python3 tools/harness/backlog.py next`

Validação do backlog:
!`python3 tools/harness/backlog.py lint`

Resuma em até 15 linhas: pontos concluídos da sprint ativa contra a capacidade, stories bloqueadas e por quê, e a próxima story recomendada (maior prioridade sem bloqueio). Cruze com `git log --oneline -20` para citar commits que mencionam IDs de story (`Refs: US-xxx`). Não altere arquivos.
