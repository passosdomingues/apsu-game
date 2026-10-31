#!/usr/bin/env python3
"""Convert the four supplied phase MP3s to runtime PCM WAV resources."""
from pathlib import Path
import os
import shutil
import subprocess
import sys
import tempfile

ROOT = Path(__file__).resolve().parents[2]
AUDIO = ROOT / "src/main/resources/audio"
DESTINATIONS = {
    1: "coastal",
    2: "deep-reef",
    3: "abyssal-plain",
    4: "hydrothermal-vent",
}


def convert(ffmpeg: str, source: Path, destination: Path) -> None:
    destination.parent.mkdir(parents=True, exist_ok=True)
    fd, temporary_name = tempfile.mkstemp(prefix=f".{destination.stem}-", suffix=".wav", dir=destination.parent)
    os.close(fd)
    try:
        subprocess.run([
            ffmpeg, "-hide_banner", "-loglevel", "error", "-y", "-i", str(source),
            "-map", "0:a:0", "-map_metadata", "-1", "-c:a", "pcm_s16le",
            "-ar", "44100", "-ac", "2", temporary_name,
        ], check=True)
        Path(temporary_name).replace(destination)
    except (subprocess.CalledProcessError, OSError):
        Path(temporary_name).unlink(missing_ok=True)
        raise
    finally:
        Path(temporary_name).unlink(missing_ok=True)


def main() -> int:
    ffmpeg = shutil.which("ffmpeg")
    if not ffmpeg:
        print("FFmpeg não encontrado no PATH.", file=sys.stderr)
        return 2
    sources = {phase: AUDIO / f"fase{phase}.mpeg" for phase in DESTINATIONS}
    missing = [path.name for path in sources.values() if not path.is_file()]
    if missing:
        print("Músicas de fase ausentes: " + ", ".join(missing), file=sys.stderr)
        return 2

    for phase, profile in DESTINATIONS.items():
        destination = AUDIO / "generated" / profile / f"fase{phase}.wav"
        convert(ffmpeg, sources[phase], destination)
        print(f"fase {phase}: {destination.relative_to(ROOT)}")

    shutil.copy2(AUDIO / "generated/coastal/fase1.wav", AUDIO / "apsu-theme.wav")
    print("Faixa costeira atualizada em audio/apsu-theme.wav (fallback).")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
