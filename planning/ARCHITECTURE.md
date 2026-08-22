# 🏛️ ARCHITECTURE — Decisões de Arquitetura Técnica

> **ADR = Architecture Decision Record**
> Cada decisão importante é documentada aqui com contexto, opções consideradas e justificativa.
> Nunca reverta uma decisão sem criar um novo ADR explicando o motivo.

---

## Diagrama Geral da Arquitetura

```
┌─────────────────────────────────────────────────────────────┐
│                    ApsuGame.java (monolito)                  │
│                                                             │
│  ┌──────────┐  ┌──────────┐  ┌──────────┐  ┌───────────┐  │
│  │  INPUT   │  │  UPDATE  │  │  RENDER  │  │  STATE    │  │
│  │          │  │          │  │          │  │  MACHINE  │  │
│  │ KeyCode  │─▶│ Physics  │─▶│ Canvas   │  │           │  │
│  │ Set<Key> │  │ Collide  │  │ GC       │  │ MENU      │  │
│  │ onKey()  │  │ Entities │  │ drawImg  │  │ DIALOGUE  │  │
│  └──────────┘  └──────────┘  └──────────┘  │ PHASE1-3  │  │
│                                             │ VICTORY   │  │
│  ┌──────────────────────────┐               │ GAMEOVER  │  │
│  │  AnimationTimer (60 FPS) │               └───────────┘  │
│  │  handle(long now) ───────┼──▶ update() ──▶ render()     │
│  └──────────────────────────┘                               │
└─────────────────────────────────────────────────────────────┘
           │
           ▼
┌──────────────────────────────┐
│  JavaFX Runtime (JVM)        │
│  Canvas → OpenGL (Mesa/UHD)  │
│  AudioClip → ALSA/PulseAudio │
└──────────────────────────────┘
           │
           ▼
┌──────────────────────────────┐
│  Build System                │
│  Maven 3.9 ──▶ javafx:run   │
│  GNU Make  ──▶ run-local    │
│  Docker    ──▶ docker-run   │
└──────────────────────────────┘
```

---

## ADR-009 — FXGL como migração incremental para v2

**Data:** 2026-08-07 | **Status:** ✅ Aprovado

**Decisão:** manter `ApsuGame` (Canvas) como runtime de produção enquanto
`ApsuFXGLGame` estabelece a fundação FXGL para novas entidades, input, colisão,
câmera e cenas. Não haverá reescrita total antes de cada sistema ser reproduzido
e validado no laboratório.

**Compatibilidade:** o projeto fixa FXGL `21.1`: a release `25` é compilada
para Java 25, enquanto o runtime do jogo é Java 21 LTS.

**Motivo:** FXGL é uma engine Java/JavaFX para jogos 2D, com entidades,
animações e partículas; ela reduz a infraestrutura manual para a expansão do
projeto. A transição gradual preserva o jogo jogável e os assets existentes.

**Fronteira 2.5D:** Blender/Hunyuan produzem `.blend` e GLB de autoria. O
runtime recebe PNGs pré-renderizados e dados de colisão, não malhas 3D. Assim,
um personagem ou camada do cenário pode ser trocado isoladamente sem elevar o
requisito de GPU do jogador.

---

## ADR-001 — Monolito vs. Classes Separadas

**Data:** 2026-08-07 | **Status:** ✅ Aprovado

**Contexto:** O jogo pode ser organizado como um único arquivo Java ou em múltiplas classes/pacotes.

**Opções consideradas:**
1. Monolito (um único `ApsuGame.java`)
2. Pacotes separados (`engine/`, `entities/`, `screens/`)
3. ECS (Entity-Component-System) com classes genéricas

**Decisão:** Monolito — opção 1.

**Justificativa:**
- Escopo do jogo é pequeno (~1200 linhas de código)
- Eliminação de overhead de gerenciamento de múltiplos arquivos em projeto solo
- JavaFX Canvas não exige separação para performance
- Facilita a leitura sequencial por devs juniores
- Pode ser refatorado para pacotes na v2.0 sem impacto externo

**Consequências:** Arquivo único grande. Mitigação: seções bem comentadas com `// === SECTION ===`.

---

## ADR-002 — Canvas API vs. Scene Graph (JavaFX)

**Data:** 2026-08-07 | **Status:** ✅ Aprovado

**Contexto:** JavaFX oferece dois modos de rendering: Canvas (immediate mode) e Scene Graph (retained mode com Nodes).

**Opções:**
1. `Canvas` + `GraphicsContext` — desenho imperativo a cada frame
2. Scene Graph — Nodes, ImageViews, TranslateTransitions gerenciados pelo JavaFX
3. LibGDX — engine separado

**Decisão:** Canvas API — opção 1.

**Justificativa:**
- Canvas é superior para game loops com muitos sprites dinâmicos
- Scene Graph tem overhead de layout a cada frame para jogos de ação rápida
- `AnimationTimer` + Canvas é o padrão recomendado pela Oracle para jogos JavaFX
- LibGDX seria overkill para este escopo

