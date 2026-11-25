# Sprint 13 — Narrativa, Fluidez, Imersão e Balanceamento

**Período:** Pós-Sprint 12 (Release v20261119)
**Capacidade Estimada:** 32 pontos
**Meta:** Lapidar integralmente a experiência de gameplay, ritmo narrativo, feedbacks audiovisuais e fluidez orgânica dos Enkis e cenários, conectando o fluxo da Sprint 12 às otimizações estruturais das Sprints 14 e 15 sob total conformidade com o harness (60 FPS, zero-alloc no loop quente).

---

## 1. Histórias Concluídas nesta Sprint

### [x] APSU-N01 | Diálogos Enki com Personalidade e Mitologia Suméria | 3 pts | Cawe
- **Contexto:** Cada variante de Enki (Sábio, Celestial, Eremita, Guia, Senhor) precisava de identidade própria e referência mitológica autêntica nos textos canônicos.
- **Entregue em:** commit `24e3ccc`
- **Critérios de aceite:**
  - [x] Falas distintas incorporando Eridu, as tabuletas dos Me e o destino de Apsu
  - [x] Introduções de fase enriquecidas com call-to-action em `GuardianEntity.java`
  - [x] 198/198 testes verdes

### [x] APSU-N03 | Alertas Contextuais por Fase | 2 pts | Rafa
- **Contexto:** Alertas genéricos de transição e tutorial foram substituídos por mensagens imersivas.
- **Entregue em:** commit `24e3ccc`
- **Critérios de aceite:**
  - [x] Alertas de `startP1` a `startP5` em `GameContext.java` contextualizados
  - [x] Salto de tutorial orienta diretamente para Enki

### [x] APSU-V02 | Efeito de Brilho Pulsante (Glow) na Tabuleta Sagrada 3D | 3 pts | Marcos
- **Contexto:** A tabuleta 3D requeria aura luminosa pulsante visível antes da coleta.
- **Entregue em:** commit `82495cd`
- **Critérios de aceite:**
  - [x] Tabelas estáticas `TABLET_GLOW_AURA` e `TABLET_GLOW_CORE` (64 passos) em `Palette.java`
  - [x] Zero alocação no loop quente via `LightingEngine.drawTabletGlow`
  - [x] Brilho cessa instantaneamente após a coleta

---

## 2. Histórias Prioritárias para Execução (Planejadas)

### APSU-N02 | Parametrização e Calibração de Cutscenes por Fase | 3 pts | Rafa [x]
- **Contexto:** A duração das cutscenes anteriormente era uniforme e continha números mágicos dispersos. Foi criada parametrização limpa por fase (`PhaseCutsceneConfig`) para acomodar a leitura das mensagens mitológicas sem quebrar o ritmo da descida.
- **Entregue em:** commit em andamento
- **Critérios de aceite:**
  - [x] AC1: Criação de `PhaseCutsceneConfig` parametrizando duração: P1 (intro Enki 4.0s), P2-P4 (3.0s), P5 Boss (5.0s com ativação de tema musical)
  - [x] AC2: `GuardianIntroductionController` e `PhaseExitIntroductionController` utilizam a configuração da fase ativa sem números mágicos
  - [x] AC3: Pulo suave via tecla (`Espaço`, `Enter`, `ESC`) respeita transição de retorno rápido (<= 0.3s)
  - [x] AC4: Sem alocação de objetos no loop de atualização
  - [x] AC5: `make test` verde (198/198 testes)
- **Tarefas:**
  - [x] Criar enum ou estrutura imutável de tempos por fase em `br.apsu.cutscene` (1 h)
  - [x] Integrar configuração nos controllers de cutscene e em `GameContext.advancePhaseFromDialogue` (1,5 h)
  - [x] Conectar gatilho musical dedicado na cutscene de P5 (1 h)
  - [x] Testes de unidade em `CutsceneControllerTest` e `make test` (1 h)

### APSU-V01 | Aparição Suave dos Enkis (Fade-in e Presença Divina) | 5 pts | Wagner / Rafa [x]
- **Contexto:** Quando o jogador se aproxima de um Enki no cenário das fases 1 a 5, o surgimento do modelo 3D/sprite agora é gradual e orgânico, evitando pop-in abrupto.
- **Entregue em:** commit em andamento
- **Critérios de aceite:**
  - [x] AC1: Campo `alphaEnki` interpolado linearmente por `dt` (0.0 a 1.0) com transição de 400 ms ao atingir a distância de aproximação (800 unidades de mundo)
  - [x] AC2: Leve scale-up suave de 0.85 a 1.0 sincronizado com o fade-in
  - [x] AC3: Zero alocação no loop quente (campos primitivos `double`, sem `new` ou lambdas no update/render)
  - [x] AC4: Transição aplicada de forma consistente em `Runtime3DLayer` e no fallback 2D de `RenderEngine`
  - [x] AC5: `make test` verde (198/198 testes)
