# Sprint 8: Render sem gargalo (13/11 a 19/11)

Capacidade: 34 pontos. Planejado: 34.

Meta: o render cabe em 9 ms médios e 12 ms no pior caso no hardware de referência, sem trocar os modelos 3D por sprites.

### APSU-R01 | Zero alocação no loop de render | 5 pts | Marcos
Como jogador, quero menos pausas de coleta de lixo, para o jogo não engasgar.

Complexidade 5: muitos pontos pequenos em quatro arquivos.

Critérios de aceite:
- AC1: `python3 .claude/hooks/hotpath_lint.py` sem violações em `RenderEngine`, `UIRenderer`, `LightingEngine`, `ParticleSystem` e nos métodos quentes de `GameContext`.
- AC2: Fontes, cores e gradientes ficam em campos estáticos ou em cache criado na carga (`UiFonts`, `Palette`).
- AC3: Texto estático do HUD é desenhado de uma `Image` pré-renderizada quando o texto não muda entre frames.
- AC4: Dado 60 s de P3 depois do aquecimento, então no máximo 2 coletas de GC por minuto e nenhuma pausa acima de 10 ms.
- AC5: `make test` verde.

Tarefas:
- [ ] R01.1 Criar `UiFonts` e `Palette` e substituir `Font.font`, `Color.rgb`, `deriveColor` (3 h)
- [ ] R01.2 Cache de gradientes e brilho em `LightingEngine` (2 h)
- [ ] R01.3 Remover alocações em `ParticleSystem.update` (pool de partículas) (2 h)
- [ ] R01.4 Medir GC antes e depois com `APSU_PERF=1` (1 h)

### APSU-R02 | Camada de efeitos acima da cena 3D | 5 pts | Marcos
Como jogador, quero ver bolhas, impactos e partículas na frente dos modelos, para o tiro ser legível.

Complexidade 5: muda a pilha de nós em `ApsuGameMain` e o fluxo de `RenderEngine`.

Critérios de aceite:
- AC1: A pilha vira `Canvas` de fundo, `SubScene` 3D e `Canvas` de efeitos transparente por cima.
- AC2: Projéteis, partículas, flash e HUD são desenhados no Canvas de efeitos; fundo, parallax e aura no Canvas de fundo.
- AC3: Dado um projétil passando na frente de um inimigo 3D, então o projétil aparece inteiro sobre o inimigo (captura de tela anexada ao PR).
- AC4: O custo extra de um segundo Canvas transparente é menor que 0,5 ms por frame em `perf_gate`.
- AC5: Diálogos e menus continuam por cima de tudo.

Tarefas:
- [ ] R02.1 Segundo Canvas e escala idêntica ao primeiro em `ApsuGameMain` (1,5 h)
- [ ] R02.2 Separar `drawBackgroundLayer` e `drawEffectsLayer` em `RenderEngine` por classe nova `EffectsRenderer` (3 h)
- [ ] R02.3 Mover HUD e alertas para o Canvas de efeitos (1 h)
- [ ] R02.4 Captura de tela e medição (1 h)

### APSU-R03 | Orçamento de malha do herói | 8 pts | Cawe
Como jogador, quero o herói leve para renderizar, para sobrar frame para o resto.

Complexidade 8: refaz materiais e topologia no Blender e valida no runtime.

Critérios de aceite:
- AC1: O OBJ do herói exportado tem no máximo 6 materiais e no máximo 6.000 triângulos (hoje 42 e 18.312).
- AC2: Texturas unificadas em um atlas de no máximo 1024x1024 (diffuse) e, se necessário, um de emissão.
- AC3: Silhueta e cores aprovadas por Rafa lado a lado com o modelo atual (captura no PR).
- AC4: `Runtime3DLayer` instancia no máximo 6 `MeshView` para o herói.
- AC5: O ator de ataque atual segue disponível até a G07 substituí-lo por frames, mas fica fora do grafo de cena enquanto está oculto (ver R06).
- AC6: O custo de render do herói medido em `perf_gate` cai pelo menos 40% contra o baseline.

Tarefas:
- [ ] R03.1 Auditoria do `01_adapa_heroi.py`: materiais, polígonos, modificadores (2 h)
- [ ] R03.2 Atlas de texturas e unificação de materiais por bake (4 h)
- [ ] R03.3 Decimação controlada e normais (3 h)
- [ ] R03.4 Exportar com `make export-runtime-models` e validar no jogo (2 h)
- [ ] R03.5 Criar o alvo `mesh-budget` no Makefile chamando `python3 .claude/tools/mesh_budget.py --only hero` (2 h)

