---
description: Inicia uma story do backlog, cria a branch e carrega os critérios de aceite
argument-hint: <ID, por exemplo APSU-F01>
allowed-tools: Bash(python3 .claude/tools/sprint_report.py:*), Bash(git checkout -b:*), Bash(git switch -c:*), Bash(git status:*), Read, Grep, Glob
---
Inicie a story $ARGUMENTS.

1. Rode `python3 .claude/tools/sprint_report.py --story $ARGUMENTS` e mostre a story completa. Se não existir, pare e liste os IDs com `python3 .claude/tools/sprint_report.py`.
2. Confirme com `git status` que a árvore está limpa. Se não estiver, pare e informe.
3. Crie a branch: `feature/<id-minusculo>-<slug>` para features, `fix/` para tremor e colisão, `perf/` para render e malhas. Exemplo: `perf/apsu-r01-zero-alloc`.
4. Grave o ID e o título em `.claude/state/active-story` no formato `APSU-R01 Zero alocação no loop de render`.
5. Leia em `.claude/docs/diagnosis.md` a causa que a story corrige e abra os arquivos citados na evidência (AGENTS.md regra 2: auditar antes de editar).
6. Responda com: critérios de aceite, arquivos que serão tocados, testes que serão criados e o comando de medição. Não edite código ainda: espere confirmação do usuário.
