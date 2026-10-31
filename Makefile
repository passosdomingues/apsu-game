# ============================================================
# AS AGUAS DE APSU -- RETRO ARCADE ENGINE (1980-2026)
# Makefile de Orquestração Multiplataforma (Linux / Windows)
# ============================================================

APP_NAME     := apsu-game
VERSION      := 20261031-111014
MAIN_CLASS   := br.apsu.Launcher
SRC_DIR      := src/main/java
RES_DIR      := src/main/resources
TARGET_DIR   := target

# ============================================================
# DETECÇÃO AUTOMÁTICA DE SISTEMA OPERACIONAL (LINUX VS WINDOWS)
# ============================================================
ifeq ($(OS),Windows_NT)
    IS_WINDOWS    := 1
    SO_NAME       := Windows
    SHELL_SETUP   := powershell -ExecutionPolicy Bypass -File tools/setup/setup_environment.ps1
    
    JAVA_CMD      ?= java
    JAVAC_CMD     ?= javac
    MVN_CMD       ?= mvn
    BLENDER_BIN   ?= $(shell where blender 2>NUL || echo "C:\Program Files\Blender Foundation\Blender 4.5\blender.exe")
    PYTHON_CMD    ?= python
    
    CPU_CORES     := $(shell powershell -NoProfile -Command "(Get-CimInstance Win32_ComputerSystem).NumberOfLogicalProcessors" 2>NUL || echo 4)
    RAM_GB        := $(shell powershell -NoProfile -Command "[math]::Round((Get-CimInstance Win32_ComputerSystem).TotalPhysicalMemory / 1GB)" 2>NUL || echo 8)
    
    RM_DIR        := rmdir /s /q
    RM_FILE       := del /f /q
    MKDIR         := mkdir
else
    IS_WINDOWS    := 0
    SO_NAME       := Linux
    SHELL_SETUP   := bash tools/setup/setup_environment.sh
    
    JDK21_CANDIDATE := $(shell for d in "$$JAVA_HOME" $$HOME/java/current /opt/java-temurin-21 /usr/lib/jvm/java-21-openjdk-amd64 /usr/lib/jvm/java-21-openjdk $$HOME/.local/jdk-21; do if [ -n "$$d" ] && [ -x "$$d/bin/java" ]; then echo "$$d"; break; fi; done)
    JAVA_HOME_DIR   ?= $(JDK21_CANDIDATE)
    
    JAVA_CMD        := $(if $(JAVA_HOME_DIR),$(JAVA_HOME_DIR)/bin/java,java)
    JAVAC_CMD       := $(if $(JAVA_HOME_DIR),$(JAVA_HOME_DIR)/bin/javac,javac)
    MVN_CMD         := $(if $(JAVA_HOME_DIR),JAVA_HOME=$(JAVA_HOME_DIR) mvn,mvn)
    
    BLENDER_BIN     ?= $(shell which blender 2>/dev/null || for b in /opt/blender-4.5.5-lts/blender /opt/blender-4.5.0-lts/blender /opt/blender-4.5-lts/blender $$HOME/.local/blender-4.5/blender /usr/bin/blender; do if [ -x "$$b" ]; then echo "$$b"; break; fi; done || echo "blender")
    PYTHON_CMD      := python3
    
    CPU_CORES       := $(shell nproc 2>/dev/null || echo 4)
    RAM_MIB         := $(shell host=$$(free -m 2>/dev/null | awk '/Mem:/ {print $$2}'); limit=$$(cat /sys/fs/cgroup/memory.max 2>/dev/null); if [ -z "$$limit" ]; then limit=$$(cat /sys/fs/cgroup/memory/memory.limit_in_bytes 2>/dev/null); fi; if [ -n "$$host" ] && [ -n "$$limit" ] && [ "$$limit" != max ]; then limit=$$(expr "$$limit" / 1048576 2>/dev/null || echo $$host); if [ "$$limit" -lt "$$host" ]; then echo $$limit; else echo $$host; fi; else echo "$${host:-8192}"; fi)
    RAM_GB          := $(shell awk -v m="$(RAM_MIB)" 'BEGIN {printf "%.1f", m/1024}')
    MACHINE_MODEL   := $(shell cat /sys/devices/virtual/dmi/id/product_name 2>/dev/null || echo "Linux host")
    CPU_MODEL       := $(shell lscpu 2>/dev/null | sed -n 's/^Model name:[[:space:]]*//p' | head -1)
    OS_RELEASE      := $(shell . /etc/os-release 2>/dev/null; echo "$${PRETTY_NAME:-Linux}")
    GPU_INFO        := $(shell lspci 2>/dev/null | grep -Ei 'VGA compatible controller|3D controller|Display controller' | sed 's/^[^:]*: //' | paste -sd '; ' -)
    GL_RENDERER     := $(shell glxinfo -B 2>/dev/null | sed -n 's/^OpenGL renderer string: //p' | head -1)
    GL_ACCELERATED  := $(shell glxinfo -B 2>/dev/null | sed -n 's/^ *Accelerated: //p' | head -1)
    RM_DIR          := rm -rf
    RM_FILE         := rm -f
    MKDIR           := mkdir -p
