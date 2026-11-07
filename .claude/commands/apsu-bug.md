---
description: Corrige um bug começando por um teste que o reproduz
argument-hint: [descrição do bug, fase e passos]
allowed-tools: Read, Grep, Glob, Edit, Write, Bash(make test), Bash(make build), Bash(git status:*), Bash(git diff:*)
---
Bug: $ARGUMENTS

1. Audite a área afetada (somente leitura) e formule até três hipóteses ordenadas por probabilidade, com a evidência de cada uma.
2. Escreva um teste que falha pelo motivo certo. Se o bug é puramente visual e não testável sem display, escreva um teste sobre o dado que o causa (por exemplo, hitbox sem visual associado).
3. Corrija a causa raiz com a menor mudança possível (regras 01 e 05).
4. Rode `make test` e informe o antes e depois.
5. Registre o bug no BACKLOG.md como story ou nota quando ele revelar uma lacuna de critério.
6. Proponha a mensagem `fix(escopo): descrição`. Não faça commit sem pedido.
