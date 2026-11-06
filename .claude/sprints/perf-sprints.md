# Sprint perf: Eliminar gargalos de performance (prioridade)

Capacidade: 40 pontos. Planejado: 36.

Meta: eliminar os gargalos identificados no diagnóstico de lentidão sem regredir gameplay nem testes.

### APSU-R01 | Zero alocação no loop de render | 5 pts | Rafael
Como desenvolvedor, quero remover alocações de objetos do caminho quente do renderer, para reduzir a pressão no GC e estabilizar o frame time.

Complexidade 5: mudança cirúrgica em RenderEngine mas com alto impacto em GC e p95.

Critérios de aceite:
- AC1: Nenhuma chamada a `Font.font(...)` ocorre dentro de métodos chamados por frame em `RenderEngine` ou `UIRenderer`; todas viram constantes `static final Font`.
- AC2: `COLOR_CACHE` em `RenderEngine` é substituído por `HashMap` simples (sem overhead de lock do `ConcurrentHashMap`).
- AC3: As chaves de gradiente de menu e dialogo passam a ser constantes `static final LinearGradient`, sem concatenação de String por frame.
- AC4: `python3 .claude/hooks/hotpath_lint.py` não acusa nenhuma violação em `RenderEngine.java` nem em `UIRenderer.java`.
- AC5: `make test` verde. GC por minuto em jogo cai para no máximo 2 pausas medido com `perf_gate`.

Tarefas:
- [x] R01.1 Extrair todas as chamadas `Font.font(...)` de `RenderEngine` e `UIRenderer` para campos `static final` (2 h)
- [x] R01.2 Trocar `ConcurrentHashMap` por `HashMap` em `COLOR_CACHE` (0,5 h)
- [x] R01.3 Pré-alocar os 3 gradientes de fundo como constantes estáticas (0,5 h)
- [x] R01.4 Rodar `hotpath_lint.py` em ambos os arquivos e corrigir o que restar (1 h)
- [x] R01.5 Medir com `APSU_PERF=1` e registrar antes e depois em `.claude/perf/r01-after.log` (1 h)

### APSU-R02 | ParticleSystem sem alocação por frame | 3 pts | Rafael
Como desenvolvedor, quero um array fixo de partículas reutilizável, para eliminar o ciclo de alloc/dealloc do combate.

Complexidade 3: classe isolada, interface pública preservada.

Critérios de aceite:
- AC1: `List<Particle>` substituído por array `Particle[]` de tamanho `MAX_PARTICLES_HIGH` pré-alocado no construtor.
- AC2: Remoção usa swap-and-pop (sobrescreve o slot morto com o último elemento vivo), sem `ArrayList.remove`.
- AC3: `addBurst`, `addBubble` e `update` preservam o comportamento externo: mesmo budget, mesma expiração.
- AC4: Teste `ParticleSystemTest` expandido com: (a) pool não aloca novo array em steady-state; (b) burst acima do limite respeita o budget; (c) partículas expiram corretamente com array fixo.
- AC5: `make test` verde.

Tarefas:
- [x] R02.1 Substituir `ArrayList<Particle>` por `Particle[]` e contador `int alive` (1,5 h)
- [x] R02.2 Ajustar `update()` para compactação in-place com swap-and-pop (1 h)
- [x] R02.3 Expandir `ParticleSystemTest` (1 h)

### APSU-R03 | Cache de atores 3D entre fases | 8 pts | Rafael
Como desenvolvedor, quero que a troca de fase não reconstrua todos os MeshView no thread JavaFX, para eliminar o spike de entrada de fase.

Complexidade 8: toca o gerenciamento de ciclo de vida do scene graph 3D.

