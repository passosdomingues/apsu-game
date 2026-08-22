#!/usr/bin/env bash
# Diagnostico sem efeitos colaterais para a pipeline local/remota Hunyuan3D.
set -euo pipefail
api_url=${HUNYUAN3D_URL:-http://127.0.0.1:8080}
threads=$(nproc)
available_mib=$(awk '/MemAvailable:/ {print int($2 / 1024)}' /proc/meminfo)

echo "CPUs logicos disponiveis: $threads"
echo "RAM disponivel: ${available_mib} MiB"
if command -v nvidia-smi >/dev/null; then
  nvidia-smi --query-gpu=name,memory.total --format=csv,noheader
else
  echo "GPU CUDA: nao detectada"
fi
if curl -fsS --max-time 2 "$api_url/health" >/dev/null; then
  echo "API Hunyuan3D: disponivel em $api_url"
else
  echo "API Hunyuan3D: indisponivel em $api_url"
fi
echo "Nota: use um unico processo de inferencia com OMP_NUM_THREADS=$threads;"
echo "varios processos duplicariam os pesos do modelo na RAM."
