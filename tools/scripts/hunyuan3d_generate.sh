#!/usr/bin/env bash
# Gera um GLB a partir de uma imagem usando um servidor Hunyuan3D local.
# O cache e' deterministico: mesma imagem + mesmos parametros = mesmo arquivo.
set -euo pipefail

usage() {
  echo "Uso: $0 <imagem.png> [nome-personagem]"
  echo "Ex.: $0 assets/characters/adapa/reference.png adapa"
}

[[ $# -ge 1 && $# -le 2 ]] || { usage; exit 2; }
input=$1
character=${2:-adapa}
[[ -f "$input" ]] || { echo "Imagem nao encontrada: $input" >&2; exit 2; }

root=$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)
out_dir="$root/assets/characters/$character/generated"
mkdir -p "$out_dir"

# A API oficial aceita uma imagem Base64 e devolve um GLB.  HUNYUAN3D_URL pode
# apontar para outro host sem mudar o projeto; o default e' o servidor local.
api_url=${HUNYUAN3D_URL:-http://127.0.0.1:8080}
texture=${HUNYUAN3D_TEXTURE:-false}
model=${HUNYUAN3D_MODEL:-Hunyuan3D-2.1}
# A inferencia e' um processo unico: mais processos duplicariam pesos enormes.
# O servidor pode usar estes limites de threads quando iniciado no mesmo shell.
export OMP_NUM_THREADS=${HUNYUAN3D_THREADS:-$(nproc)}
export MKL_NUM_THREADS="$OMP_NUM_THREADS"
export OPENBLAS_NUM_THREADS="$OMP_NUM_THREADS"
signature=$( (sha256sum "$input"; printf '\ntexture=%s\nmodel=%s\nseed=42\napi=v1\n' "$texture" "$model") | sha256sum | cut -c1-16 )
output="$out_dir/${character}-${signature}.glb"
latest="$out_dir/${character}.glb"
manifest="$out_dir/${character}-${signature}.json"

if [[ -s "$output" ]]; then
  ln -sfn "$(basename "$output")" "$latest"
  echo "Cache reutilizado: $output"
  exit 0
fi

if ! curl -fsS --max-time 2 "$api_url/health" >/dev/null; then
  cat >&2 <<EOF
Hunyuan3D nao esta acessivel em $api_url.
Inicie-o no checkout oficial, por exemplo:
  python api_server.py --host 127.0.0.1 --port 8080
Depois rode este comando novamente. Ollama nao hospeda Hunyuan3D.
EOF
  exit 3
fi

encoded=$(base64 -w 0 "$input")
tmp="$output.part"
trap 'rm -f "$tmp"' EXIT
payload=$(printf '{"image":"%s","texture":%s,"type":"glb","seed":42}' "$encoded" "$texture")
curl -fsS --max-time 1800 -X POST "$api_url/generate" \
  -H 'Content-Type: application/json' -d "$payload" -o "$tmp"

# GLB inicia com os bytes ASCII glTF. Nao publiquemos uma mensagem de erro HTML
# como se fosse um modelo valido.
[[ $(head -c 4 "$tmp") == "glTF" ]] || { echo "Resposta do Hunyuan3D nao e' um GLB valido." >&2; exit 4; }
mv "$tmp" "$output"
ln -sfn "$(basename "$output")" "$latest"
printf '{\n  "character": "%s",\n  "source": "%s",\n  "signature": "%s",\n  "model": "%s",\n  "texture": %s,\n  "seed": 42\n}\n' \
  "$character" "$input" "$signature" "$model" "$texture" > "$manifest"
echo "Modelo gerado: $output"
