#!/usr/bin/env bash
# Sons temporarios autorais, sintetizados localmente. Podem ser substituidos
# mantendo os mesmos nomes em src/main/resources/audio/.
set -euo pipefail
command -v ffmpeg >/dev/null || { echo "ffmpeg e' necessario" >&2; exit 1; }
root=$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)
out="$root/src/main/resources/audio"
mkdir -p "$out"

tone() {
  local name=$1 frequency=$2 duration=$3 volume=$4
  ffmpeg -y -loglevel error -f lavfi -i "sine=frequency=$frequency:sample_rate=22050:duration=$duration" \
    -af "volume=$volume,afade=t=in:st=0:d=0.01,afade=t=out:st=$(awk "BEGIN { print $duration - 0.04 }"):d=0.04" \
    -ac 1 -ar 22050 "$out/$name.wav"
}

tone shoot 880 0.16 0.32
tone collect 1320 0.32 0.25
tone hurt 110 0.25 0.40
tone boss-hit 220 0.28 0.36
tone victory 660 0.60 0.25

# Ambiente subaquatico discreto em loop (drone + oscilacao lenta, 24 s).
ffmpeg -y -loglevel error -f lavfi -i \
  "aevalsrc=0.10*sin(2*PI*55*t)+0.055*sin(2*PI*(110+4*sin(2*PI*0.09*t))*t)+0.018*sin(2*PI*220*t):s=22050:d=24" \
  -af "afade=t=in:st=0:d=1,afade=t=out:st=22:d=2" -ac 1 -ar 22050 "$out/apsu-theme.wav"
echo "Audio criado em $out"
