# Guia de Desenvolvimento — As Águas de Apsu (`apsu-game`)

Este documento instrui novos desenvolvedores no processo de preparação do ambiente local, compilação, execução da suíte de testes e inicialização do jogo tanto em sistemas **Linux (Debian/Ubuntu/Mint)** quanto em **Windows (10/11)**.

---

## Próximos Passos (Clone -> Setup -> Build -> Test -> Run)

### 1. Clonando o Repositório
```bash
git clone https://github.com/rafael/apsu-game.git
cd apsu-game
```

---

### 2. Preparação Automática de Ambiente (Setup Idempotente)

O alvo padrão `make` verifica somente as dependências de runtime (**Java 21 LTS** e **Apache Maven**). Os OBJ/MTL dos personagens e as imagens do cenário já estão em `src/main/resources`; Blender só é necessário para regenerar assets.

#### No Linux (Debian / Ubuntu / Linux Mint):
```bash
make setup
# ou diretamente via script:
APSU_RUNTIME_ONLY=1 bash tools/setup/setup_environment.sh
```

Para gerar assets, instale Blender sob demanda:
```bash
make setup-assets   # Blender
```

#### No Windows (PowerShell 5.1+):
```powershell
powershell -ExecutionPolicy Bypass -File tools/setup/setup_environment.ps1
```

---

### 3. Compilação do Código Fonte

```bash
# Compilar todas as classes Java 21
make build
# ou via Maven:
mvn compile
```

---

### 4. Execução dos Testes Automatizados

O projeto possui uma suíte com 45 testes JUnit 5 cobrindo motor gráfico, físicas, câmera, eventos, persistência e IA:

```bash
# Rodar todos os testes com relatório
make test
# ou via Maven:
mvn test
```

---

### 5. Execução do Jogo (Runtime 60 FPS)

```bash
# Iniciar a aplicação JavaFX principal
make run
# Ver o hardware/renderer e o perfil que o Makefile detectou
make system-info
# Substituir o perfil automático (performance, balanced ou low)
APSU_PROFILE=balanced make run
# Em notebook híbrido com Mesa, comparar a Radeon dedicada
APSU_GPU=amd make run
# Log phase, FPS, frame pacing, update/render, GC/heap and async model loads
APSU_PERF=1 make run
# Isolate rendering from JavaFX audio/media during performance comparison
APSU_AUDIO=0 APSU_PERF=1 make run
# Record Java Flight Recorder data alongside the live frame metrics
APSU_PERF=1 APSU_JFR=1 make run
# Include thread-creation stacks to investigate recurring runtime threads
APSU_PERF=1 APSU_JFR=1 APSU_JFR_THREAD_STACKS=1 make run
# Summarize target/apsu-gameplay.jfr after closing the game
make perf-summary
# ou via Maven:
mvn javafx:run
```

`make run` escolhe `performance` a partir de 12 threads, 16 GB de RAM e GPU
acelerada detectada. O perfil `performance` inicia com modelos 3D; abaixo de
50 FPS, o loop reduz iluminação e partículas sem trocar malhas 3D por sprites.
`balanced` e `low` mantêm os modelos 3D e reduzem somente efeitos visuais auxiliares.
`auto` mantém o renderer padrão da sessão; `APSU_GPU=amd` pede `DRI_PRIME=1` e
`APSU_GPU=intel` pede `DRI_PRIME=0` para comparar manualmente. Confira
`make system-info` para ver o renderer OpenGL ativo. `balanced` e `low`
limitam progressivamente a memória máxima da JVM e iniciam com efeitos reduzidos,
sem alterar regras de jogo, conteúdo ou colisões.
`APSU_AUDIO=0` desliga música e efeitos temporariamente para um teste A/B do
renderer; o padrão é `1` e deve ser mantido no jogo normal.

Com `APSU_PERF=1`, cada amostra de um segundo inclui fase/estado, FPS, p50/p95 e
pior intervalo entre frames, contagem de frames acima de 16/33 ms, tempos médios
de update/render, passos de simulação, coleta de lixo/tempo e heap. Os modelos
OBJ são carregados fora do thread de render; os tempos aparecem como `[PERF] OBJ
async`. Para comparar uma fase, registre ao menos 60 segundos depois do primeiro
carregamento e anote fase, dificuldade, perfil (`APSU_PROFILE`), renderer de
`make system-info` e número de inimigos visíveis. O tempo de `render` mede a
chamada do renderer, enquanto p95/worst pulse inclui espera e cadência JavaFX.
`APSU_JFR=1` grava também `target/apsu-gameplay.jfr`; após encerrar o jogo,
`make perf-summary` lista eventos de CPU, threads, alocação e GC. O caminho pode
ser substituído com `APSU_JFR_FILE=...`. Para identificar quem cria threads,
`APSU_JFR_THREAD_STACKS=1` inclui pilhas em cada evento; use somente durante
diagnóstico, pois aumenta o custo e o tamanho da gravação.

---

### 6. Malhas 3D no runtime (Blender 4.5 LTS)

Caso modifique algum script Python em `personagens/scripts/`:

```bash
# Atualizar recursos runtime usando os .blend existentes (não altera fontes Blender)
make assets

# Regenerar os .blend de elementos procedurais de hazards/fauna
make assets-procedural

# Exportar uma malha 3D editada
make refresh-model MODEL=personagens/blends/04_peixe_sombrio_inimigo/04_peixe_sombrio_inimigo.blend

# Atualizar as malhas 3D do Adapa, incluindo a pose de ataque
make refresh-adapa-models

# Exportar novamente apenas os modelos 3D de runtime a partir dos .blend existentes
make export-runtime-models

# Regenerar fontes .blend e recursos (pode substituir fontes editadas manualmente)
make assets-regenerate

# Converter música MPEG das fases para WAV PCM ou atualizar panoramas 3D
make convert-phase-music
make render-backgrounds
```

Inimigos, NPCs e elementos de fase (recifes/cardumes, naufrágio, baú, ruínas,
correntes, marco de descida e obstáculos) usam malhas OBJ/MTL em `Runtime3DLayer`
em todos os perfis. Adapa usa o modelo vertical e a pose thrust exportados do
`.blend`; a direção muda pela rotação da malha 3D. O ataque visual acompanha o
estado de disparo das bolhas, inclusive no modo múltiplo. Para um asset isolado,
rode `make refresh-model MODEL=personagens/blends/.../arquivo.blend`; para o
herói e a pose de ataque, rode `make refresh-adapa-models`. Os modelos ficam em
`src/main/resources/models/`. PNGs gerados são somente previews/compatibilidade,
fora do caminho normal de gameplay.

`make docker-run` repassa `/dev/dri` quando disponível e habilita a seleção
normal do driver. Para Docker Compose com GPU Intel/AMD, use
`docker compose -f docker-compose.yml -f docker-compose.gpu.yml up`. Defina
`LIBGL_ALWAYS_SOFTWARE=1` apenas para diagnosticar problemas do driver.

---
