package br.apsu.audio;

import br.apsu.model.environment.OceanDepthProfile;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class AudioManagerTest {
    @Test
    void effectsUseCurrentDepthAndMusicTracksDepthBossAndPlayingState() {
        RecordingOutput output = new RecordingOutput();
        AudioManager manager = new AudioManager(output, true);

        manager.playSound(AudioCue.ATTACK);
        manager.playSound((AudioCue) null);
        manager.setDepth(null);
        manager.setDepth(OceanDepthProfile.HADAL_TRENCH);
        manager.setBossMode(true);
        assertTrue(output.music.isEmpty(), "Perfil muda sem iniciar áudio antes de playMusic");

        manager.playMusic();
        manager.setDepth(OceanDepthProfile.ABYSSAL_PLAIN);
        manager.setDepth(OceanDepthProfile.ABYSSAL_PLAIN); // mudança idempotente
        manager.setBossMode(false);
        manager.setBossMode(false); // mudança idempotente
        manager.stopMusic();
        manager.stopMusic();

        assertEquals(List.of("ATTACK@COASTAL"), output.effects);
        assertEquals(List.of("play@HADAL_TRENCH/boss", "change@ABYSSAL_PLAIN/boss",
            "change@ABYSSAL_PLAIN/ocean"), output.music);
        assertEquals(1, output.stops);
    }

    @Test
    void disabledAudioIsInert() {
        RecordingOutput output = new RecordingOutput();
        AudioManager manager = new AudioManager(output, true);
        manager.playSound(AudioCue.ATTACK);
        assertEquals(List.of("ATTACK@COASTAL"), output.effects);

        RecordingOutput disabledOutput = new RecordingOutput();
        AudioManager disabled = new AudioManager(disabledOutput, false);
        disabled.playSound(AudioCue.VICTORY);
        disabled.setDepth(OceanDepthProfile.HADAL_TRENCH);
        disabled.setBossMode(true);
        disabled.playMusic();
        disabled.stopMusic();
        assertTrue(disabledOutput.effects.isEmpty());
        assertTrue(disabledOutput.music.isEmpty());
        assertEquals(0, disabledOutput.stops);
    }

    @Test
    void javafxBackendCanBeDisabledWithoutInitializingMediaOrGraphics() {
        JavaFxAudioOutput output = new JavaFxAudioOutput(false);
        output.playEffect(AudioCue.PICKUP, OceanDepthProfile.COASTAL);
        output.playMusic(OceanDepthProfile.COASTAL, false);
        output.changeMusic(OceanDepthProfile.HADAL_TRENCH, true, true);
        output.stopMusic();
    }

    private static final class RecordingOutput implements AudioOutput {
        private final List<String> effects = new ArrayList<>();
        private final List<String> music = new ArrayList<>();
        private int stops;
        @Override public void playEffect(AudioCue cue, OceanDepthProfile depth) {
            effects.add(cue.name() + "@" + depth.name());
        }
        @Override public void playMusic(OceanDepthProfile depth, boolean bossMode) {
            music.add("play@" + depth.name() + "/" + (bossMode ? "boss" : "ocean"));
        }
        @Override public void changeMusic(OceanDepthProfile depth, boolean bossMode, boolean resume) {
            music.add("change@" + depth.name() + "/" + (bossMode ? "boss" : "ocean"));
        }
        @Override public void stopMusic() { stops++; }
    }
}
