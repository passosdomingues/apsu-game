---
description: Verifica a Definição de Pronto contra as mudanças atuais, sem editar nada
argument-hint: [ID da story, opcional]
allowed-tools: Read, Grep, Glob, Bash(git diff:*), Bash(git status:*), Bash(make test), Bash(python3 tools/harness/backlog.py:*)
---
Verifique a Definição de Pronto (docs/product/DEFINITION_OF_DONE.md) para: $ARGUMENTS (se vazio, as mudanças atuais).

Rode `git status` e `git diff`, depois `make test` e `python3 tools/harness/backlog.py lint`. Para cada um dos 13 itens da lista "Por story", responda atende, não atende ou não verificável sem jogar, com a evidência (arquivo, teste ou saída de comando). Termine com a lista exata do que falta para a story poder ir a `done`. Não edite arquivos e não marque critérios.