**Para saber mais:** [JavaFX Canvas Tutorial](https://openjfx.io/javadoc/21/javafx.graphics/javafx/scene/canvas/Canvas.html)

---

## ADR-003 — Sistema de Coordenadas

**Data:** 2026-08-07 | **Status:** ✅ Aprovado

**Contexto:** Em jogos com scroll horizontal, objetos podem estar em "coordenadas de mundo" ou "coordenadas de tela".

**Decisão:** Coordenadas de **mundo** para entidades, convertidas para tela apenas na renderização.

**Regras:**
```
// CORRETO — armazenar em mundo, converter na renderização:
enemy.worldX = 2500.0;       // posição no mundo
screenX = enemy.worldX - camX;  // converter só no render()

// ERRADO — misturar os dois sistemas:
enemy.x = 2500 - camX;      // NÃO FAÇA ISSO
```

**Por que isso importa:**
- Colisão deve ser feita em coords de mundo (não de tela)
- Spawning de inimigos usa coords de mundo
- Apenas `gc.drawImage()` e similares recebem coords de tela

---

## ADR-004 — Timing do Game Loop

**Data:** 2026-08-07 | **Status:** ✅ Aprovado

**Decisão:** Usar o parâmetro `now` (nanosegundos) do `AnimationTimer.handle()` para todo timing.

**Por que NÃO usar `System.currentTimeMillis()`:**
```java
// ERRADO — pode ser afetado por ajuste de relógio do SO:
long ms = System.currentTimeMillis();

// CORRETO — monotônico, garantido pelo JVM:
long nano = now;  // parâmetro do AnimationTimer.handle(long now)
double seconds = nano / 1_000_000_000.0;
```

**Padrões de uso:**
```java
// Cooldown de disparo:
if (nano - lastShot > 400_000_000L) { ... lastShot = nano; }

// Animação senoidal independente de FPS:
double t = nano / 1_000_000_000.0;
y = baseY + Math.sin(t * 2.5) * amplitude;

// Invencibilidade temporária (1.5s):
if (nano - hitTime > 1_500_000_000L) inv = false;
```

---

## ADR-005 — Gestão de Listas de Entidades

**Data:** 2026-08-07 | **Status:** ✅ Aprovado

**Decisão:** Usar `ArrayList<double[]>` para entidades simples (inimigos, projéteis, partículas).

**Formato dos arrays:**
```
Inimigo:   double[] {worldX, screenY, speed, amplitude, baseY, type}
Projétil:  double[] {screenX, screenY, vx, vy}
Partícula: double[] {x, y, vx, vy, life, r, g, b}
Bolha:     double[] {screenX, screenY, vx, vy}
Coral:     double[] {worldX, y, width, height}
```

**Remoção segura durante iteração:**
```java
// CORRETO — Iterator para remover durante loop:
Iterator<double[]> it = beams.iterator();
while (it.hasNext()) {
    double[] b = it.next();
    if (outOfBounds(b)) it.remove();
}

// Alternativa limpa — removeIf:
particles.removeIf(p -> p[4] <= 0);  // p[4] = life

// ERRADO — ConcurrentModificationException:
for (double[] b : beams) {
    if (outOfBounds(b)) beams.remove(b);  // NUNCA FAÇA
}
```

**Por que `double[]` e não classe `Enemy`:**
- Projeto pequeno — overhead de classes é desnecessário
- Cache-friendly: arrays primitivos são mais eficientes na JVM
- Facilidade de adição de propriedades sem refatorar construtores

---

## ADR-006 — Docker + JavaFX com Display

**Data:** 2026-08-07 | **Status:** ✅ Aprovado

**Problema:** JavaFX precisa de um display X11 para renderizar. Containers Docker não têm display por padrão.

**Solução adotada:** X11 socket sharing (mais simples que VNC).

```bash
# No host (antes de docker run):
xhost +local:docker

# No docker run:
docker run -e DISPLAY=$DISPLAY -v /tmp/.X11-unix:/tmp/.X11-unix apsu-game
```

**Imagem base escolhida:** `bellsoft/liberica-openjdk-debian:21-full`
- Inclui JavaFX 21 nativamente (sem download extra)
- Inclui libs X11 necessárias
- Debian-based (fácil de adicionar dependências com apt)

**Alternativa considerada e rejeitada:** Xvfb virtual display — mais complexo e sem benefício para desenvolvimento local.

---

## ADR-007 — MPI para Geração de Mapa

**Data:** 2026-08-07 | **Status:** ✅ Aprovado

**Objetivo:** Demonstrar uso de paralelismo com os 8 cores do i7-8565U.

**Stack MPI:**
- Linguagem: C (mais natural para MPI)
- Implementação: OpenMPI 4.x
- Arquivo: `mpi/MapGenerator.c`
- Compilação: `mpicc -O2 -o map_gen mpi/MapGenerator.c`

**Padrão de uso:**
```
Processo 0 (master): Divide o mapa em 8 faixas horizontais
Processos 1-7 (workers): Cada um gera tiles para sua faixa
MPI_Scatter → distribui work
MPI_Gather  → coleta resultados
Processo 0: Escreve mapa completo em arquivo
```

**Integração com o jogo:** O `map_gen` é um utilitário independente — não roda durante o jogo. Gera arquivos de configuração de nível opcionalmente utilizados pelo jogo.

---

## Diagrama de Fluxo de Estados do Jogo

```
                    ┌──────────┐
          ┌────────▶│   MENU   │◀────────────────┐
          │         └────┬─────┘                 │
          │              │ ENTER (Iniciar)        │
          │              ▼                        │
          │         ┌──────────┐                 │
          │         │ DIALOGUE │                 │
          │         └────┬─────┘                 │
          │              │ (dlgLine >= 7)         │
          │              ▼                        │
          │         ┌──────────┐    tabs >= 1     │
          │         │  PHASE1  │─────────────▶──┐ │
          │ ESC     └──────────┘                │ │
          │                                     ▼ │
          │         ┌──────────┐    tabs >= 2   │ │
          ├────────▶│  PHASE2  │◀───────────────┘ │
          │         └──────────┘─────────────▶──┐ │
          │                        tabs >= 3     │ │
          │         ┌──────────┐                ▼ │
          ├────────▶│  PHASE3  │◀───────────────┘ │
          │         └────┬─────┘                  │
          │              │                        │
          │         ┌────┴────────────┐           │
          │         │                │           │
          │    bossHP == 0      heroHP == 0       │
          │         │                │           │
          │         ▼                ▼           │
          │    ┌─────────┐    ┌──────────┐       │
          └────│ VICTORY │    │ GAMEOVER │───────┘
               └─────────┘    └──────────┘
               ESPAÇO/ENTER   ESPAÇO/ENTER/R
```

---

## ADR-008 — FXGL vs. JavaFX Canvas Puro

**Data:** 2026-08-07 | **Status:** ✅ Aprovado — **Manter Canvas puro na v1.0**

**Contexto:** O usuário sugeriu o framework [FXGL](https://github.com/AlmasB/FXGL) (Java/JavaFX/Kotlin Game Library) como alternativa ao Canvas manual atual.

**O que o FXGL oferece de relevante:**
- Entity-Component System (ECS) nativo
- Sistema de partículas, câmera, colisão AABB built-in
- Animação de sprites, pathfinding (A*), física box2d
- `GameApplication` com lifecycle gerenciado
- Empacotamento trivial em fat JAR
- Java 8–25, Win/Mac/Linux, zero setup

**Análise comparativa com nosso estado atual:**

| Feature | FXGL | Nosso código | Ganho em migrar? |
|---|---|---|---|
| AnimationTimer / game loop | ✅ | ✅ Implementado | ❌ Nenhum |
| Colisão AABB | ✅ | ✅ `rectsHit()` | ❌ Nenhum |
| Câmera / scroll | ✅ | ✅ `camX` | ❌ Nenhum |
| Partículas | ✅ | ✅ Implementado | ❌ Nenhum |
| Carregamento de sprites | ✅ | ✅ `loadImg()` | ❌ Nenhum |
| Game states (menu/game) | ✅ | ✅ Enum `State` | ❌ Nenhum |
| ECS para entidades | ✅ | ⚠️ `double[]` | ✅ Sim (mas requer refactor total) |
| Física real (gravidade) | ✅ | ❌ Não precisa | ❌ Fora de escopo |
| Pathfinding A* | ✅ | ❌ Não precisa | ❌ Fora de escopo |
| Tilemap / Tiled editor | ✅ | ❌ Não precisa | ❌ Fora de escopo |

**Razões para NÃO migrar:**

1. **Já implementamos tudo o que o FXGL daria:** Migrar agora seria jogar fora trabalho feito sem ganho proporcional.

2. **Migração = projeto novo:** FXGL usa `GameApplication` (herança diferente), `Entity/Component`, `GameWorld` — incompatível com Canvas manual. Não é refactor de horas, é semanas.

3. **Dependência pesada sem necessidade:** Fat JAR do FXGL pesa ~25MB incluindo física box2d, localização, shaders — 90% não usaremos.

4. **Docker mais complexo:** FXGL tem seu próprio sistema de configuração de display que adiciona camadas ao container.

**Decisão:**
- **v1.0:** Manter `ApsuGame.java` com Canvas puro — código já escrito, funcional, simples de entender.
- **v2.0 (futuro):** Se o escopo crescer (física de plataforma, pathfinding de inimigos, > 5 fases), reavaliar migração para FXGL com dependency `com.github.almasb:fxgl:21.1`.

**Referências:**
- [github.com/AlmasB/FXGL](https://github.com/AlmasB/FXGL) — ~10k stars, ativo, bem documentado
- [FXGL Games de exemplo](https://github.com/AlmasB/FXGLGames)
- [YouTube: FXGL Tutorials](https://www.youtube.com/playlist?list=PL4h6ypqTi3RTiTuAQFKE6xwflnPKyFuPp)