### APSU-R04 | Orçamento de malha de inimigos e boss | 5 pts | Wagner
Como jogador, quero inimigos e boss leves, para as fases cheias não derrubarem o FPS.

Complexidade 5: três famílias de modelos com orçamentos diferentes.

Critérios de aceite:
- AC1: Inimigo comum com no máximo 4.000 triângulos e 3 materiais; boss com no máximo 12.000 triângulos e 6 materiais.
- AC2: Variações do mesmo inimigo compartilham a malha e mudam só a tint (`ObjModelLoader.instance`).
- AC3: Aparência aprovada por Rafa em captura lado a lado.
- AC4: `python3 .claude/tools/mesh_budget.py --only enemy --only boss` sai com código 0.

Tarefas:
- [ ] R04.1 Auditar inimigos, variações e boss (1,5 h)
- [ ] R04.2 Unificar materiais e decimar (3 h)
- [ ] R04.3 Exportar e validar (1,5 h)

### APSU-R05 | Orçamento de malha do cenário | 5 pts | Carlos Eduardo
Como jogador, quero o cenário 3D leve, para corais, correntes e ruínas não pesarem.

Complexidade 5: muitas instâncias repetidas, como as correntes.

Critérios de aceite:
- AC1: Cada elemento de cenário em `models/scenery` com no máximo 2.500 triângulos e 2 materiais.
- AC2: A corrente abissal passa de 25.728 triângulos para no máximo 2.500, e os recifes de 82 materiais para no máximo 2.
- AC3: Elementos repetidos usam `ObjModelLoader.instance` com a malha compartilhada.
- AC4: `python3 .claude/tools/mesh_budget.py --only scenery` sai com código 0.

Tarefas:
- [ ] R05.1 Auditoria do cenário por triângulos e materiais (1,5 h)
- [ ] R05.2 Decimar corrente, recifes e ruínas (3 h)
- [ ] R05.3 Exportar e validar visualmente (1,5 h)

### APSU-R06 | Culling e atores duplicados | 3 pts | Marcos
Como jogador, quero que só o visível seja desenhado, para o custo acompanhar a tela.

Complexidade 3: lógica simples em `Runtime3DLayer`, com teste de visibilidade.

Critérios de aceite:
- AC1: Atores 3D fora de `[camX - 100, camX + viewport + 100]` ficam com `visible = false` e não são sincronizados.
- AC2: O ator `hero-attack` duplicado é removido da cena (o ataque usa frames, ver G07); até lá, só um dos dois fica visível e o outro é removido do grafo.
- AC3: Dado P3 com 30 inimigos no mundo, então o número de `MeshView` visíveis é no máximo o dos atores dentro da janela.
- AC4: Teste unitário de `isInView(worldX, camX)`.

Tarefas:
- [ ] R06.1 Função `isInView` e uso em `sync` (1,5 h)
- [ ] R06.2 Remover ator duplicado do herói do grafo quando oculto (1 h)
- [ ] R06.3 Teste (1 h)

### APSU-R07 | Pipeline JavaFX e JVM | 3 pts | Marcos
Como jogador, quero o jogo no pipeline acelerado, para a GPU fazer o trabalho de 3D.

Complexidade 3: configuração e medição, sem código de jogo.

Critérios de aceite:
- AC1: `make run` registra o pipeline ativo (es2, d3d ou sw) na primeira linha do log com `APSU_PERF=1`.
- AC2: Dado hardware sem es2 funcional, o log avisa que está em software e indica `APSU_GPU=intel` ou `amd`.
- AC3: Flags comparadas em A/B por `perf_gate` (`-Dprism.vsync=true`, `-Dprism.order=es2,sw`, `-XX:+UseG1GC` com `-XX:MaxGCPauseMillis=8`) e a melhor configuração fixada no `Makefile` com a justificativa em `docs/DEVELOPMENT.md`.
- AC4: O Dockerfile usa as mesmas flags da configuração escolhida.

Tarefas:
- [ ] R07.1 Log do pipeline no `GameLoop` ou `ApsuGameMain` (1 h)
- [ ] R07.2 Rodar A/B com 60 s por configuração (2 h)
- [ ] R07.3 Fixar flags no Makefile e no Dockerfile e documentar (1 h)
