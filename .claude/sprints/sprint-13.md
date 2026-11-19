# Sprint 13 — Narrativa, Fluidez e Lapidação

**Período:** Pós-Sprint 12 (Release v20261119)
**Meta:** Polimento narrativo e fluência de jogo após unificação dos Enkis.

---

## Histórias

### APSU-N01 | Diálogos Enki polidos | 3 pts | Cawe [x]

**Contexto:** Cada variante de Enki (Sábio, Celestial, Eremita, Guia, Senhor) deve ter personalidade e referência mitológica distinta, refletindo o papel que desempenha em cada fase.

**Critérios de aceite:**
- [x] Cada uma das 5 falas tem tom único e identificável (Sábio: contemplativo; Celestial: majestoso; Eremita: solitário; Guia: didático; Senhor: autoritário)
- [x] Cada fala contém referências mitológicas sumérias canônicas (Eridu, Me sagrados, águas de Apsu)
- [x] Falas de introdução enriquecidas para guiar a progressão de fase
- [x] `make test` verde (198/198 testes)

**Tarefas:**
- [x] Pesquisar referências sumerianas por variante nos textos canônicos (Enuma Elish, hinos de Enki) (2 h)
- [x] Reescrever as 5 falas com tom e referência distintos em `GuardianEntity.java` (2 h)
- [x] Atualizar introduções de fase com call-to-action imersivo (1 h)
- [x] Validar com `make test` após alteração (0,5 h)

---

### APSU-N02 | Timing de cutscenes por fase | 3 pts | Rafa

**Contexto:** A duração atual das cutscenes é uniforme e não respeita a densidade narrativa de cada fase. A fase 5 (boss) merece introdução mais longa com música.

**Critérios de aceite:**
- Fase 1: cutscene de intro com duração de 4 s
- Fases 2-4: cutscene com duração de 3 s
- Fase 5 (boss): cutscene com duração de 5 s com trilha musical ativa
- Texto não é truncado em nenhuma resolução (1366x768 mínimo)
- Sem regressão de FPS durante a cutscene

**Tarefas:**
- [ ] Mapear os pontos de disparo de cutscene em `GameContext.java` por fase (1 h)
- [ ] Parametrizar duração por fase (constante ou enum) sem hardcode repetido (1 h)
- [ ] Acionar trilha musical específica na entrada da fase 5 (1 h)
- [ ] Testar em 1366x768 que nenhum texto é truncado (1 h)

---

### APSU-N03 | Alerta contextual por fase | 2 pts | Rafa [x]

**Contexto:** O texto de alerta exibido ao entrar em cada fase é genérico. Substituir por mensagem específica que contextualiza o objetivo da fase e reforça a narrativa.

**Critérios de aceite:**
- [x] Cada fase exibe mensagem de alerta única ao iniciar
- [x] Mensagens verificadas nos métodos `startP1` a `startP5` em `GameContext.java`
- [x] Tom coerente com a identidade da fase (P1 calmo/tutorial, P5 tenso contra Kullullû)

**Tarefas:**
- [x] Auditar métodos `startP1` a `startP5` em `GameContext.java` e localizar strings de alerta (0,5 h)
- [x] Redigir mensagem contextual para cada fase (P1 com Enki, P2 no Galeão, etc.) (1 h)
- [x] Substituir strings genéricas pelas contextuais (0,5 h)
- [x] Validar com `make test` (0,5 h)

---

### APSU-V01 | Animação de entrada dos Enkis | 5 pts | Wagner/Rafa

**Contexto:** Enki aparece de forma abrupta quando o herói se aproxima. Uma animação de fade-in com leve scale-up torna a aparição mais orgânica e reforça a presença divina.

**Critérios de aceite:**
- Enki aparece com fade-in de alpha 0.0 a 1.0 e scale de 0.8 a 1.0 em 400 ms
- Interpolação por `dt` usando campo `alphaEnki` existente ou novo campo dedicado
- Zero alocação no loop quente: sem `new`, lambdas ou boxing na lógica de interpolação
- Transição imperceptível em hardware de referência (notebook 4 threads, GPU integrada)

**Tarefas:**
- [ ] Adicionar campo `alphaEnki` (double) em `GuardianEntity` ou equivalente (0,5 h)
- [ ] Implementar interpolação linear de alpha e scale por `dt` em `update` (2 h)
- [ ] Aplicar alpha e scale no draw de `RenderEngine` ou `Runtime3DLayer` (2 h)
- [ ] Verificar ausência de alocação com `hotpath_lint.py` (0,5 h)

---

### APSU-V02 | Efeito de brilho na tabuleta | 3 pts | Marcos

**Contexto:** A tabuleta 3D deve emitir aura luminosa pulsante quando ainda não coletada, reforçando que é um objeto interativo.

**Critérios de aceite:**
- Tabuleta emite glow pulsante visível sobre o fundo de qualquer fase
- Glow implementado como pré-cálculo em cache (array de valores de raio/alpha por fase do ciclo) sem `Color.web` por frame
- Zero alocação no loop: sem `new Color`, `deriveColor` ou gradiente criado por frame
- Glow some ao coletar a tabuleta

**Tarefas:**
- [ ] Pré-calcular tabela de glow (256 passos de alpha) na carga da fase (1 h)
- [ ] Aplicar glow na renderização da tabuleta em `Runtime3DLayer` ou `SceneryElement` (1,5 h)
- [ ] Desativar glow ao marcar tabuleta como coletada (0,5 h)
- [ ] Verificar ausência de alocação com `hotpath_lint.py` (0,5 h)

---

### APSU-P01 | Benchmarks de desempenho com novos assets | 3 pts | Marcos

**Contexto:** Os sprites 3D adicionados no Sprint 12 (tabuleta, corais, bolhas) precisam ser validados contra o orçamento de performance do projeto.

**Critérios de aceite:**
- Benchmark executado por 60 s em P1, P2 e P5 com `APSU_PERF=1 make run`
- Resultado aprovado em `python3 .claude/tools/perf_gate.py <log>`
- Resultado salvo em `.claude/perf/baseline-v20261119.txt`
- Nenhuma métrica acima do limiar de falha (ver tabela em `CLAUDE.md`)

**Tarefas:**
- [ ] Executar benchmark de 60 s em P1 com `APSU_PERF=1 make run` e salvar log (0,5 h)
- [ ] Executar benchmark de 60 s em P2 e salvar log (0,5 h)
- [ ] Executar benchmark de 60 s em P5 e salvar log (0,5 h)
- [ ] Rodar `perf_gate.py` em cada log e consolidar em `.claude/perf/baseline-v20261119.txt` (0,5 h)
