#!/usr/bin/env bash
# ============================================================
# setup_environment.sh — Auto-Instalação Idempotente de Dependências
# Suporta: Debian / Ubuntu / Linux Mint e derivados
# Estilo Terminal Retro: Sem Emojis, Formatação Tabulada
# ============================================================

set -e

CLR_HEADER='\033[1;35m'
CLR_CYAN='\033[1;36m'
CLR_YELLOW='\033[1;33m'
CLR_GREEN='\033[1;32m'
CLR_RED='\033[1;31m'
CLR_RESET='\033[0m'

echo -e "${CLR_HEADER}+-----------------------------------------------------------------------+${CLR_RESET}"
echo -e "${CLR_HEADER}| [SYSTEM SETUP] LINUX DEBIAN/UBUNTU DEPENDENCY ORCHESTRATOR           |${CLR_RESET}"
echo -e "${CLR_HEADER}+-----------------------------------------------------------------------+${CLR_RESET}"

has_cmd() {
    command -v "$1" >/dev/null 2>&1
}

# 1. Verificar JDK 21 LTS
echo -e "${CLR_CYAN}>> [1/5] Checking Java 21 LTS...${CLR_RESET}"
JDK21_FOUND=0
JDK21_PATH=""

if [ -n "$JAVA_HOME" ] && [ -x "$JAVA_HOME/bin/java" ]; then
    VERSION_STR=$("$JAVA_HOME/bin/java" -version 2>&1 | head -n 1)
    if echo "$VERSION_STR" | grep -q '21'; then
        JDK21_FOUND=1
        JDK21_PATH="$JAVA_HOME"
    fi
fi

if [ $JDK21_FOUND -eq 0 ]; then
    for candidate in \
        "/opt/java-temurin-21" \
        "/usr/lib/jvm/java-21-openjdk-amd64" \
        "/usr/lib/jvm/java-21-openjdk" \
        "$HOME/.local/jdk-21"; do
        if [ -x "$candidate/bin/java" ]; then
            VERSION_STR=$("$candidate/bin/java" -version 2>&1 | head -n 1)
            if echo "$VERSION_STR" | grep -q '21'; then
                JDK21_FOUND=1
                JDK21_PATH="$candidate"
                break
            fi
        fi
    done
fi

if [ $JDK21_FOUND -eq 0 ] && has_cmd java; then
    VERSION_STR=$(java -version 2>&1 | head -n 1)
    if echo "$VERSION_STR" | grep -q '21'; then
        JDK21_FOUND=1
        JDK21_PATH=$(dirname $(dirname $(readlink -f $(command -v java))))
    fi
fi

if [ $JDK21_FOUND -eq 1 ]; then
    echo -e "${CLR_GREEN}[OK] JDK 21 LTS found at: ${JDK21_PATH}${CLR_RESET}"
else
    echo -e "${CLR_YELLOW}[WARN] JDK 21 LTS not found. Installing openjdk-21-jdk...${CLR_RESET}"
    if has_cmd apt-get; then
        sudo apt-get update -qq && sudo apt-get install -y openjdk-21-jdk openjdk-21-jre -qq
        echo -e "${CLR_GREEN}[OK] OpenJDK 21 LTS installed via apt-get.${CLR_RESET}"
    else
        echo -e "${CLR_YELLOW}[WARN] apt-get not available. Downloading Eclipse Temurin 21 portable...${CLR_RESET}"
        mkdir -p "$HOME/.local/jdk-21"
        curl -sSL "https://github.com/adoptium/temurin21-binaries/releases/download/jdk-21.0.4%2B7/OpenJDK21U-jdk_x64_linux_hotspot_21.0.4_7.tar.gz" | tar -xz -C "$HOME/.local/jdk-21" --strip-components=1
        echo -e "${CLR_GREEN}[OK] JDK 21 Temurin installed at ~/.local/jdk-21${CLR_RESET}"
    fi
fi

# 2. Verificar Apache Maven
echo -e "${CLR_CYAN}>> [2/5] Checking Apache Maven...${CLR_RESET}"
if has_cmd mvn; then
    echo -e "${CLR_GREEN}[OK] Apache Maven found: $(mvn -v | head -n 1)${CLR_RESET}"
else
    echo -e "${CLR_YELLOW}[WARN] Maven not found. Installing maven via apt...${CLR_RESET}"
    if has_cmd apt-get; then
        sudo apt-get install -y maven -qq
        echo -e "${CLR_GREEN}[OK] Apache Maven installed via apt.${CLR_RESET}"
    else
        echo -e "${CLR_RED}[FAIL] Please install Apache Maven manually.${CLR_RESET}"
    fi
