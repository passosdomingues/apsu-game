#!/usr/bin/env python3
"""Render deterministic, depth-shaped game audio variants with FFmpeg."""
from pathlib import Path
import os
import shutil
import subprocess
import sys
import tempfile

ROOT = Path(__file__).resolve().parents[2]
AUDIO = ROOT / "src/main/resources/audio"
PROFILES = {
    "coastal": (12000, "28|56", "0.16|0.07", 0.28, 0.035),
    "deep-reef": (9000, "38|76", "0.18|0.08", 0.25, 0.060),
    "abyssal-plain": (6500, "54|108", "0.20|0.09", 0.22, 0.090),
    "hydrothermal-vent": (4600, "72|144", "0.20|0.10", 0.19, 0.125),
    "hadal-trench": (3200, "92|184", "0.22|0.11", 0.16, 0.160),
}
SOURCES = ("apsu-theme", "shoot", "collect", "hurt", "boss-hit", "victory",
           "menu-navigate", "menu-confirm", "enemy-damaged")
SOURCE_DIR = AUDIO / "sources"


def create_missing_cue_sources(ffmpeg: str) -> None:
    """Create replaceable synthesized cues only when an artist source is absent."""
    cues = {
        "menu-navigate": ("sine=frequency=880:duration=0.09", "afade=t=out:st=0.035:d=0.055,highpass=f=250,volume=0.35"),
        "menu-confirm": ("sine=frequency=660:duration=0.19", "afade=t=out:st=0.10:d=0.09,highpass=f=180,volume=0.48"),
        "enemy-damaged": ("anoisesrc=color=pink:duration=0.14:amplitude=0.7", "lowpass=f=1800,highpass=f=140,afade=t=out:st=0.035:d=0.105,volume=0.52"),
    }
    for name, (source, filters) in cues.items():
        output = AUDIO / f"{name}.wav"
        if output.is_file():
            continue
        subprocess.run([
            ffmpeg, "-hide_banner", "-loglevel", "error", "-y", "-f", "lavfi",
            "-i", source, "-af", filters, "-ar", "22050", "-ac", "2",
            "-c:a", "pcm_s16le", str(output),
        ], check=True)
        print(f"Fonte sintetizada (substituível): {output.name}")


def main() -> int:
    ffmpeg = shutil.which("ffmpeg")
    if not ffmpeg:
        print("FFmpeg não encontrado no PATH. Instale FFmpeg para renderizar os perfis.", file=sys.stderr)
        return 2
    create_missing_cue_sources(ffmpeg)
    missing = [name for name in SOURCES if not (AUDIO / f"{name}.wav").is_file()]
    if missing:
        print("Áudios-fonte ausentes: " + ", ".join(missing), file=sys.stderr)
        return 2

    ambient_pad = SOURCE_DIR / "underwater-ambient-pad.ogg"
    deep_rumble = SOURCE_DIR / "deep-rumble.ogg"
    boss_source = SOURCE_DIR / "boss-approach.ogg"
    missing_layers = [path.name for path in (ambient_pad, deep_rumble, boss_source) if not path.is_file()]
    if missing_layers:
        print("Camadas licenciadas ausentes: " + ", ".join(missing_layers), file=sys.stderr)
        return 2

    for profile, (cutoff, delays, decays, pad_gain, rumble_gain) in PROFILES.items():
        destination = AUDIO / "generated" / profile
        destination.mkdir(parents=True, exist_ok=True)
        for name in SOURCES:
            source = AUDIO / f"{name}.wav"
            fd, temporary = tempfile.mkstemp(prefix=f".{name}-", suffix=".wav", dir=destination)
            os.close(fd)
            try:
                # Mais profundidade: menos agudos e reflexões mais tardias/baixas.
                echo = f"aecho=0.88:0.24:{delays}:{decays}"
                shape = f"lowpass=f={cutoff}:poles=2,{echo},alimiter=limit=0.96,loudnorm=I=-20:LRA=10:TP=-1.5"
                command = [ffmpeg, "-hide_banner", "-loglevel", "error", "-y", "-i", str(source)]
                if name == "apsu-theme":
                    command.extend(["-stream_loop", "-1", "-i", str(ambient_pad),
                                    "-stream_loop", "-1", "-i", str(deep_rumble),
                                    "-filter_complex",
                                    f"[0:a]volume=0.88[theme];[1:a]volume={pad_gain}[pad];"
                                    f"[2:a]volume={rumble_gain}[rumble];"
                                    f"[theme][pad][rumble]amix=inputs=3:duration=first:normalize=0[m];[m]{shape}[out]",
                                    "-map", "[out]"])
                else:
                    command.extend(["-af", shape])
                command.extend(["-ar", "22050", "-ac", "2", "-c:a", "pcm_s16le", temporary])
                subprocess.run(command, check=True)
                os.replace(temporary, destination / f"{name}.wav")
                print(f"{profile:20} {name}.wav")
            except (subprocess.CalledProcessError, OSError) as exc:
                Path(temporary).unlink(missing_ok=True)
                print(f"Falha ao renderizar {profile}/{name}: {exc}", file=sys.stderr)
                return 1

        # Música de boss é uma faixa separada: mantém tensão e ganha corpo grave
        # conforme a profundidade, sem trocar assets ou fazer DSP durante o jogo.
        boss_dir = AUDIO / "generated" / "boss"
        boss_dir.mkdir(parents=True, exist_ok=True)
        fd, temporary = tempfile.mkstemp(prefix=f".{profile}-boss-", suffix=".wav", dir=boss_dir)
        os.close(fd)
        try:
            boss_shape = f"lowpass=f={cutoff}:poles=2,{echo},alimiter=limit=0.96,loudnorm=I=-20:LRA=10:TP=-1.5"
            subprocess.run([
                ffmpeg, "-hide_banner", "-loglevel", "error", "-y", "-i", str(boss_source),
                "-stream_loop", "-1", "-i", str(deep_rumble),
                "-filter_complex",
                f"[0:a]volume=0.90[combat];[1:a]volume={rumble_gain * 1.35}[rumble];"
                f"[combat][rumble]amix=inputs=2:duration=first:normalize=0[m];[m]{boss_shape}[out]",
                "-map", "[out]", "-ar", "22050", "-ac", "2", "-c:a", "pcm_s16le", temporary,
            ], check=True)
            os.replace(temporary, boss_dir / f"{profile}.wav")
            print(f"{profile:20} boss.wav")
        except (subprocess.CalledProcessError, OSError) as exc:
            Path(temporary).unlink(missing_ok=True)
            print(f"Falha ao renderizar {profile}/boss: {exc}", file=sys.stderr)
            return 1
    print(f"Áudio pronto em {AUDIO / 'generated'}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
