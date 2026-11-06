#!/usr/bin/env bash
set -euo pipefail

release_dir="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
system="$(uname -s)"
machine="$(uname -m)"

case "$system:$machine" in
  Linux:x86_64|Linux:amd64) platform=linux-x64; api_os=linux; api_arch=x64 ;;
  Darwin:x86_64) platform=macos-x64; api_os=mac; api_arch=x64 ;;
  Darwin:arm64|Darwin:aarch64) platform=macos-arm64; api_os=mac; api_arch=aarch64 ;;
  *) echo "Sistema/arquitetura não cobertos por este launcher: $system $machine" >&2; exit 1 ;;
esac

shopt -s nullglob
jars=("$release_dir"/apsu-game-v*-"$platform".jar)
resources=("$release_dir"/apsu-game-v*-resources.zip)
if ((${#jars[@]} != 1 || ${#resources[@]} != 1)); then
  echo "Coloque nesta pasta o JAR de $platform e o arquivo de recursos da mesma release." >&2
  exit 1
fi

java_major() {
  local version
  version="$("$1" -version 2>&1 | sed -n '1s/.*version "\([0-9]*\).*/\1/p')"
  [[ "$version" =~ ^[0-9]+$ ]] || return 1
  printf '%s' "$version"
}

JVM_FLAGS=(-XX:+UseG1GC -XX:MaxGCPauseMillis=5 -Dprism.order=es2,sw -Dprism.vsync=true)

system_java="$(command -v java || true)"
if [[ -n "$system_java" ]]; then
  major="$(java_major "$system_java" || true)"
  if [[ -n "$major" && "$major" -ge 21 ]]; then
    exec "$system_java" "${JVM_FLAGS[@]}" -cp "$release_dir/${jars[0]##*/}:$release_dir/${resources[0]##*/}" br.apsu.Launcher
  fi
fi

if [[ "$system" == Linux ]]; then
  runtime_root="${XDG_DATA_HOME:-$HOME/.local/share}/apsu-game/runtimes/$platform-temurin-21"
else
  runtime_root="$HOME/Library/Application Support/ApsuGame/runtimes/$platform-temurin-21"
fi
runtime_java="$(find "$runtime_root" -type f -path '*/bin/java' -print -quit 2>/dev/null || true)"
runtime_major="$(java_major "$runtime_java" 2>/dev/null || true)"
if [[ -n "$runtime_java" && -n "$runtime_major" && "$runtime_major" -ge 21 ]]; then
  exec "$runtime_java" "${JVM_FLAGS[@]}" -cp "$release_dir/${jars[0]##*/}:$release_dir/${resources[0]##*/}" br.apsu.Launcher
fi

command -v curl >/dev/null 2>&1 || { echo "Instale curl e tente novamente." >&2; exit 1; }
command -v tar >/dev/null 2>&1 || { echo "Instale tar e tente novamente." >&2; exit 1; }
tmp="$(mktemp -d)"
trap 'rm -rf "$tmp"' EXIT
url="https://api.adoptium.net/v3/binary/latest/21/ga/$api_os/$api_arch/jre/hotspot/normal/eclipse"
echo "Baixando Java 21 uma única vez para sua conta de usuário..."
curl -fL --retry 2 "$url" -o "$tmp/temurin.tar.gz"
mkdir -p "$runtime_root"
tar -xzf "$tmp/temurin.tar.gz" -C "$runtime_root"
runtime_java="$(find "$runtime_root" -type f -path '*/bin/java' -print -quit)"
if [[ -z "$runtime_java" ]]; then
  echo "Não foi possível localizar o Java 21 baixado." >&2
  exit 1
fi
exec "$runtime_java" "${JVM_FLAGS[@]}" -cp "$release_dir/${jars[0]##*/}:$release_dir/${resources[0]##*/}" br.apsu.Launcher
