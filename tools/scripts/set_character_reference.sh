#!/usr/bin/env bash
# Define a imagem-fonte atual sem apagar geracoes anteriores.
set -euo pipefail
[[ $# -eq 2 ]] || { echo "Uso: $0 <nome-personagem> <imagem.png>" >&2; exit 2; }
name=$1
input=$2
[[ -f "$input" ]] || { echo "Imagem nao encontrada: $input" >&2; exit 2; }
root=$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)
dest="$root/assets/characters/$name/reference.png"
mkdir -p "$(dirname "$dest")"
cp "$input" "$dest"
echo "Referencia ativa: $dest"