fi

# Blender/OpenMPI are only needed when generating assets/maps. The checked-in
# runtime OBJ/MTL resources let a normal `make` run without these large tools.
if [ "${APSU_RUNTIME_ONLY:-0}" != "1" ] && [ "${APSU_SKIP_BLENDER:-0}" != "1" ]; then
# 3. Verificar Blender 4.5 LTS
echo -e "${CLR_CYAN}>> [3/5] Checking Blender 4.5 LTS...${CLR_RESET}"
BLENDER_FOUND=0
BLENDER_PATH=""

for candidate in \
    $(command -v blender 2>/dev/null) \
    "/opt/blender-4.5.5-lts/blender" \
    "/opt/blender-4.5.0-lts/blender" \
    "/opt/blender-4.5-lts/blender" \
    "$HOME/.local/blender-4.5/blender" \
    "/usr/bin/blender"; do
    if [ -x "$candidate" ]; then
        B_VER=$("$candidate" --version 2>&1 | head -n 1)
        echo -e "${CLR_GREEN}[OK] Blender found at ${candidate}: ${B_VER}${CLR_RESET}"
        BLENDER_FOUND=1
        BLENDER_PATH="$candidate"
        break
    fi
done

if [ $BLENDER_FOUND -eq 0 ]; then
    echo -e "${CLR_YELLOW}[WARN] Blender 4.5 LTS not found. Downloading official portable tarball...${CLR_RESET}"
    INSTALL_DIR="$HOME/.local/blender-4.5"
    mkdir -p "$INSTALL_DIR"
    BLENDER_TAR_URL="https://download.blender.org/release/Blender4.5/blender-4.5.0-linux-x64.tar.xz"
    echo -e "${CLR_CYAN}       Downloading ${BLENDER_TAR_URL} -> ${INSTALL_DIR}...${CLR_RESET}"
    if curl -sSL "$BLENDER_TAR_URL" | tar -xJ -C "$INSTALL_DIR" --strip-components=1; then
        mkdir -p "$HOME/.local/bin"
        ln -sf "$INSTALL_DIR/blender" "$HOME/.local/bin/blender"
        echo -e "${CLR_GREEN}[OK] Blender 4.5 LTS installed at: ${INSTALL_DIR}/blender${CLR_RESET}"
    else
        echo -e "${CLR_YELLOW}[WARN] Download failed. Set BLENDER_BIN manually if needed.${CLR_RESET}"
    fi
fi

fi # Blender generation tools

# 4. Verificar OpenMPI & Ferramentas
if [ "${APSU_RUNTIME_ONLY:-0}" != "1" ] && [ "${APSU_SKIP_MPI:-0}" != "1" ]; then
echo -e "${CLR_CYAN}>> [4/5] Checking OpenMPI & Build Tools...${CLR_RESET}"
if has_cmd mpicc && has_cmd mpirun; then
    echo -e "${CLR_GREEN}[OK] OpenMPI found at: $(command -v mpirun)${CLR_RESET}"
else
    echo -e "${CLR_YELLOW}[WARN] OpenMPI not found. Installing openmpi-bin via apt...${CLR_RESET}"
    if has_cmd apt-get; then
        sudo apt-get install -y build-essential openmpi-bin libopenmpi-dev python3 python3-pip -qq || true
        echo -e "${CLR_GREEN}[OK] OpenMPI installed.${CLR_RESET}"
    fi
fi
fi # OpenMPI tools

# 5. Hardware Diagnostics
echo -e "${CLR_CYAN}>> [5/5] Hardware Diagnostics...${CLR_RESET}"
CPU_CORES=$(nproc 2>/dev/null || echo 4)
RAM_GB=$(free -g 2>/dev/null | awk '/Mem:/ {print $2}' || echo 8)
echo -e "${CLR_GREEN}[OK] Hardware Detected: ${CPU_CORES} CPU Threads | ~${RAM_GB}GB RAM${CLR_RESET}"

echo -e "${CLR_GREEN}+-----------------------------------------------------------------------+${CLR_RESET}"
echo -e "${CLR_GREEN}| [OK] ENVIRONMENT VERIFICATION COMPLETE                               |${CLR_RESET}"
echo -e "${CLR_GREEN}+-----------------------------------------------------------------------+${CLR_RESET}"
