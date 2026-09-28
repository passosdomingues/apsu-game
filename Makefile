# ============================================================
# AS AGUAS DE APSU -- RETRO ARCADE ENGINE (1980-2026)
# Makefile de Orquestração Multiplataforma (Linux / Windows)
# ============================================================

APP_NAME     := apsu-game
VERSION      := 0.2.0-SNAPSHOT
MAIN_CLASS   := br.apsu.Launcher
SRC_DIR      := src/main/java
RES_DIR      := src/main/resources
TARGET_DIR   := target
MPI_DIR      := mpi

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
    RAM_GB          := $(shell free -g 2>/dev/null | awk '/Mem:/ {print $$2}' || echo 8)
    
    RM_DIR          := rm -rf
    RM_FILE         := rm -f
    MKDIR           := mkdir -p
endif

MPI_PROCESSES ?= 8

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

.PHONY: all setup setup-assets setup-mpi copy-blends clean-blender-backups test build run stop generate-characters generate-geyser-model export-runtime-models generate-variant-attacks render-sprites render-variant-attacks assets assets-variant-attacks package docker-build docker-run mpi-demo mpi-generate clean help

# Target padrão
all: setup run

## Verifica e instala dependências de forma 100% idempotente
setup:
	@echo "$(CLR_HEADER)+-----------------------------------------------------------------------+$(CLR_RESET)"
	@echo "$(CLR_HEADER)| [SETUP] SYSTEM VERIFICATION & DEPENDENCY ORCHESTRATION               |$(CLR_RESET)"
	@echo "$(CLR_HEADER)+-----------------------------------------------------------------------+$(CLR_RESET)"
	@$(if $(filter 1,$(IS_WINDOWS)),$(SHELL_SETUP) -RuntimeOnly,APSU_RUNTIME_ONLY=1 $(SHELL_SETUP))

## Instala também Blender/OpenMPI, necessários só para geração de assets e mapas
setup-assets:
	@echo "$(CLR_CYAN)[SETUP] Checking asset-generation dependencies...$(CLR_RESET)"
	@$(if $(filter 1,$(IS_WINDOWS)),$(SHELL_SETUP),APSU_SKIP_MPI=1 $(SHELL_SETUP))

## Instala OpenMPI sob demanda, sem baixar Blender
setup-mpi:
	@echo "$(CLR_CYAN)[SETUP] Checking OpenMPI dependencies...$(CLR_RESET)"
	@$(if $(filter 1,$(IS_WINDOWS)),$(SHELL_SETUP),APSU_SKIP_BLENDER=1 $(SHELL_SETUP))

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
	@$(MVN_CMD) javafx:run

## Regenera TODOS os modelos .blend via Python/bpy
generate-characters: setup-assets
	@echo "$(CLR_CYAN)[BAKE ] Re-generating 3D models (.blend) via Blender Python API...$(CLR_RESET)"
	@BLENDER_BIN="$(BLENDER_BIN)" $(PYTHON_CMD) personagens/scripts/gerador_mestre_apsu.py
	@$(MAKE) --no-print-directory export-runtime-models
	@echo "$(CLR_GREEN)[OK] All .blend models re-generated successfully.$(CLR_RESET)"

## Exporta personagens do Blender como malhas OBJ/MTL carregadas no jogo
export-runtime-models: setup-assets generate-geyser-model
	@echo "$(CLR_CYAN)[3D   ] Exporting runtime character meshes from Blender...$(CLR_RESET)"
	@$(BLENDER_BIN) --background --python tools/assets/export_runtime_3d_models.py -- "$(CURDIR)"
	@echo "$(CLR_GREEN)[OK] Runtime 3D character models exported.$(CLR_RESET)"

## Gera o modelo low-poly 3D do gêiser para o pipeline de runtime
generate-geyser-model: setup-assets
	@$(BLENDER_BIN) --background --python tools/assets/generate_geyser_3d.py -- "$(CURDIR)"

## Renderiza PNGs 2.5D do cenário (personagens são modelos 3D de runtime)
render-sprites: setup-assets
	@echo "$(CLR_CYAN)[BAKE ] Baking 2.5D scenery PNGs using Blender...$(CLR_RESET)"
	@$(BLENDER_BIN) --background --python tools/assets/render_all_2d5_sprites.py
	@echo "$(CLR_GREEN)[OK] 2.5D scenery rendering complete.$(CLR_RESET)"

## Gera poses de ataque das variantes
generate-variant-attacks: setup-assets
	@echo "$(CLR_CYAN)[BAKE ] Generating variant attack poses (.blend)...$(CLR_RESET)"
	@$(BLENDER_BIN) --background --python personagens/scripts/01_adapa_variacoes_ataque.py
	@echo "$(CLR_GREEN)[OK] Variant attack poses generated.$(CLR_RESET)"