endif

APSU_PROFILE ?= auto
APSU_GPU ?= auto
APSU_PERF ?= 0
APSU_AUDIO ?= 1
APSU_JFR ?= 0
APSU_JFR_FILE ?= target/apsu-gameplay.jfr
APSU_JFR_THREAD_STACKS ?= 0
APSU_COMMA := ,
ifeq ($(APSU_PROFILE),auto)
    ifeq ($(IS_WINDOWS),1)
        APSU_PROFILE := $(shell if [ "$(CPU_CORES)" -ge 12 ] && [ "$(RAM_GB)" -ge 16 ]; then echo performance; elif [ "$(CPU_CORES)" -ge 4 ] && [ "$(RAM_GB)" -ge 8 ]; then echo balanced; else echo low; fi)
    else
        APSU_PROFILE := $(shell if [ "$(CPU_CORES)" -ge 12 ] && [ "$(RAM_MIB)" -ge 16384 ] && [ "$(GL_ACCELERATED)" = yes ] && printf '%s' "$(GPU_INFO)" | grep -Eqi 'Intel|AMD|ATI|NVIDIA'; then echo performance; elif [ "$(CPU_CORES)" -ge 4 ] && [ "$(RAM_MIB)" -ge 7168 ]; then echo balanced; else echo low; fi)
    endif
endif
ifeq ($(filter $(APSU_PROFILE),performance balanced low),)
    $(error APSU_PROFILE deve ser auto, performance, balanced ou low)
endif
ifeq ($(APSU_PROFILE),performance)
    APSU_JVM_ARGS ?= -Xms256m -Xmx3g -Dapsu.quality=high
else ifeq ($(APSU_PROFILE),balanced)
    APSU_JVM_ARGS ?= -Xms128m -Xmx2g -Dapsu.quality=balanced
else
    APSU_JVM_ARGS ?= -Xms64m -Xmx1g -Dapsu.quality=low
