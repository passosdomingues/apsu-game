# 📝 CHANGELOG — As Águas de Apsu

> Formato baseado em [Keep a Changelog](https://keepachangelog.com/pt-BR/1.0.0/)
> Versionamento seguindo [Semantic Versioning](https://semver.org/lang/pt-BR/) — `MAJOR.MINOR.PATCH`
>
> - **MAJOR**: mudança que quebra compatibilidade (ex: novo engine, refactor completo)
> - **MINOR**: nova feature ou fase adicionada
> - **PATCH**: bugfix, ajuste de balance, correção visual

---

## [Unreleased] — Sprint 12 Concluída (2026-08-21)

### Visual, UI & Limpeza
- **Eliminação Total de Emojis**: Removidos todos os caracteres emoji e Unicode especiais de `GameContext`, `UIRenderer`, `RenderEngine`, `Difficulty` e `Makefile`.
- **HUD Renovado**: HP renderizado como barra de vida segmentada colorida por limiar, tabuletas representadas como blocos retangulares dourados desenhados via Canvas, sem dependência de fontes/emojis do sistema.
- **Menu e Telas Limpas**: Substituídos ícones de emoji por formatação de texto limpa.

### Rendering & Estabilidade
- **Correção de Artefato Escuro (Quadrado Preto)**: Removido `BlendMode.ADD` de `LightingEngine.java` que causava renderização de caixas escuras/flicker em drivers Mesa/SW-GL. Halos bioluminescentes agora usam dois ovais concêntricos com transparência alpha simples, eliminando a alocação de objetos por frame.
- **Eliminação do Overlay de Coral 3D**: Removido o overlay semi-transparente de `coral3D` em `RenderEngine.java` que projetava retângulos escuros sobre o herói.
- **Otimização de Performance**: Desempenho de renderização procedural otimizado nas fases 2-4 (redução de contadores de partículas, filamentos, faíscas vulcânicas e vapor em ~35%), mantendo a estética e acabamento visual sem engasgos.
- **Limiar Adaptativo de FPS**: Ajustado o gatilho de modo reduzido para 30 FPS, prevenindo falso-positivos durante o warm-up inicial da JVM.

### Controles & Física
- **Física de Nado Responsiva**: Frenagem em inversão de direção aumentada de `0.70` para `0.58` em X e `0.62` em Y. Desaceleração idle ajustada de `0.92` para `0.86` em X e `0.94` para `0.88` em Y. O herói para e faz curvas com maior precisão.
- **Empuxo / Flutuação Neutra**: Buoyancy recalculada em `HeroType` (0.04 a 0.09), reduzindo a tendência de afundar rapidamente ao parar de nadar.
- **Hitboxes Precisas**: Núcleo do herói ampliado de `24x80` para `28x100` para corresponder ao corpo real do sprite; sobreposição exigida nos inimigos ajustada para `55%` central, eliminando acertos "a distância".

---

## [0.4.0] — 2026-08-21 — Sprint 11 Concluída

### Redesign & Gameplay
- **Fase 2 — Cavernas de Coral**: Reduzido de 9 para 6 colunas de coral com padrão em S suave e previsível. Inimigos reposicionados exclusivamente nas salas abertas. Baú do galeão em área de fácil acesso (`wx=1800`).
- **Fase 3 — Correntes Abissais**: Reduzido de 6 para 4 inimigos. Correntes organizadas em 3 zonas sequenciais (~1000px de separação cada) em vez de sobrepostas. Reduzido para 2 obstáculos móveis bem espaçados.
- **Fase 4 — Abismo Vulcânico**: Reduzido de 7 para 5 inimigos. Leviatã isolado como sub-boss com 600px de espaço exclusivo. 2 piscinas de lava maiores com coluna de vapor.
- Hitbox do herói compactada para `24x80` focada no peito, garantindo esquivas ágeis.

### Visual & Rendering
- **Corais Procedurais Orgânicos (`drawOrganicCoral`)**: Corais desenhados com gradiente de profundidade, borda bioluminescente e ramos articulados procedurais que balançam com a água. Bioluminescência rotativa por coluna.
- **Correntes com Filamentos (`drawCurrentZone`)**: Substituídas as marcas pontuais por filamentos líquidos em movimento.
- **Gêiser Renovado (`drawGeyser`)**: Coluna vertical sólida translúcida + base em cápsula indicando o raio de impulso.
- **Fundo Marinho Decorativo**: Fase 2 enriquecida com recifes de fundo e criaturas marinhas em paralaxe.
- **Vulcão Atmosférico**: Faixa de lava pulsante no rodapé, reflexos de superfície, vapor ascendente e faíscas incandescentes (*embers*) emergindo das rochas.

### Pipeline & Tooling
- Integração do Blender via `/opt/blender-4.5.5-lts/blender`.
- Execução do orquestrador mestre `gerador_mestre_apsu.py` e gerador de sprites 2.5D.
- Suíte de testes automatizados estendida para **37/37 testes** passando com 100% de sucesso.

---

## [0.3.0] — 2026-08-20 — Sprint 10 Concluída

### Corrigido
- Progressão das fases: herói, câmera, tabuletas, projéteis e colisões das fases 1–2 agora usam coordenadas de mundo consistentes; o portal final fica alcançável.
- Inimigos podem ser derrotados pelo feixe e somem ao contato, removendo a fonte de dano por frame; invencibilidade após dano aumentada para 2,4 s.
- Balanceamento Sábio/Ira: menos inimigos, oscilações e velocidades menores, além de atraso no primeiro ataque do boss.

### Adicionado
- Gerador Blender de 12 poses de ataque exclusivas para as variantes Abissal, Deus Dourado e Recife; o runtime as carrega automaticamente assim que `make assets` gerar os PNGs.
- Atalho seletivo `make assets-variant-attacks`, que evita re-renderizar todos os assets ao atualizar somente as poses de ataque das variantes.
- Viewport responsivo: o canvas lógico é escalado proporcionalmente e centralizado, preservando toda a composição em monitores de proporções diferentes.
- Dificuldade adaptativa por fase, baseada em danos recebidos e inimigos derrotados, mantendo a dificuldade escolhida no menu como base.
- Testes de viewport e DDA; a suíte passa a ter 34 testes.
- Kullullû agora luta em três estágios: leque de obsidiana, onda de choque vulcânica e meteoros de lava. Cada transição tem telegrafo visual de 0,5 s, alerta, partículas e evento desacoplado `BOSS_PHASE_CHANGED`.
- Teste unitário dos limiares de estágio e do telegrafo do boss; suíte passa a ter 29 testes.
- Feedback tátil de impacto: tremor de câmera determinístico para dano, perigos ambientais e derrota do boss, sem alterar a física ou as coordenadas de mundo.
- Testes de regressão de câmera, carregamento de sprite, fallback, persistência, eventos e radar; suíte passa a ter 28 testes.
- Cache de sequências de sprite e testes de carregamento/fallback, reduzindo alocações no caminho de renderização.
- Cache nativo JavaFX dos testes isolado em `target/`, tornando a suíte portátil para ambientes com diretório pessoal somente leitura.
- Modo silencioso configurável para testes, evitando dependência de dispositivo de áudio sem alterar o comportamento do jogo.
- Workflow GitHub Actions para validar testes/pacote Java e compilação do demonstrador OpenMPI em push e pull request.
- `SaveManager` JSON versionado, resiliente a save inexistente ou corrompido, com suporte a injeção de caminho para testes.
- Preferências e progresso agora são restaurados e persistidos pelo `GameContext`, sem I/O obrigatório durante testes.
- Barramento de eventos desacopla a lógica de jogo do áudio JavaFX; o adaptador de áudio é conectado apenas no bootstrap da aplicação.
- Radar/minimapa no HUD para localizar herói, inimigos, guardiões e boss em todas as fases.
- Pipeline local `make character-reference` / `make character-3d`: referência substituível e geração Hunyuan3D por API com cache SHA-256, sem sobrescrever modelos anteriores.
- Trilha ambiente e efeitos de tiro, coleta, dano, boss e vitória em `resources/audio/`; todos podem ser substituídos mantendo o nome do arquivo.
- Ponte Blender → PNG: `make blender-sprites` exporta Actions e direções para assets de runtime; fundos e primeiro plano aceitam substituição por fase.
- FXGL 21.1 + `ApsuFXGLGame`, um laboratório compilado para migração gradual de entidades e cenas.

### Modificado
- Variantes Abissal, Deus e Recife preservam seus próprios sprites ao atacar e passam a priorizar suas novas poses Blender, mantendo o ciclo da própria skin como fallback seguro antes do render.
- Fase 2 virou um corredor labiríntico com aberturas alternadas; dificuldade adaptativa reage ao desempenho sem mudar o layout durante uma tentativa.
- Exportação Blender agora assume o layout real `assets/characters/<asset>.blend` e usa todos os CPUs lógicos por padrão; `BLENDER_THREADS` permite limitar esse uso.
- O exportador usa perfil responsivo de render software (96 px, 1 amostra, uma direção) e a pose estática de Adapa passou a ser carregada pelo jogo como fallback antes do sprite legado.

### Pendente para próxima versão
- Validação de sprites in-game (proporção, transparência) — APSU-040
- Parallax de 2 camadas — APSU-041
- Benchmark de FPS — APSU-043
- Fix: JAVA_HOME apontando para JDK 21 Temurin para `make run` funcionar
- `git init && git commit` — inicializar repositório

---

## [0.2.0] — 2026-08-07 — Sprints 1, 2 e 3 Concluídas

> **Sprint 1: Core Engine & Menu**  
> **Sprint 2: Game Phases & HUD**  
> **Sprint 3: Build System & DevOps**  
> Implementação completa do jogo em uma única sessão intensiva.
> Compilação validada: `javac` + JDK 21 Temurin + JavaFX `/opt/javafx-21/lib` → **0 erros**.

### Adicionado
- `pom.xml` — Maven + JavaFX 21.0.3 linux, javafx-maven-plugin 0.0.8, assembly fat JAR (APSU-010)
- `src/main/java/ApsuGame.java` — jogo completo monolítico ~600 linhas (APSU-011–028):
  - Input `Set<KeyCode>` + eventos pontuais `onKey()`
  - Menu oceânico animado (bolhas, gradiente, seleção ↑↓)
  - Diálogo Enki 7 linhas + avatar PNG/fallback
  - Câmera `camX` deadzone 35%, herói WASD flip horizontal
  - **Fase 1** — Águas Claras: 4 inimigos senoidal, Tabuleta, portal
  - **Fase 2** — Cavernas Coral: obstulos sólidos, Easter Egg baú, Tabuleta
  - **Fase 3** — Boss Kullullû: HP bar 5 hits, bolhas leque, feixes guiados, Tabuleta
  - HUD: corações, tabuletas 0/3→3/3, fase, dificuldade
  - Partículas: 14/evento (dourado/vermelho/ciano), fade
  - Vitória (orbital dourado) + Game Over (vermelho, restart R)
  - SpriteManager fallback geométrico — zero NPE
- `src/main/resources/` — 6 sprites: hero(1.3MB) boss(565KB) npc(744KB) bg1(978KB) bg2(895KB) bg3(957KB) (APSU-016)
- `Makefile` — build/run/run-local/package/docker-build/docker-run/mpi-demo/clean/help, JAVAFX_PATH configurável (APSU-030/032)
- `Dockerfile` — multi-stage Maven builder + Liberica JDK 21 Full + X11 libs (APSU-031)
- `mpi/MapGenerator.c` — MPI_Scatter/Gather 8 processos, mapa colorido ANSI 3 fases (APSU-033)
- `.gitignore` — Java/Maven/JavaFX/Docker/MPI/IDEs/OS (APSU-017)
- `planning/ARCHITECTURE.md` — ADR-008: FXGL avaliado e rejeitado para v1.0

### Corrigido
- BLOCK-001: `pom.xml` não existia → criado
- BLOCK-002: sprites fora de `src/main/resources/` → copiados

### Notas
- **Build Maven bloqueado:** Maven usa JDK 17 (JAVA_HOME). Fix: `export JAVA_HOME=/opt/java-temurin-21`
- **Build direto OK:** `javac` JDK 21 Temurin + JAVAFX_PATH → 4 .class gerados sem erro
- **FXGL avaliado:** Rejeitado (ADR-008) — migrar seria reescrever o projeto

---

## [0.1.0] — 2026-08-07 — Sprint 0 Concluída

> **Sprint 0: Kickoff & Game Plan**
> Primeira versão registrada. Apenas assets e planejamento — sem código executável ainda.

### Adicionado
- `GamePlan.md` — documento de design completo com mecânicas, fases e requisitos técnicos
- `Apkallu.png` — sprite base do herói Adapa fornecido pelo designer
- Sprites gerados por IA com estilo coerente ao Apkallu:
  - `boss_kullullu_*.png` — Boss: Kullullû corrompido (visual sombrio, tons roxo/preto)
  - `npc_enki_*.png` — NPC: Enki deus das águas (tons dourado/azul, cetro)
  - `bg1_aguas_claras_*.png` — Background Fase 1: águas azuis claras tropicais
  - `bg2_cavernas_coral_*.png` — Background Fase 2: cavernas escuras com corais
  - `bg3_templo_submerso_*.png` — Background Fase 3: templo mesopotâmico submerso
- `implementation_plan.md` — plano técnico de implementação aprovado pelo Tech Lead

### Decisões Técnicas
- Stack definida: JavaFX 21 + Maven 3.9 + Docker (Liberica) + GNU Make + OpenMPI
- Resolução alvo: 1366×768 fullscreen adaptativo
- Dificuldades: Sábio (5❤, inimigos lentos) e Ira de Enki (3❤, inimigos rápidos)
- Colisão: AABB simples (suficiente para side-scroller)
- Sprite fallback: formas geométricas quando PNG não encontrado (resiliência)

---

## [0.0.1] — 2026-07-28 — Projeto Iniciado

### Adicionado
- Repositório criado em `/home/rafael/github/cache/grafica/Game/`
- Reunião inicial de kickoff: tema mesopotâmico aprovado
- Personagem principal definido: Adapa, o Apkallu (homem-peixe sábio)

---

## Convenção de Tipos de Mudança

| Tag | Descrição |
|---|---|
| `Adicionado` | Nova funcionalidade ou arquivo |
| `Modificado` | Mudança em funcionalidade existente |
| `Depreciado` | Funcionalidade que será removida em breve |
| `Removido` | Funcionalidade removida |
| `Corrigido` | Correção de bug |
| `Segurança` | Correção de vulnerabilidade |
| `Balance` | Ajuste em valores de gameplay (HP, velocidade, etc.) |
| `Visual` | Mudança cosmética sem impacto em gameplay |

---

## Template para Nova Entrada

```markdown
## [X.Y.Z] — YYYY-MM-DD — Título da Release

> **Sprint N: Nome da Sprint**
> Breve descrição do que foi entregue.

### Adicionado
- Item adicionado (APSU-XXX)

### Modificado
- Item modificado (APSU-XXX)

### Corrigido
- Bug corrigido — descrição do problema e solução (APSU-XXX)

### Balance
- Velocidade dos inimigos ajustada: 2.2 → 2.5 (Modo Sábio)

### Notas para Próxima Versão
- Lista de débitos técnicos identificados
```