## Gera renders PNG de preview das poses de ataque (não usados no jogo)
render-variant-attacks: setup
	@echo "$(CLR_CYAN)[BAKE ] Baking variant attack sprites...$(CLR_RESET)"
	@$(BLENDER_BIN) --background --python tools/assets/render_all_2d5_sprites.py -- --only 01_adapa_var_abissal_ataque_thrust,01_adapa_var_abissal_ataque_slash,01_adapa_var_abissal_ataque_spin,01_adapa_var_abissal_ataque_charge,01_adapa_var_deus_dourado_ataque_thrust,01_adapa_var_deus_dourado_ataque_slash,01_adapa_var_deus_dourado_ataque_spin,01_adapa_var_deus_dourado_ataque_charge,01_adapa_var_recife_ataque_thrust,01_adapa_var_recife_ataque_slash,01_adapa_var_recife_ataque_spin,01_adapa_var_recife_ataque_charge
	@echo "$(CLR_GREEN)[OK] Variant attack preview renders baked.$(CLR_RESET)"

## Pipeline completo: modelos de personagens 3D runtime + cenários/PNGs 2.5D
assets: generate-characters render-sprites copy-blends clean-blender-backups
	@echo "$(CLR_GREEN)[OK] Full 3D character + 2.5D scenery asset pipeline completed.$(CLR_RESET)"

## Pipeline rápido de variantes
assets-variant-attacks: generate-variant-attacks export-runtime-models copy-blends
	@echo "$(CLR_GREEN)[OK] Fast 3D variant attack models exported for runtime.$(CLR_RESET)"

## Gera Fat JAR executável
package: build
	@echo "$(CLR_CYAN)[PACK ] Building Fat JAR package in target/...$(CLR_RESET)"
	@$(MVN_CMD) package -q -DskipTests
	@echo "$(CLR_GREEN)[OK] Executable JAR created: target/apsu-game-1.0.0-jar-with-dependencies.jar$(CLR_RESET)"

## Constrói a imagem Docker
docker-build:
	@echo "$(CLR_CYAN)[DOCK ] Building Docker image $(APP_NAME):$(VERSION)...$(CLR_RESET)"
	@docker build -t $(APP_NAME):$(VERSION) .

## Executa container Docker com X11 forwarding
docker-run: docker-build
	@echo "$(CLR_CYAN)[DOCK ] Running game in Docker container with X11 forwarding...$(CLR_RESET)"
	@command -v xhost >/dev/null 2>&1 && xhost +local:docker > /dev/null 2>&1 || true
	@DRI_FLAGS=$$( [ -d /dev/dri ] && echo "--device /dev/dri:/dev/dri" || echo "" ); \
	docker run --rm --name $(APP_NAME) -e DISPLAY=$${DISPLAY:-:0} -e LIBGL_ALWAYS_SOFTWARE=$${LIBGL_ALWAYS_SOFTWARE:-1} \
	-v $${X11_SOCKET_DIR:-/tmp/.X11-unix}:/tmp/.X11-unix:rw $$DRI_FLAGS $(APP_NAME):$(VERSION)

## Encerra containers Docker
stop:
	@echo "$(CLR_YELLOW)[STOP ] Stopping active Docker containers...$(CLR_RESET)"
	@docker ps -q --filter ancestor=$(APP_NAME):$(VERSION) | xargs -r docker stop 2>/dev/null || true
	@docker ps -q --filter name=$(APP_NAME) | xargs -r docker stop 2>/dev/null || true
	@echo "$(CLR_GREEN)[OK] Docker containers stopped.$(CLR_RESET)"

## Executa a demonstração MPI paralela em C
mpi-demo: setup-mpi
	@echo "$(CLR_CYAN)[MPI  ] Compiling and running parallel C map generator...$(CLR_RESET)"
	@mkdir -p $(MPI_DIR)
	@command -v mpicc >/dev/null || { echo "[ERROR] mpicc (OpenMPI) is required for mpi-demo."; exit 1; }
	@command -v mpirun >/dev/null || { echo "[ERROR] mpirun (OpenMPI) is required for mpi-demo."; exit 1; }
	@mpicc -O2 -Wall -Wextra -o $(MPI_DIR)/map_gen $(MPI_DIR)/src/MapGenerator.c
	@mpirun --oversubscribe -np $(MPI_PROCESSES) $(MPI_DIR)/map_gen --phase 1