Critérios de aceite:
- AC1: Na transição de fase, `Runtime3DLayer` atualiza o tint do `PhongMaterial` existente via `setDiffuseColor` sem remover e recriar o `Group`.
- AC2: Um ator cujo modelo, largura e altura não mudam entre fases reutiliza o mesmo nó; apenas o material é atualizado.
- AC3: `requestModel` para modelos de P2 (naufrágio, bau) é chamado uma única vez na entrada de P2, não a cada frame.
- AC4: Teste headless em `Runtime3DLayerTest` verifica que o ator do herói não é recriado na transição P1 a P2.
- AC5: `make test` verde. Nenhuma mudança visual detectada em playtest manual das 5 fases.

Tarefas:
- [x] R03.1 Adicionar cache de `Actor` por chave `(modelName, targetW, targetH)` com tint como campo mutável (2 h)
- [x] R03.2 Substituir remove+recreate por `setDiffuseColor` quando só o tint muda (1,5 h)
- [x] R03.3 Mover `requestModel` de P2 para o método `startP2` do `GameContext` (1 h)
- [x] R03.4 Escrever teste headless de não-recreação (1,5 h)
- [x] R03.5 Medir spike de transição com `perf_gate` (1 h)

### APSU-R04 | QuadTree com pool de nos e cenário indexado | 8 pts | Rafael
Como desenvolvedor, quero reutilizar os nos da QuadTree e indexar elementos de cenário, para eliminar alocações por frame e iterações duplicadas.

Complexidade 8: afeta GameContext e QuadTree; colisões são gate de corretude.

Critérios de aceite:
- AC1: `QuadTree.clear()` reusa as instâncias de no filho (pool interno), sem alocar novo `QuadTree` a cada frame.
- AC2: Uma `QuadTree<SceneryElement>` é usada em `applyEnvironmentalForces` e `checkSceneryCollisions`, eliminando a iteração linear completa.
- AC3: O loop inline de `LAVA_POOL` em `updateP4` é removido; a detecção passa a vir de `checkSceneryCollisions`.
- AC4: O ternário identidade em `applyEnvironmentalForces` linha 813 é corrigido.
- AC5: `GameContextIntegrationTest` expandido com cenário de colisão de corrente e lava na P4.
- AC6: `QuadTreeTest` expandido: pool de nos (clear + reinsert nao aloca), recuperação correta de `SceneryElement`.
- AC7: `make test` verde. Colisões detectadas idênticas ao estado anterior.

Tarefas:
- [x] R04.1 Pool de nos em `QuadTree` (2 h)
- [x] R04.2 `QuadTree<SceneryElement>` em `GameContext` (1,5 h)
- [x] R04.3 Remover loop duplicado de `LAVA_POOL` em `updateP4` (0,5 h)
- [x] R04.4 Corrigir ternário identidade (0,5 h)
- [x] R04.5 Expandir `QuadTreeTest` e `GameContextIntegrationTest` (2 h)

### APSU-R05 | Cobertura minima e relatorio final | 3 pts | Rafael
Como desenvolvedor, quero cobertura de testes acima do gate e um relatorio de ganhos, para fechar o ciclo de performance com evidência.

Complexidade 3: só testes e documentacao, sem risco de regressão.

Critérios de aceite:
- AC1: `mvn verify` passa com gate JaCoCo de 50% linha e 30% branch em todos os pacotes.
- AC2: Classes `Camera`, `AdaptiveDifficulty` e `SceneryElement` atingem ao menos 50% de cobertura de linha.
- AC3: `docs/perf-sprint-report.md` preenchido com métricas `perf_gate` antes e depois de cada sprint R01 a R04.
- AC4: `ARCHITECTURE.md` atualizado com as mudanças de R02, R03 e R04.

Tarefas:
- [x] R05.1 Rodar `mvn verify` e analisar relatório JaCoCo (0,5 h)
- [x] R05.2 Escrever testes para `Camera`, `AdaptiveDifficulty` e `SceneryElement` até atingir o gate (2 h)
- [x] R05.3 Preencher `docs/perf-sprint-report.md` com tabelas antes/depois (1 h)
- [x] R05.4 Atualizar `ARCHITECTURE.md` (0,5 h)