- **Tarefas:**
  - [x] Adicionar controle de interpolação `alphaEnki` e `scaleEnki` em `GuardianEntity` atualizado via `dt` fixo (1,5 h)
  - [x] Aplicar transparência e escala no nó 3D do ator do guardião em `Runtime3DLayer` (2 h)
  - [x] Replicar atenuação no fallback de `RenderEngine` (1 h)
  - [x] Testes de unidade e validação de regressão (1 h)

### APSU-U01 | Elevação e Hierarquia Visual da Caixa de Diálogo | 3 pts | Rafa
- **Contexto:** A caixa de diálogo in-game é exibida muito próxima do rodapé inferior, sofrendo oclusão parcial por elementos de cenário do chão (corais, rochas, algas) e camadas de efeitos.
- **Critérios de aceite:**
  - AC1: Coordenada vertical da caixa de diálogo em `UIRenderer.drawDialogueOverlay` elevada para posicionamento seguro (`boxY = canvasH - boxH - 60`)
  - AC2: Borda e fundo com alto contraste glassmorphism utilizando `Palette.DIALOGUE_OVERLAY_BG` e `Palette.GOLD`
  - AC3: Diálogo permanece como elemento de camada superior sem sofrer sobreposição visual
  - AC4: `wrapText` revisado para evitar criação desnecessária de strings/arrays no loop
  - AC5: `make test` verde
- **Tarefas:**
  - [ ] Ajustar geometria e margem vertical em `UIRenderer.java` (1 h)
  - [ ] Otimizar renderização do texto e botões de avanço sem alocações temporárias (1,5 h)
  - [ ] Validar leitura visual em todas as fases P1 a P5 (0,5 h)

### APSU-U02 | HUD Superior Esquerda Minimalista e Feedback de Controles | 3 pts | Rafa
- **Contexto:** A área superior esquerda acumula muitas linhas de texto simultâneas (profundidade, barra de vida, modo de bolha, comandos e objetivo), poluindo a leitura do campo de visão subaquático.
- **Critérios de aceite:**
  - AC1: HUD compactada e reorganizada: barras de vida e tabuletas em primeiro plano; profundidade em badge discreto
  - AC2: Dica textual longa de teclas ("WASD / SETAS: NADAR...") substituída por dica contextual transitória recolhida após os primeiros segundos de jogo
  - AC3: Alinhamentos utilizam constantes de `Palette` e `UiFonts` sem cálculos dinâmicos repetidos
  - AC4: `make test` verde
- **Tarefas:**
  - [ ] Redesenhar distribuição de elementos no método `drawHUD` de `UIRenderer` (2 h)
  - [ ] Adicionar temporizador de fade-out suave para a dica de teclas após movimentação confirmada (1,5 h)
  - [ ] Validar proporção em 1366x768 (0,5 h)

### APSU-B01 | Balanceamento de Combate e Cadência de Ataque Inimigo | 3 pts | Marcos
- **Contexto:** Na dificuldade Média os inimigos comuns mantêm postura excessivamente passiva, enquanto na dificuldade Difícil os chefes e criaturas podem ser derrotados rápido demais pelo herói fortalecido.
- **Critérios de aceite:**
  - AC1: Inimigos comuns disparam/investem de forma cadenciada na dificuldade Média com cooldown de ataque balanceado (intervalo de 2.5s a 3.5s)
  - AC2: HP do boss final Kullullû e subchefe Leviatã Menor ajustado com aumento equilibrado (+25% a +35%) para garantir tensão nas 3 fases de combate
  - AC3: Dano base das bolhas do herói regulado para valor proporcional ao ritmo arcade sem frustração
  - AC4: Testes em `EnemyEntityTest`, `BossEntityTest` e `DifficultyTest` atualizados e verdes
- **Tarefas:**
  - [ ] Calibrar frequência de disparo dos inimigos em `GameContext.updateEnemies` para dificuldade Média (1,5 h)
  - [ ] Ajustar tabela de vida de boss/subchefes e tabelas de dano de projétil (1,5 h)
  - [ ] Atualizar assertions em testes de combate (1 h)

### APSU-A01 | Integração 3D da Bolha de Lava e Projéteis Inimigos | 3 pts | Wagner / Marcos
- **Contexto:** Os projéteis de inimigos e chaminés térmicas ainda utilizam primitivas simples, enquanto já existe o modelo fonte `tools/personagens/blends/12_elementos/bola_de_Lava.blend`.
- **Critérios de aceite:**
  - AC1: Malha 3D `bola_de_lava_3d` exportada e integrada ao carregador `ObjModelLoader`
  - AC2: Projéteis do tipo `ENEMY_MALIGN` e disparos vulcânicos na Fase 4 utilizam a malha 3D com rotação contínua e brilho ígneo na `Runtime3DLayer`
  - AC3: Fallback 2D mantido caso o modo 3D seja degradado por performance
  - AC4: Sem impacto perceptível de FPS em hardware de referência