endif
APSU_PERF_ARGS := $(if $(filter 1 true yes,$(APSU_PERF)),-Dapsu.perf=true,)
APSU_AUDIO_ENABLED := $(if $(filter 1 true yes on,$(APSU_AUDIO)),true,false)
APSU_AUDIO_ARGS := -Dapsu.audio.enabled=$(APSU_AUDIO_ENABLED)
APSU_JFR_THREAD_ARGS := $(if $(filter 1 true yes,$(APSU_JFR_THREAD_STACKS)),$(APSU_COMMA)jdk.ThreadStart#stackTrace=true,)
APSU_JFR_ARGS := $(if $(filter 1 true yes,$(APSU_JFR)),-XX:StartFlightRecording=filename=$(APSU_JFR_FILE)$(APSU_COMMA)settings=profile$(APSU_COMMA)dumponexit=true$(APSU_JFR_THREAD_ARGS),)
JFR_CMD ?= jfr
ifeq ($(APSU_GPU),amd)
    APSU_GPU_ENV := DRI_PRIME=1
    APSU_GPU_SELECTED := Radeon (manual)
else ifeq ($(APSU_GPU),intel)
    APSU_GPU_ENV := DRI_PRIME=0
    APSU_GPU_SELECTED := Intel UHD (manual)
else ifneq ($(APSU_GPU),auto)
    $(error APSU_GPU deve ser auto, intel ou amd)
else
    APSU_GPU_SELECTED := renderer padrão da sessão
endif

ADAPA_RUNTIME_MODEL_PATHS := personagens/blends/01_adapa_heroi/01_adapa_heroi.blend,personagens/blends/01_adapa_heroi/01_adapa_ataque_thrust.blend

# ============================================================
# PALETA DE CORES ANSI RETRO ARCADE (NEON CYAN, MAGENTA, YELLOW)
# ============================================================
CLR_HEADER := \033[1;35m
CLR_CYAN   := \033[1;36m
CLR_YELLOW := \033[1;33m
CLR_GREEN  := \033[1;32m
CLR_BLUE   := \033[1;34m
CLR_WHITE  := \033[1;37m
CLR_DIM    := \033[2;37m
CLR_RED    := \033[1;31m
CLR_RESET  := \033[0m

.PHONY: all setup setup-assets audio-assets convert-phase-music copy-blends clean-blender-backups test coverage build run perf-summary system-info stop generate-characters generate-geyser-model generate-lava-pool-model generate-abyssal-enemies export-runtime-models export-model render-model-sprites refresh-model refresh-adapa-models refresh-adapa-sprites render-sprites render-3d-previews render-backgrounds assets assets-procedural assets-regenerate package docker-build docker-run clean help

# Target padrão
all: setup run

## Verifica e instala dependências de forma 100% idempotente
setup:
	@echo "$(CLR_HEADER)+-----------------------------------------------------------------------+$(CLR_RESET)"
	@echo "$(CLR_HEADER)| [SETUP] SYSTEM VERIFICATION & DEPENDENCY ORCHESTRATION               |$(CLR_RESET)"
	@echo "$(CLR_HEADER)+-----------------------------------------------------------------------+$(CLR_RESET)"
	@$(if $(filter 1,$(IS_WINDOWS)),$(SHELL_SETUP) -RuntimeOnly,APSU_RUNTIME_ONLY=1 $(SHELL_SETUP))

## Instala Blender, necessário apenas para regenerar assets 3D
setup-assets:
	@echo "$(CLR_CYAN)[SETUP] Checking asset-generation dependencies...$(CLR_RESET)"
	@$(if $(filter 1,$(IS_WINDOWS)),$(SHELL_SETUP),$(SHELL_SETUP))

## Organiza e copia os modelos 3D .blend para assets/
copy-blends:
	@echo "$(CLR_CYAN)[ASSETS] Organizing 3D .blend models in assets/...$(CLR_RESET)"
	@mkdir -p assets/characters/adapa assets/characters/kullullu assets/characters/enki \
	         assets/characters/inimigos assets/characters/guardioes assets/scenery
	@rm -f assets/characters/adapa.blend
	@cp -f personagens/blends/01_adapa_heroi/*.blend assets/characters/adapa/ 2>/dev/null || true
	@cp -f personagens/blends/02_kullullu_boss/*.blend assets/characters/kullullu/ 2>/dev/null || true
	@cp -f personagens/blends/03_enki_npc/*.blend assets/characters/enki/ 2>/dev/null || true
	@cp -f personagens/blends/04_peixe_sombrio_inimigo/*.blend assets/characters/inimigos/ 2>/dev/null || true
	@cp -f personagens/blends/05_guardiao_atlante_npc/*.blend assets/characters/guardioes/ 2>/dev/null || true
	@cp -f personagens/blends/06_*/*.blend assets/scenery/ 2>/dev/null || true
	@cp -f personagens/blends/07_*/*.blend assets/scenery/ 2>/dev/null || true
	@cp -f personagens/blends/08_*/*.blend assets/scenery/ 2>/dev/null || true
	@cp -f personagens/blends/09_*/*.blend assets/scenery/ 2>/dev/null || true
	@cp -f personagens/blends/10_*/*.blend assets/scenery/ 2>/dev/null || true
	@cp -f personagens/blends/11_*/*.blend assets/characters/inimigos/ 2>/dev/null || true
	@echo "$(CLR_GREEN)[OK] 3D .blend models organized successfully.$(CLR_RESET)"

## Remove backups .blend1
clean-blender-backups:
	@find personagens/blends -type f -name '*.blend1' -delete 2>/dev/null || true
	@echo "$(CLR_GREEN)[OK] Temporary .blend1 backup files removed.$(CLR_RESET)"

## Executa a suíte de testes JUnit 5
test: setup
	@mkdir -p logs
	@echo "$(CLR_YELLOW)[TEST ] Running JUnit 5 test suite...$(CLR_RESET)"
	@$(MVN_CMD) test
	@echo "$(CLR_GREEN)[OK] All unit and integration tests passed successfully.$(CLR_RESET)"

## Executa testes e gera relatório JaCoCo em target/site/jacoco/index.html
coverage: setup
	@echo "$(CLR_YELLOW)[COVERAGE] Running tests and measuring JaCoCo coverage...$(CLR_RESET)"
	@$(MVN_CMD) verify
	@echo "$(CLR_GREEN)[OK] Coverage report: target/site/jacoco/index.html$(CLR_RESET)"

## Compila o código Java 21
build: setup
	@echo "$(CLR_CYAN)[BUILD] Compiling Java 21 source code...$(CLR_RESET)"
	@$(MVN_CMD) compile
	@echo "$(CLR_GREEN)[OK] Java compilation complete.$(CLR_RESET)"

## Executa o jogo principal (JavaFX 21)
run: setup
	@echo "$(CLR_HEADER)+-----------------------------------------------------------------------+$(CLR_RESET)"
	@echo "$(CLR_HEADER)| [RUN  ] LAUNCHING AS AGUAS DE APSU ON $(SO_NAME)                         |$(CLR_RESET)"
	@echo "$(CLR_HEADER)+-----------------------------------------------------------------------+$(CLR_RESET)"
	@echo "$(CLR_CYAN)[PERF ] Perfil $(APSU_PROFILE) | $(CPU_CORES) threads | ~$(RAM_GB) GB RAM | GPU: $(APSU_GPU_SELECTED)$(CLR_RESET)"
	@$(APSU_GPU_ENV) JAVA_TOOL_OPTIONS="$${JAVA_TOOL_OPTIONS:+$$JAVA_TOOL_OPTIONS }$(APSU_JVM_ARGS) $(APSU_AUDIO_ARGS) $(APSU_PERF_ARGS) $(APSU_JFR_ARGS)" $(MVN_CMD) javafx:run

## Mostra o resumo de uma gravação JFR criada com APSU_JFR=1
perf-summary:
	@test -f "$(APSU_JFR_FILE)" || { echo "Arquivo JFR não encontrado: $(APSU_JFR_FILE)"; exit 1; }
	@$(JFR_CMD) summary "$(APSU_JFR_FILE)"

## Mostra o hardware detectado e os perfis disponíveis para execução
system-info:
	@echo "$(CLR_HEADER)[APSU] Diagnóstico do sistema$(CLR_RESET)"
	@echo "  SO: $(if $(filter 1,$(IS_WINDOWS)),$(SO_NAME),$(OS_RELEASE))"
	@echo "  Máquina: $(MACHINE_MODEL)"
	@echo "  CPU: $(CPU_MODEL) ($(CPU_CORES) threads disponíveis)"
	@echo "  RAM/limite de memória detectado: ~$(RAM_GB) GB"
	@echo "  GPU(s): $(if $(GPU_INFO),$(GPU_INFO),não detectada)"
	@echo "  Renderer OpenGL padrão: $(if $(GL_RENDERER),$(GL_RENDERER),indisponível nesta sessão/display) ($(if $(GL_ACCELERATED),aceleração $(GL_ACCELERATED),aceleração não detectada))"
	@echo "  Perfil selecionado: $(APSU_PROFILE) (use APSU_PROFILE=low|balanced|performance para substituir)"
	@echo "  GPU selecionada: $(APSU_GPU_SELECTED) (use APSU_GPU=intel|amd para comparar)"
	@echo "  Diagnóstico de frame: use APSU_PERF=1 make run para FPS/update/render e tempo de carga OBJ"

## Regenera os modelos .blend editáveis via Python/bpy
generate-characters: setup-assets
	@echo "$(CLR_CYAN)[BAKE ] Re-generating 3D models (.blend) via Blender Python API...$(CLR_RESET)"
	@BLENDER_BIN="$(BLENDER_BIN)" $(PYTHON_CMD) personagens/scripts/gerador_mestre_apsu.py
	@echo "$(CLR_GREEN)[OK] Blender source models re-generated.$(CLR_RESET)"

## Exporta personagens do Blender como malhas OBJ/MTL carregadas no jogo
export-runtime-models: setup-assets
	@echo "$(CLR_CYAN)[3D   ] Exporting runtime character meshes from Blender...$(CLR_RESET)"
	@$(BLENDER_BIN) --background --python tools/assets/export_runtime_3d_models.py -- "$(CURDIR)" $(if $(MODEL),--only-path "$(MODEL)",)
	@echo "$(CLR_GREEN)[OK] Runtime 3D character models exported.$(CLR_RESET)"

## Exporta apenas um .blend alterado (caminho relativo a personagens/blends ou absoluto)
export-model: setup-assets
	@test -n "$(MODEL)" || { echo "Use: make export-model MODEL=personagens/blends/.../arquivo.blend"; exit 2; }
	@$(BLENDER_BIN) --background --python tools/assets/export_runtime_3d_models.py -- "$(CURDIR)" --only-path "$(MODEL)"

## Compatibilidade: atualiza apenas a malha 3D de runtime de um .blend
render-model-sprites: export-model

## Atualiza as malhas 3D do Adapa (modelo de nado e pose de ataque)
refresh-adapa-models: MODEL=$(ADAPA_RUNTIME_MODEL_PATHS)
refresh-adapa-models: export-runtime-models

## Compatibilidade: o runtime agora atualiza malhas 3D, não PNGs de sprites
refresh-adapa-sprites: refresh-adapa-models

## Exporta a malha de runtime de uma única fonte Blender
refresh-model: export-model

## Gera o modelo low-poly 3D do gêiser para o pipeline de runtime
generate-geyser-model: setup-assets
	@$(BLENDER_BIN) --background --python tools/assets/generate_geyser_3d.py -- "$(CURDIR)"

## Gera a piscina irregular de lava em malha 3D low-poly
generate-lava-pool-model: setup-assets
	@$(BLENDER_BIN) --background --python tools/assets/generate_lava_pool_3d.py -- "$(CURDIR)"

## Gera inimigos 3D do golfinho predador e criaturas abissais
generate-abyssal-enemies: setup-assets
	@$(BLENDER_BIN) --background --python tools/assets/generate_abyssal_enemies.py -- "$(CURDIR)"

## Compatibilidade: exporta as malhas Blender usadas diretamente no runtime 3D
render-sprites: export-runtime-models

## Gera renders auxiliares em personagens/renders/previews (fora do runtime)
render-3d-previews: setup-assets
	@$(BLENDER_BIN) --background --python tools/assets/render_3d_previews.py

## Renderiza panoramas 3D estilizados para as cinco fases
render-backgrounds: setup-assets
	@echo "$(CLR_CYAN)[BG 3D] Rendering phase panoramas with Blender...$(CLR_RESET)"
	@$(BLENDER_BIN) --background --python tools/assets/generate_phase_backgrounds.py -- "$(CURDIR)"
	@echo "$(CLR_GREEN)[OK] Phase panoramas written to src/main/resources/backgrounds/.$(CLR_RESET)"

## Gera recursos runtime a partir das fontes existentes sem sobrescrever .blend
assets: export-runtime-models render-backgrounds audio-assets copy-blends
	@echo "$(CLR_GREEN)[OK] Runtime 3D meshes, phase backgrounds, and audio updated.$(CLR_RESET)"

## Regenera apenas os modelos procedurais de hazards/fauna e atualiza recursos runtime
assets-procedural: generate-geyser-model generate-lava-pool-model generate-abyssal-enemies export-runtime-models copy-blends
	@echo "$(CLR_GREEN)[OK] Procedural Blender models and runtime assets regenerated.$(CLR_RESET)"

## Regenera fontes .blend e depois reconstrói todos os recursos (pode sobrescrever fontes)
assets-regenerate: generate-characters generate-geyser-model generate-lava-pool-model generate-abyssal-enemies export-runtime-models render-backgrounds audio-assets copy-blends clean-blender-backups
	@echo "$(CLR_GREEN)[OK] Regenerated Blender sources and runtime assets.$(CLR_RESET)"

## Renderiza variantes acústicas de profundidade a partir dos WAVs-fonte com FFmpeg
audio-assets: convert-phase-music
	@command -v ffmpeg >/dev/null 2>&1 || (echo "FFmpeg é necessário para make audio-assets"; exit 1)
	@$(PYTHON_CMD) tools/audio/render_audio_assets.py

## Converte as quatro músicas de fase para WAV PCM e atualiza o fallback costeiro
convert-phase-music:
	@$(PYTHON_CMD) tools/audio/convert_phase_music.py

## Pipeline rápido de variantes
## Gera o JAR de execução e o pacote de recursos partilhado
package: build
	@echo "$(CLR_CYAN)[PACK ] Building app JAR and shared resources in target/...$(CLR_RESET)"
	@$(MVN_CMD) package -q -DskipTests
	@echo "$(CLR_GREEN)[OK] App JAR: target/apsu-game-$(VERSION)-jar-with-dependencies.jar$(CLR_RESET)"
	@echo "$(CLR_GREEN)[OK] Shared game resources: target/apsu-game-$(VERSION)-resources.zip$(CLR_RESET)"

## Constrói a imagem Docker
docker-build:
	@echo "$(CLR_CYAN)[DOCK ] Building Docker image $(APP_NAME):$(VERSION)...$(CLR_RESET)"
	@docker build -t $(APP_NAME):$(VERSION) .

## Executa container Docker com X11 forwarding
docker-run: docker-build
	@echo "$(CLR_CYAN)[DOCK ] Running game in Docker container with X11 forwarding...$(CLR_RESET)"
	@command -v xhost >/dev/null 2>&1 && xhost +local:docker > /dev/null 2>&1 || true
	@DRI_FLAGS=$$( [ -d /dev/dri ] && echo "--device /dev/dri:/dev/dri" || echo "" ); \
	docker run --rm --name $(APP_NAME) -e DISPLAY=$${DISPLAY:-:0} -e LIBGL_ALWAYS_SOFTWARE=$${LIBGL_ALWAYS_SOFTWARE:-0} \
	-v $${X11_SOCKET_DIR:-/tmp/.X11-unix}:/tmp/.X11-unix:rw $$DRI_FLAGS $(APP_NAME):$(VERSION)

## Encerra containers Docker
stop:
	@echo "$(CLR_YELLOW)[STOP ] Stopping active Docker containers...$(CLR_RESET)"
	@docker ps -q --filter ancestor=$(APP_NAME):$(VERSION) | xargs -r docker stop 2>/dev/null || true
	@docker ps -q --filter name=$(APP_NAME) | xargs -r docker stop 2>/dev/null || true
	@echo "$(CLR_GREEN)[OK] Docker containers stopped.$(CLR_RESET)"

## Limpa artefatos temporários
clean: stop
	@echo "$(CLR_YELLOW)[CLEAN] Removing build directory (target/)...$(CLR_RESET)"
	@$(RM_DIR) $(TARGET_DIR) 2>/dev/null || true
	@echo "$(CLR_GREEN)[OK] Project clean completed.$(CLR_RESET)"

## Exibe o menu de ajuda estilo fliperama retro arcade
help:
	@echo ""
	@echo "$(CLR_HEADER)+-----------------------------------------------------------------------+$(CLR_RESET)"
	@echo "$(CLR_HEADER)|   [ARCADE ENGINE 2026] -- AS AGUAS DE APSU // RETRO GAME SYSTEM       |$(CLR_RESET)"
	@echo "$(CLR_HEADER)+-----------------------------------------------------------------------+$(CLR_RESET)"
	@echo "$(CLR_WHITE)| COMMAND                      | DESCRIPTION                            |$(CLR_RESET)"
	@echo "$(CLR_DIM)+------------------------------+----------------------------------------+$(CLR_RESET)"
	@echo "  $(CLR_GREEN)make / make all$(CLR_RESET)              Check runtime dependencies and start the game"
	@echo "  $(CLR_GREEN)make run$(CLR_RESET)                     Start JavaFX 21 main game application"
	@echo "  $(CLR_GREEN)make system-info$(CLR_RESET)             Detect machine, graphics driver and runtime profile"
	@echo "  $(CLR_GREEN)make setup$(CLR_RESET)                   Check & install dependencies (idempotent)"
	@echo "  $(CLR_GREEN)make build$(CLR_RESET)                   Compile Java 21 classes"
	@echo "  $(CLR_GREEN)make test$(CLR_RESET)                    Execute the JUnit 5 test suite"
	@echo "  $(CLR_CYAN)make assets$(CLR_RESET)                  Refresh runtime assets from existing sources"
	@echo "  $(CLR_CYAN)make assets-procedural$(CLR_RESET)       Rebuild generated hazard and fauna Blender sources"
	@echo "  $(CLR_CYAN)make assets-regenerate$(CLR_RESET)       Regenerate Blender source files and runtime assets"
	@echo "  $(CLR_CYAN)make refresh-model MODEL=...$(CLR_RESET)  Export one edited model and its sprite preview"
	@echo "  $(CLR_CYAN)make refresh-adapa-models$(CLR_RESET)   Export Adapa's runtime 3D swim and attack meshes"
	@echo "  $(CLR_CYAN)make convert-phase-music$(CLR_RESET)      Convert phase MPEG audio to WAV with FFmpeg"
	@echo "  $(CLR_CYAN)make audio-assets$(CLR_RESET)            Convert phase music and render audio profiles"
	@echo "  $(CLR_CYAN)APSU_PERF=1 make run$(CLR_RESET)          Log FPS, frame times and OBJ load duration"
	@echo "  $(CLR_CYAN)APSU_AUDIO=0 make run$(CLR_RESET)         Disable audio to isolate media/driver frame pacing"
	@echo "  $(CLR_CYAN)APSU_PERF=1 APSU_JFR=1 make run$(CLR_RESET) Log gameplay and record Java Flight Recorder data"
	@echo "  $(CLR_CYAN)APSU_JFR_THREAD_STACKS=1$(CLR_RESET)          Include stacks for every thread creation (diagnostic overhead)"
	@echo "  $(CLR_CYAN)make perf-summary$(CLR_RESET)             Summarize target/apsu-gameplay.jfr after closing the game"
	@echo "  $(CLR_CYAN)make generate-characters$(CLR_RESET)   Re-generate .blend and runtime models"
	@echo "  $(CLR_CYAN)make render-sprites$(CLR_RESET)        Compatibility alias: export runtime 3D meshes"
	@echo "  $(CLR_CYAN)make render-3d-previews$(CLR_RESET)   Create review-only Blender renders outside runtime assets"
	@echo "  $(CLR_CYAN)make generate-abyssal-enemies$(CLR_RESET) Generate dolphin, octopus and squid models"
	@echo "  $(CLR_CYAN)make copy-blends$(CLR_RESET)           Organize .blend 3D models into assets/"
	@echo "  $(CLR_CYAN)make package$(CLR_RESET)               Build app JAR and shared game resources"
	@echo "  $(CLR_YELLOW)make docker-run$(CLR_RESET)           Run inside Docker container (Linux)"
	@echo "  $(CLR_RED)make clean$(CLR_RESET)                Clean build artifacts and release RAM"
	@echo "$(CLR_DIM)+-----------------------------------------------------------------------+$(CLR_RESET)"
	@echo "$(CLR_HEADER)| SYSTEM DIAGNOSTICS                                                    |$(CLR_RESET)"
	@echo "$(CLR_DIM)+-----------------------------------------------------------------------+$(CLR_RESET)"
	@echo "  $(CLR_CYAN)OPERATING SYSTEM$(CLR_RESET) : $(CLR_WHITE)$(SO_NAME)$(CLR_RESET)"
	@echo "  $(CLR_CYAN)HARDWARE SPECS  $(CLR_RESET) : $(CLR_WHITE)$(CPU_CORES) Cores / ~$(RAM_GB)GB RAM$(CLR_RESET)"
	@echo "  $(CLR_CYAN)BLENDER BINARY  $(CLR_RESET) : $(CLR_WHITE)$(BLENDER_BIN)$(CLR_RESET)"
	@echo "$(CLR_HEADER)+-----------------------------------------------------------------------+$(CLR_RESET)"
	@echo ""
