package br.apsu.audio;

import br.apsu.model.environment.OceanDepthProfile;

/** Porta pequena para manter eventos de jogo testáveis sem iniciar JavaFX Media. */
public interface AudioPort {
    void playSound(AudioCue cue);
    void setDepth(OceanDepthProfile profile);
    void setBossMode(boolean active);
}
