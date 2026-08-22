#!/usr/bin/env bash
# Um worker MPI renderiza apenas as direcoes que lhe pertencem. Cada arquivo
# sai com nome distinto, portanto os workers podem escrever simultaneamente.
set -euo pipefail
[[ $# -eq 2 ]] || { echo "Uso: $0 <arquivo.blend> <asset>" >&2; exit 2; }
rank=${OMPI_COMM_WORLD_RANK:-0}
size=${OMPI_COMM_WORLD_SIZE:-1}
echo "[MPI worker $rank/$size] direcoes $rank,$((rank + size)),..."
BLENDER_DIRECTION_START="$rank" \
BLENDER_DIRECTION_STEP="$size" \
tools/render_blender_sprites.sh "$1" "$2"
