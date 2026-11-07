---
name: apsu-user-story
description: Escreve ou revisa user stories de As Águas de Apsu com pontuação Fibonacci e critérios de aceitação Dado/quando/então. Use ao criar, dividir ou estimar stories.
---
# Escrever user stories do APSU

Formato exato de cada story em docs/product/BACKLOG.md:

```
### US-NNN | Título curto | N pts | P0 a P3 | Sn | En | todo
**Como** <papel>, **quero** <capacidade>, **para** <valor>.
- Fonte: <feedback, README ou decisão>.
- Depende de: nenhuma | US-xxx, US-yyy
- Notas técnicas: <hipóteses e pontos de extensão, a confirmar na auditoria>.

**Critérios de aceitação**
- [ ] Dado <contexto>, quando <ação>, então <resultado mensurável>.
```

Como estimar (Fibonacci):
- 1: mudança local, uma classe, sem risco.
- 2: pequena mudança com teste simples.
- 3: componente novo pequeno ou mudança em duas áreas.
- 5: componente novo com integração e testes.
- 8: infraestrutura reutilizável ou mudança que cruza várias fases.
- 13: só se não houver como dividir. Acima disso, divida a story.

Critérios bons: têm número (tempo, raio, contagem, limite), um único resultado verificável, e dizem como verificar (teste, medição, playtest). Evite "deve ser intuitivo" sem métrica; converta em tempo, contagem de acertos ou presença de um sinal visual.

Inclua um critério de desempenho quando a story roda no loop de 60 FPS e um critério de teste quando há regra de lógica. Não ultrapasse a capacidade da sprint (ver SPRINTS.md). Rode `python3 tools/harness/backlog.py lint` ao terminar.
