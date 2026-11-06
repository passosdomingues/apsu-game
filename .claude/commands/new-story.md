---
description: Cria uma nova story no formato validado, com pontuação Fibonacci
argument-hint: <sprint 07..10> <ID> <título>
allowed-tools: Read, Edit(.claude/sprints/**), Bash(python3 .claude/tools/sprint_report.py:*)
---
Argumentos: $ARGUMENTS

Adicione ao arquivo `.claude/sprints/sprint-<NN>.md` uma story no formato exato dos existentes:
cabeçalho `### APSU-<ID> | <título> | <N> pts | <responsável>`, frase "Como ..., quero ..., para ...", linha "Complexidade N: justificativa", ao menos 4 critérios `- ACn:` mensuráveis e ao menos 2 tarefas `- [ ] ID.n texto (h)`. Pontue só com 1, 2, 3, 5 ou 8 (acima disso, quebre). Atualize "Planejado" e a tabela de `backlog.md`. Rode `python3 .claude/tools/sprint_report.py` e corrija até não haver erros. Se a soma passar da capacidade, diga o que sai da sprint e proponha, sem decidir sozinho.
