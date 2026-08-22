#!/usr/bin/env bash
# Executa o Blender em background para transformar um modelo/cenario .blend
# em PNGs que o jogo 2D carrega diretamente.
set -euo pipefail
[[ $# -eq 2 ]] || { echo "Uso: $0 <arquivo.blend> <asset>" >&2; exit 2; }
blend=$1
asset=$2
[[ -f "$blend" ]] || { echo "Arquivo nao encontrado: $blend" >&2; exit 2; }
blender_bin=${BLENDER_BIN:-}
if [[ -z "$blender_bin" ]]; then
  blender_bin=$(command -v blender || true)
fi
if [[ -z "$blender_bin" || ! -x "$blender_bin" ]]; then
  for candidate in "/opt/blender-4.5.5-lts/blender" "/usr/bin/blender"; do
    if [[ -x "$candidate" ]]; then
      blender_bin="$candidate"
      break
    fi
  done
fi
[[ -n "$blender_bin" && -x "$blender_bin" ]] || {
  echo "Blender nao encontrado. Defina BLENDER_BIN=/caminho/para/blender." >&2
  exit 3
}
root=$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)
out="$root/src/main/resources/sprites/$asset"
mkdir -p "$out"
threads=${BLENDER_THREADS:-$(nproc)}
resolution=${BLENDER_RESOLUTION:-192}
samples=${BLENDER_SAMPLES:-8}
directions=${BLENDER_DIRECTIONS:-1}
direction_start=${BLENDER_DIRECTION_START:-0}
direction_step=${BLENDER_DIRECTION_STEP:-1}
echo "Renderizando com $threads threads, ${resolution}px, $samples amostras; direcoes $direction_start/$directions (passo $direction_step)..."
LIBGL_ALWAYS_SOFTWARE=1 EGL_PLATFORM=surfaceless "$blender_bin" -b "$blend" -P "$root/tools/blender_render_sprites.py" -- "$out" "$threads" "$resolution" "$samples" "$directions" "$direction_start" "$direction_step"
echo "Sprites exportados em $out"
