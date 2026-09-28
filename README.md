# AS ÁGUAS DE APSU: A LENDA DOS APKALLU

<div align="center">

```text
+-----------------------------------------------------------------------------------+
|                                                                                   |
|   A S   A G U A S   D E   A P S U  ::  A   L E N D A   D O S   A P K A L L U      |
|                                                                                   |
|   [ 2.5D FREE SWIMMING ENGINE -- COMPUTATION GRAPHICS & FLUID MECHANICS 2026 ]    |
|                                                                                   |
+-----------------------------------------------------------------------------------+
```

<p align="center">
  <img src="https://img.shields.io/badge/Java-21_LTS-FF6600?style=for-the-badge&logo=openjdk&logoColor=white" alt="Java 21 LTS" />
  <img src="https://img.shields.io/badge/JavaFX-21.0.3-007ACC?style=for-the-badge&logo=openjfx&logoColor=white" alt="JavaFX 21" />
  <img src="https://img.shields.io/badge/Blender-4.5_LTS-E87D0D?style=for-the-badge&logo=blender&logoColor=white" alt="Blender 4.5 LTS" />
  <img src="https://img.shields.io/badge/Maven-3.9+-C71A36?style=for-the-badge&logo=apache-maven&logoColor=white" alt="Apache Maven" />
  <img src="https://img.shields.io/badge/Platform-Linux_%7C_Windows-009944?style=for-the-badge&logo=linux&logoColor=white" alt="Linux & Windows" />
  <img src="https://img.shields.io/badge/OpenMPI-Parallel_Map-00599C?style=for-the-badge&logo=c&logoColor=white" alt="OpenMPI" />
  <img src="https://img.shields.io/badge/JUnit_5-45%2F45_Passed-00AA00?style=for-the-badge&logo=junit5&logoColor=white" alt="JUnit 5 Passed" />
</p>

