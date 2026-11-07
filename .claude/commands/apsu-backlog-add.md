---
description: Cria uma nova user story no backlog no formato validado pelo lint
argument-hint: [ideia ou feedback do jogador]
allowed-tools: Read, Edit, Bash(python3 tools/harness/backlog.py:*)
---
Transforme em user story: $ARGUMENTS

Use a skill apsu-user-story. Passos: ler o fim do BACKLOG.md para obter o próximo ID livre, escolher épico e sprint que ainda tenham capacidade (`python3 tools/harness/backlog.py report`), estimar em Fibonacci (dividir se passar de 13), escrever no mesmo formato das demais, inserir na seção da sprint e rodar `python3 tools/harness/backlog.py lint` até passar. Atualize a tabela de rastreabilidade se a origem for feedback de jogador.
