---
description: Implementa uma user story do backlog com testes, sem quebrar o harness existente
argument-hint: [ID da story, ex.: US-010]
allowed-tools: Read, Grep, Glob, Edit, Write, Bash(make test), Bash(make build), Bash(make coverage), Bash(git status:*), Bash(git diff:*), Bash(python3 tools/harness/backlog.py:*)
---
Implemente a story $ARGUMENTS.

Story:
!`python3 tools/harness/backlog.py show $ARGUMENTS`

Fluxo obrigatório:
1. Rode a auditoria prévia (`/apsu-audit $ARGUMENTS`) e confirme que a story não está bloqueada por dependência pendente (`python3 tools/harness/backlog.py next`).
2. Escreva primeiro os testes que provam cada critério automatizável. Critérios que dependem de playtest viram um roteiro em `docs/qa/` com passos e resultado esperado.
3. Implemente em classes novas, com integração mínima nos objetos centrais (o harness pedirá confirmação ao editar GameContext, RenderEngine ou GameLoop).
4. Rode `make test`. Se falhar, corrija a causa, sem desativar testes.
5. Marque `[x]` apenas os critérios realmente verificados. Para o que depende de ver o jogo, use `/apsu-aiplay` e confira as capturas. O que depende de áudio, sensação de jogo ou GPU real fica `[ ]` e é informado ao humano. Nunca use `make run`.
6. Atualize README, ARCHITECTURE ou DEVELOPMENT se interfaces ou fluxos mudaram (regra 09).
7. Proponha a mensagem de commit em Conventional Commits com `Refs: $ARGUMENTS`. Não faça commit sem pedido explícito.

Se um critério conflitar com AGENTS.md, pare e pergunte (regra 13).