[Quick Start](#-quick-start--inicio-rapido) |
[Diagrama DOT de Classes](#-diagrama-de-arquitetura-de-classes-preto-no-branco) |
[Arquitetura Ponta a Ponta](#-comunicacao-entre-componentes-de-ponta-a-ponta) |
[Personagens 3D + cenário 2.5D](#pipeline-gráfico-personagens-3d-em-tempo-real--cenário-25d) |
[Conceitos de CG](#-mapeamento-de-conceitos-de-computacao-grafica) |
[Comandos Makefile](#-menu-de-comandos-arcade-makefile) |
[Troubleshooting](#-solucao-de-problemas-troubleshooting)

---

</div>

> [!IMPORTANT]
> **Propósito Acadêmico & Técnico:** Este projeto foi desenvolvido para demonstrar de ponta a ponta a aplicação prática dos fundamentos de **Computação Gráfica**, **Processamento de Imagens**, **Física de Mecânica dos Fluidos em Jogos** e **Arquitetura de Software Multiplataforma**. Ele conecta scripts procedurais 3D no Blender 4.5 LTS a um motor de renderização imperativo em JavaFX (Canvas 60 FPS), orquestrado de forma 100% idempotente por um Makefile compatível com **Linux (Debian/Ubuntu/Mint)** e **Windows**.

---

## SCREENSHOT SCHEMATIC (GAMEVIEW)

```text
+----------------------------------------------------------------------------------+
|                            AS ÁGUAS DE APSU (2.5D)                               |
|                                                                                  |
|   [HUD: Vida [========] 100% | Tabuletas: [3/5] | Fase: 3 - Correntes Abissais]   |
|                                                                                  |
|      ( ~ ~ ~ ~ Corrente Fluida ~ ~ ~ > )                                         |
|                                                                                  |
|            Swimmer          Bubble Shot                                          |
|           (Adapa) -----------> (*) (*)               (Enguia Abissal)            |
|          /~~~~~\                                      <><                        |
|          \_____/                                                                 |
|                                                                                  |
|   [Recifes & Corais 2.5D]       [Gêiser Hidrotermal]     [Estátua do Guardião]   |
+----------------------------------------------------------------------------------+
```

---

## QUICK START / INÍCIO RÁPIDO

O comando `make` verifica e instala apenas o necessário para executar o jogo (**Java 21 LTS e Maven**). Blender e OpenMPI são opcionais e só são verificados pelos alvos de geração de assets/mapas; os modelos 3D de runtime já acompanham o repositório.

### Option 1: Linux (Debian / Ubuntu / Linux Mint)
Abra o terminal na pasta do projeto e digite:
```bash
make
```
*O Makefile detecta seu sistema, configura o runtime e inicia o jogo. Para executar em container Linux com X11, use `make docker-run`.*

---

### Option 2: Windows (Windows 10 / Windows 11)

#### Via Makefile (no Git Bash ou WSL):
```bash
make
```

#### Via PowerShell Direto:
```powershell
# 1. Executa a verificação/instalação das dependências de runtime (Java 21 e Maven)
powershell -ExecutionPolicy Bypass -File tools/setup/setup_environment.ps1

# 2. Executa o jogo
mvn javafx:run
```

---

## DIAGRAMA DE ARQUITETURA DE CLASSES (PRETO NO BRANCO)

O diagrama de arquitetura mapeia **100% das classes do sistema** (`br.apsu.*`), agrupadas por subgrafos funcionais, em formato de alto contraste monocromático ("preto no branco").

### Arquivos do Diagrama:
* **DOT Source:** [`docs/diagrams/architecture_diagram.dot`](docs/diagrams/architecture_diagram.dot)
* **Vectorial SVG:** [`docs/diagrams/architecture_diagram.svg`](docs/diagrams/architecture_diagram.svg)
* **Imagem PNG:** [`docs/diagrams/architecture_diagram.png`](docs/diagrams/architecture_diagram.png)

```text
+---------------------------------------------------------------------------------------+
|                                    LAUNCHER (Fat JAR)                                 |
|                                            |                                          |
|                                            v                                          |
|                                    ApsuGameMain (Stage)                               |
|                                            |                                          |
|         +----------------------------------+----------------------------------+       |
|         |                                  |                                  |       |
|         v                                  v                                  v       |
|     EventBus                        GameContext                         RenderEngine  |
|         |                                  |                                  |       |
|         +------------+                     +-------------+                    |       |
|                      v                                   v                    v       |
|            AudioEventSubscriber                     GameLoop <----------> Viewport    |
|                      |                       (60 FPS Timer)                   |       |
|                      v                                                        |       |
|                 AudioManager <------------------------------------------------+       |
+---------------------------------------------------------------------------------------+
```

---

## COMUNICAÇÃO ENTRE COMPONENTES DE PONTA A PONTA

A arquitetura do jogo obedece a um fluxo desacoplado de alta performance:

### 1. Bootstrapping & Lifecycle
- `Launcher.java` -> Chama o `main()` sem requerer argumentos de módulos JavaFX no classpath.
- `ApsuGameMain.java` -> Inicializa o Canvas JavaFX, o `Viewport` responsivo, o `EventBus` e o `SaveManager`.
- Instancia o `GameContext` (mundo de jogo) e o `RenderEngine` (desenhista), conectando o `GameLoop` a 60 FPS.

### 2. Game Loop de 60 FPS (`GameLoop.java`)
A cada quadro do relógio monotônico:
1. `GameContext.update(delta)`:
   - O `InputManager` atualiza o estado das teclas.
   - O `HeroEntity` calcula as forças de arrasto quadrático da água e o empuxo de Arquimedes.
   - As entidades de inimigos (`EnemyEntity`), boss (`BossEntity`) e projéteis (`Projectile`) têm suas coordenadas atualizadas.
   - O spatial index `QuadTree` re-constrói os quadrantes AABB para colisão rápida $O(N \log N)$.
   - Se ocorrer um evento relevante, o `GameContext` dispara um `GameEvent` no `EventBus`.
2. `RenderEngine.render(gc, context)`:
   - A `Camera` calcula a projeção World -> Screen.
   - O `Runtime3DLayer` exibe personagens OBJ em 3D; o `SpriteManager` mantém os PNGs de cenário e interface.
   - O `LightingEngine` aplica iluminação emissiva de contorno (Fresnel Rim Light) e dispersão sob a superfície (SSS).
   - O `UIRenderer` desenha o HUD, radar e diálogos.

### 3. Sistema Pub/Sub de Eventos & Áudio
- `EventBus.java` canaliza os eventos em tempo real.
- `AudioEventSubscriber.java` escuta o barramento e solicita a execução de efeitos WAV ao `AudioManager.java` em thread separada.

---

## PIPELINE GRÁFICO: PERSONAGENS 3D EM TEMPO REAL + CENÁRIO 2.5D

Personagens e elementos de fase são exportados como malhas OBJ/MTL pelo Blender 4.5 LTS e renderizados em uma camada JavaFX 3D. Planos de fundo, paralaxe, partículas e HUD continuam no Canvas 2.5D.

```text
  [ Scripting Python (bpy) em personagens/scripts/ ]
   |-- _apsu_shared_lib.py (Paletas 60-30-10, Rim Fresnel, SSS)
   |-- 01_adapa_heroi.py (Curva procedural anguiliforme smoothstep)
   |-- 02_kullullu_boss.py (Obsidiana + Magma Voronoi)
   `-- 14_leviata_menor.py (Serpente vulcânica 8 segmentos)
             |
             v  (Execução via Blender CLI / Make)
  [ Blender 4.5 LTS Engine (Cycles / EEVEE) ]
   |-- Geração de Malhas Poligonais & Modificadores
   |-- Iluminação Três Pontos + Fresnel Rim Light
   `-- Exportação de malhas base e variantes de ataque
             |
             +--> [ OBJ/MTL em src/main/resources/models/characters/ e scenery/ ]
             |       v  (ObjModelLoader / Runtime3DLayer)
             |   [ Personagens e elementos de fase 3D na SubScene JavaFX ]
             |
             v  (tools/assets/render_all_2d5_sprites.py)
  [ PNGs 2.5D em src/main/resources/sprites/ para cenário/UI ]
             |
             v
  [ RenderEngine: planos 2.5D + malhas 3D de personagens e fase ]
```

---

## MAPEAMENTO DE CONCEITOS DE COMPUTAÇÃO GRÁFICA

| Unidade da Disciplina | Conceito Teórico | Aplicação Prática no Projeto | Classe / Artefato no Código |
|---|---|---|---|
| **I. Proc. Gráfico & Hardware** | Rasterização, GPU/CPU, VRAM | Renderização imperativa em Canvas JavaFX a 60 FPS; bake 3D->2.5D para VRAM. | [`RenderEngine.java`](src/main/java/br/apsu/graphics/RenderEngine.java), [`Makefile`](Makefile) |
| **II. Pipeline Gráfica & APIs** | Shaders, Direct Mode vs Retained | Shaders procedurais Cycles/EEVEE; renderização via `GraphicsContext` JavaFX. | [`_apsu_shared_lib.py`](personagens/scripts/_apsu_shared_lib.py), [`pom.xml`](pom.xml) |
| **III. Modelagem Poligonal** | Topologia, Extrude, Inset, Loop Cut | Construção procedural dos modelos de Adapa, Kullullû, Enki e Leviatã via `bpy`. | [`01_adapa_heroi.py`](personagens/scripts/01_adapa_heroi.py), [`02_kullullu_boss.py`](personagens/scripts/02_kullullu_boss.py) |
| **IV. Transformações Geométricas**| Translação, Rotação, Matrizes | Hierarquia de ossos/segmentos de cauda; conversão de espaço de Mundo para Tela. | [`Camera.java`](src/main/java/br/apsu/core/Camera.java), [`HeroEntity.java`](src/main/java/br/apsu/model/hero/HeroEntity.java) |
| **V. Câmeras & Visualização** | Projeção Perspectiva, Parallax | Câmera 2.5D com scroll lateral e efeito Parallax nos planos de fundo. | [`Camera.java`](src/main/java/br/apsu/core/Camera.java) |
| **VI. Cenas Gráficas & PBR** | Materiais PBR, Roughness, Metallic | Materiais procedurais de obsidiana, magma Voronoi e escamas translucentes. | [`_apsu_shared_lib.py`](personagens/scripts/_apsu_shared_lib.py) |
| **VII. Curvas e Superfícies** | Curvas de Bézier, Continuidade | Função `smooth_angle_chain` para ondulação da cauda sem quebras de junta. | [`_apsu_shared_lib.py`](personagens/scripts/_apsu_shared_lib.py) |
| **VIII. Cor e Espaços de Cor** | RGB, HSV, Fresnel Rim Light | Leitura e quantização da paleta 60-30-10; iluminação Fresnel Rim Light. | [`LightingEngine.java`](src/main/java/br/apsu/graphics/LightingEngine.java) |
| **IX. Ray Tracing vs Rasterização**| Path Tracing, Global Illumination | Síntese offline com Cycles (Path Tracing) versus rasterização 2D a 60 FPS. | [`render_all_2d5_sprites.py`](tools/assets/render_all_2d5_sprites.py) |
| **X. Otimização & Bake** | Meshes, Frame Baking | Modelos 3D OBJ/MTL em runtime e bake PNG para elementos 2.5D. | [`Runtime3DLayer.java`](src/main/java/br/apsu/graphics/Runtime3DLayer.java), [`SpriteManager.java`](src/main/java/br/apsu/graphics/SpriteManager.java) |

---

## MENU DE COMANDOS ARCADE (MAKEFILE)

O `Makefile` adota um visual retro de fliperama no terminal e gerencia o ambiente automaticamente:

```text
+-----------------------------------------------------------------------+
|   [ARCADE ENGINE 2026] -- AS AGUAS DE APSU // RETRO GAME SYSTEM       |
+-----------------------------------------------------------------------+
| COMMAND                      | DESCRIPTION                            |
+------------------------------+----------------------------------------+
  make / make all              Run setup, organize assets & start game
  make run                     Start JavaFX 21 main game application
  make setup                   Check & install dependencies (idempotent)
  make setup-assets            Check Blender before regenerating 3D assets
  make setup-mpi               Check/install OpenMPI for offline map generation
  make build                   Compile Java 21 classes
  make test                    Execute JUnit 5 test suite (45 tests)
  make assets                  Full 3D character + 2.5D scenery pipeline
  make assets-variant-attacks  Export 3D attack poses for runtime
  make generate-characters     Re-generate .blend and runtime OBJ/MTL models
  make render-sprites          Render 2.5D scenery PNGs from .blend
  make copy-blends             Organize .blend 3D models into assets/
  make package                 Build executable Fat JAR in target/
  make docker-run              Build and run in Docker with Linux X11
  make mpi-demo                Run parallel MPI map generator
  make mpi-generate            Generate 5 JSON phase layouts via MPI
  make clean                   Clean build artifacts and release RAM
+-----------------------------------------------------------------------+
```

MPI is an offline map-generation utility, not part of the JavaFX frame loop. It does not synchronize character animation or rendering; those stay on the fixed-step game clock so they remain stable without MPI.

---

## SOLUÇÃO DE PROBLEMAS (TROUBLESHOOTING)

> [!TIP]
> **Java Version Mismatch / JavaFX Error**
> O jogo requer **Java 21 LTS**. O comando `make setup` ou `tools/setup/setup_environment.ps1` ajusta o ambiente automaticamente. Caso precise forçar manualmente:
> - **Linux:** `export JAVA_HOME=$HOME/java/current`
> - **Windows:** `$env:JAVA_HOME="C:\Program Files\Eclipse Adoptium\jdk-21.0.4.7-hotspot"`

> [!TIP]
> **Permissão de Execução do JAR Baixado no Linux ("Blocked for Security Reasons")**
> Se o navegador ou gerenciador de arquivos bloquear o `.jar` baixado por segurança:
> - **Via Terminal:** `chmod +x ~/Downloads/apsu-game-1.0.0.jar && java -jar ~/Downloads/apsu-game-1.0.0.jar`
> - **Via Interface Gráfica:** Clique com o botão direito no `.jar` -> *Propriedades* -> *Permissões* -> Marque *"Permitir execução do arquivo como programa"*.

> [!NOTE]
> **Renderização via Software (Hardware Sem GPU)**
> Se estiver rodando em máquina sem aceleração gráfica nativa ou em máquina virtual, execute com fallback por software:
> ```bash
> mvn javafx:run -Dprism.order=sw
> ```

---

## DOCUMENTAÇÃO DE GOVERNANÇA E CONTRIBUIÇÃO

Para consultar as diretrizes da equipe de 5 desenvolvedores, regras para agentes de IA e padrões de código:

- [`AGENTS.md`](AGENTS.md) — As 13 Regras Invioláveis de Desenvolvimento para Humanos e Agentes de IA.
- [`CONTRIBUTING.md`](CONTRIBUTING.md) — Matriz de Responsabilidade dos 5 DEVs, Workflow Git e Conventional Commits.
- [`DEVELOPMENT.md`](DEVELOPMENT.md) — Guia detalhado de Setup, Build, Test e Run (Linux & Windows).
- [`ARCHITECTURE.md`](ARCHITECTURE.md) — Especificação técnica dos subsistemas de runtime, física e áudio.
- [`LICENSE`](LICENSE) — Apache License 2.0.

---

<div align="center">

**As Águas de Apsu: A Lenda dos Apkallu — 2026**

</div>
