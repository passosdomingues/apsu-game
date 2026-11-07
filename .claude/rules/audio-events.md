---
paths:
  - "tools/audio/**"
  - "docs/audio/**"
  - "src/main/java/**/audio/**"
---
# Regras para áudio e eventos

- Cues são semânticos (AudioCue em UPPER_SNAKE_CASE, nomeando ação ou resultado). Nome de arquivo só em JavaFxAudioOutput.
- Cada evento novo registra produtor, payload, instante, cooldown, prioridade, consumidores e teste.
- Fontes e licenças de áudio novo entram em docs/audio/SOURCES.md.
- FFmpeg roda offline por `make audio-assets`; nunca no loop do jogo.
- Efeitos curtos usam AudioClip; trilhas longas usam MediaPlayer em loop com WAV PCM 16-bit 44,1 kHz.