## Gera os 5 layouts JSON via MPI
mpi-generate: setup-mpi
	@echo "$(CLR_CYAN)[MPI  ] Generating 5 phase layouts via MPI...$(CLR_RESET)"
	@mkdir -p $(MPI_DIR)/generated
	@command -v mpicc >/dev/null || { echo "[ERROR] mpicc (OpenMPI) is required for mpi-generate."; exit 1; }
	@command -v mpirun >/dev/null || { echo "[ERROR] mpirun (OpenMPI) is required for mpi-generate."; exit 1; }
	@mpicc -O2 -Wall -Wextra -o $(MPI_DIR)/map_gen $(MPI_DIR)/src/MapGenerator.c
	@set -eu; for phase in 1 2 3 4 5; do \
		tmp=$(MPI_DIR)/generated/phase-$$phase.json.tmp; \
		mpirun --oversubscribe -np $(MPI_PROCESSES) $(MPI_DIR)/map_gen --phase $$phase --output $$tmp; \
		test -s $$tmp; \
		mv $$tmp $(MPI_DIR)/generated/phase-$$phase.json; \
	done
	@echo "$(CLR_GREEN)[OK] MPI JSON phase layouts generated in $(MPI_DIR)/generated/$(CLR_RESET)"

## Limpa artefatos temporários
clean: stop
	@echo "$(CLR_YELLOW)[CLEAN] Removing build directory (target/)...$(CLR_RESET)"
	@$(RM_DIR) $(TARGET_DIR) 2>/dev/null || true
	@rm -f $(MPI_DIR)/map_gen $(MPI_DIR)/*.o 2>/dev/null || true
	@echo "$(CLR_GREEN)[OK] Project clean completed.$(CLR_RESET)"

## Exibe o menu de ajuda estilo fliperama retro arcade
help:
	@echo ""
	@echo "$(CLR_HEADER)+-----------------------------------------------------------------------+$(CLR_RESET)"
	@echo "$(CLR_HEADER)|   [ARCADE ENGINE 2026] -- AS AGUAS DE APSU // RETRO GAME SYSTEM       |$(CLR_RESET)"
	@echo "$(CLR_HEADER)+-----------------------------------------------------------------------+$(CLR_RESET)"
	@echo "$(CLR_WHITE)| COMMAND                      | DESCRIPTION                            |$(CLR_RESET)"
	@echo "$(CLR_DIM)+------------------------------+----------------------------------------+$(CLR_RESET)"
	@echo "  $(CLR_GREEN)make / make all$(CLR_RESET)              Run setup, organize assets & start game"
	@echo "  $(CLR_GREEN)make run$(CLR_RESET)                     Start JavaFX 21 main game application"
	@echo "  $(CLR_GREEN)make setup$(CLR_RESET)                   Check & install dependencies (idempotent)"
	@echo "  $(CLR_GREEN)make build$(CLR_RESET)                   Compile Java 21 classes"
	@echo "  $(CLR_GREEN)make test$(CLR_RESET)                    Execute JUnit 5 test suite (45 tests)"
	@echo "  $(CLR_CYAN)make assets$(CLR_RESET)                  Full 3D character + 2.5D scenery pipeline"
	@echo "  $(CLR_CYAN)make assets-variant-attacks$(CLR_RESET)  Export 3D attack poses for runtime"
	@echo "  $(CLR_CYAN)make generate-characters$(CLR_RESET)   Re-generate .blend and runtime models"
	@echo "  $(CLR_CYAN)make render-sprites$(CLR_RESET)        Render 2.5D scenery PNGs from .blend"
	@echo "  $(CLR_CYAN)make copy-blends$(CLR_RESET)           Organize .blend 3D models into assets/"
	@echo "  $(CLR_CYAN)make package$(CLR_RESET)               Build executable Fat JAR in target/"
	@echo "  $(CLR_YELLOW)make docker-run$(CLR_RESET)           Run inside Docker container (Linux)"
	@echo "  $(CLR_YELLOW)make mpi-demo$(CLR_RESET)             Run parallel MPI map generator"
	@echo "  $(CLR_YELLOW)make mpi-generate$(CLR_RESET)         Generate 5 JSON phase layouts via MPI"
	@echo "  $(CLR_RED)make clean$(CLR_RESET)                Clean build artifacts and release RAM"
	@echo "$(CLR_DIM)+-----------------------------------------------------------------------+$(CLR_RESET)"
	@echo "$(CLR_HEADER)| SYSTEM DIAGNOSTICS                                                    |$(CLR_RESET)"
	@echo "$(CLR_DIM)+-----------------------------------------------------------------------+$(CLR_RESET)"
	@echo "  $(CLR_CYAN)OPERATING SYSTEM$(CLR_RESET) : $(CLR_WHITE)$(SO_NAME)$(CLR_RESET)"
	@echo "  $(CLR_CYAN)HARDWARE SPECS  $(CLR_RESET) : $(CLR_WHITE)$(CPU_CORES) Cores / ~$(RAM_GB)GB RAM$(CLR_RESET)"
	@echo "  $(CLR_CYAN)BLENDER BINARY  $(CLR_RESET) : $(CLR_WHITE)$(BLENDER_BIN)$(CLR_RESET)"
	@echo "$(CLR_HEADER)+-----------------------------------------------------------------------+$(CLR_RESET)"
	@echo ""
