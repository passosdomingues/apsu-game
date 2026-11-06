---
description: Revisão de jogabilidade de uma mudança com o subagente gameplay-feel-reviewer
argument-hint: [branch ou arquivos, padrão: diff atual]
allowed-tools: Task, Bash(git diff:*), Bash(git log:*), Read, Grep
---
Alvo: $ARGUMENTS (se vazio, `git diff HEAD`).

Chame o subagente `gameplay-feel-reviewer` com o diff e peça o veredito em quatro eixos: resposta (latência de entrada), suavidade (interpolação, dt, câmera), peso (tiro, impacto, ataque) e legibilidade (z-order, aura, contraste). Devolva bloqueios e sugestões separados.
