---
description: Verifica a Definição de Concluído da story ativa e marca as tarefas
argument-hint: <ID, por exemplo APSU-F01>
allowed-tools: Bash(make test), Bash(python3 .claude/tools/:*), Bash(python3 .claude/hooks/hotpath_lint.py), Bash(git status:*), Bash(git diff:*), Bash(git log:*), Read, Grep, Glob, Edit(.claude/sprints/**)
---
Valide a story $ARGUMENTS contra a Definição de Concluído de `.claude/sprints/backlog.md`.

1. Mostre a story com `python3 .claude/tools/sprint_report.py --story $ARGUMENTS`.
2. Para cada critério de aceite, diga se foi atendido, com a evidência (arquivo e linha, saída de teste ou número medido). Critério sem evidência é não atendido.
3. Rode `make test`. Se falhar, pare e mostre o erro.
4. Para cada arquivo Java alterado, rode o lint de hot path: `echo '{"tool_input":{"file_path":"<arquivo>"}}' | python3 .claude/hooks/hotpath_lint.py`.
5. Se a story mexe em movimento, render ou assets, peça ao usuário o log de `APSU_PERF=1 make run` de 60 s e rode `python3 .claude/tools/perf_gate.py <log> --save`.
6. Verifique `git status`: nada de `target/`, `.class`, `.jar`, `.log`, `.jfr` versionado.
7. Se tudo passou, troque `- [ ]` por `- [x]` nas tarefas da story no arquivo de sprint e limpe `.claude/state/active-story`. Se algo falhou, não marque nada e liste o que falta.
8. Sugira a mensagem de commit em Conventional Commits.