- **Tarefas:**
  - [ ] Validar e exportar `bola_de_Lava.blend` para OBJ/MTL em `src/main/resources/models/scenery/` (1,5 h)
  - [ ] Integrar modelo no switch de projéteis da `Runtime3DLayer.java` (1,5 h)
  - [ ] Testar colisão e rotação em P4 com `APSU_PERF=1` (1 h)

### APSU-A02 | Composição Orgânica do Cenário e Depuração de Fundo | 3 pts | Wagner / Marcos
- **Contexto:** A distribuição de corais e obstáculos no leito de certas fases apresenta alinhamento linear/artificial, e elementos de fundo precisam de harmonia de profundidade.
- **Critérios de aceite:**
  - AC1: Corais, algas e obstáculos rochosos distribuídos com variação procedural de espaçamento e escala
  - AC2: Ausência de qualquer bloqueio na rota principal de nado do herói até os Enkis e portais
  - AC3: Verificação de contraste de fundo nas 5 fases com `contrast_check`
- **Tarefas:**
  - [ ] Revisar coordenadas de instanciação de `CORAL` e cenários em `GameContext.startP1` a `startP4` (1,5 h)
  - [ ] Aplicar variação orgânica de altura e distância sem colisão espúria (1,5 h)
  - [ ] Validar rota livre do herói e visibilidade dos Enkis (1 h)

### APSU-P01 | Benchmark Rigoroso de Baseline (Sprint Gate) | 2 pts | Marcos
- **Contexto:** Validação obrigatória pelo harness (`CLAUDE.md`) para consolidar a sprint e garantir conformidade com o orçamento de 60 FPS, p95 <= 18 ms e zero GC excessivo.
- **Critérios de aceite:**
  - AC1: Execução de 60 s de gameplay com `APSU_PERF=1` nas fases P1, P2, P4 e P5
  - AC2: `python3 .claude/tools/perf_gate.py` executado e aprovado em todas as amostras
  - AC3: Relatório consolidado em `.claude/perf/baseline-v20261125.txt`
  - AC4: Zero regressão nos 198 testes automatizados
- **Tarefas:**
  - [ ] Rodar sessões de teste com flag de performance nas fases-chave (1 h)
  - [ ] Executar `perf_gate.py` e arquivar métricas (0,5 h)
  - [ ] Validar suite de testes completa com `mvn test` (0,5 h)

---

## 3. Matriz de Relação entre Sprints

| História Sprint 13 | Interface com Sprint 12 (Refinamentos) | Interface com Sprint 14 (Desempenho Zero-Alloc) | Interface com Sprint 15 / Futura |
|---|---|---|---|
| **APSU-N02** (Cutscenes) | Complementa S12-04 (epílogo pós-Kullullû) | Elimina alocações no loop de cutscene | Estabelece base para vinhetas finais |
| **APSU-V01** (Fade Enki) | Alinha-se ao S12-C1 (5 variantes Enki) | Aplica alpha in-place sem recriar materiais 3D | Melhora impacto visual dos guardiões |
| **APSU-U01** (Diálogo) | Integra diálogos mitológicos de N01 | Utiliza cores estáticas de `Palette` (R01) | Prepara suporte para UI superior em telas cheias |
| **APSU-U02** (HUD) | Enxuga poluição visual | Utiliza `UiFonts` e `Palette` cacheados | Prepara tutorial visual intuitivo |
| **APSU-B01** (Combate) | Harmoniza com S12-01 (telegraph Kullullû) | Mantém atualização de IA em tempo constante | Ajusta curva de dificuldade global |
| **APSU-A01** (Lava 3D) | Substitui projétil genérico | Objeto 3D com malha otimizada (< 2500 tris) | Conecta-se à ambientação do abismo |
| **APSU-A02** (Cenário) | Orgânico com corais da S12-00 | Não adiciona overhead de colisão | Prepara profundidade de parallax |
| **APSU-P01** (Benchmark) | Valida assets integrados na S12 | Ponto de partida obrigatório para R01/R02 | Critério de aceite inegociável do harness |

---

## 4. Definição de Pronto (DoR) e Concluído (DoD)

- **DoR:** Toda história possui critérios de aceite verificáveis numericamente, responsável designado e ausência de bloqueios em sprints anteriores.
- **DoD:**
  1. `mvn test` com 100% de aprovação (mínimo 198 testes).
  2. Ausência de alocação no loop quente (`new`, boxing, `String.format` ou lambdas por tick).
  3. `perf_gate.py` aprovado (FPS médio >= 55, p95 <= 22 ms).
  4. Commits atômicos no padrão Conventional Commits em português.
  5. Nenhuma alteração de código é iniciada sem aprovação prévia do PO.
