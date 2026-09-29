package br.apsu.audio;

import br.apsu.model.environment.OceanDepthProfile;

/** Backend específico de reprodução, isolado da política de seleção por fase. */
interface AudioOutput {
    void playEffect(AudioCue cue, OceanDepthProfile depth);
    void playMusic(OceanDepthProfile depth, boolean bossMode);
    void changeMusic(OceanDepthProfile depth, boolean bossMode, boolean resume);
    void stopMusic();
}
