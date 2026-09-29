package br.apsu.audio;

import br.apsu.model.environment.OceanDepthProfile;

/** Política da trilha e dos efeitos; não depende da implementação JavaFX. */
public final class AudioManager implements AudioPort {
    private static final boolean AUDIO_ENABLED = Boolean.parseBoolean(
        System.getProperty("apsu.audio.enabled", "true"));
    private static final AudioManager INSTANCE = new AudioManager(
        new JavaFxAudioOutput(AUDIO_ENABLED), AUDIO_ENABLED);

    private final AudioOutput output;
    private final boolean enabled;
    private OceanDepthProfile depth = OceanDepthProfile.COASTAL;
    private boolean bossMode;
    private boolean musicPlaying;

    AudioManager(AudioOutput output, boolean enabled) {
        this.output = output;
        this.enabled = enabled;
    }

    public static AudioManager getInstance() { return INSTANCE; }

    @Override public synchronized void playSound(AudioCue cue) {
        if (!enabled || cue == null) return;
        output.playEffect(cue, depth);
    }

    @Override public synchronized void setDepth(OceanDepthProfile next) {
        if (!enabled || next == null || next == depth) return;
        depth = next;
        if (musicPlaying) output.changeMusic(depth, bossMode, true);
    }

    @Override public synchronized void setBossMode(boolean active) {
        if (!enabled || bossMode == active) return;
        bossMode = active;
        if (musicPlaying) output.changeMusic(depth, bossMode, true);
    }

    public synchronized void playMusic() {
        if (!enabled) return;
        musicPlaying = true;
        output.playMusic(depth, bossMode);
    }

    public synchronized void stopMusic() {
        if (!enabled || !musicPlaying) return;
        musicPlaying = false;
        output.stopMusic();
    }
}
