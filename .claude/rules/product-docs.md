---
paths:
  - "docs/product/**"
  - "docs/qa/**"
---
# Regras para documentos de produto

- Backlog em docs/product/BACKLOG.md, formato validado por `python3 tools/harness/backlog.py lint`.
- Pontos somente em Fibonacci (1, 2, 3, 5, 8, 13). Acima de 13, dividir.
- Critérios de aceitação sempre em "Dado ..., quando ..., então ...", mensuráveis.
- Escrita modesta, sem emojis e sem travessões longos.
- Ao concluir uma story, atualize o status e marque apenas critérios realmente verificados.
