package br.apsu.audio;

import br.apsu.core.events.EventBus;
import br.apsu.core.events.GameEvent;
import br.apsu.model.environment.OceanDepthProfile;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class AudioEventSubscriberTest {
    @Test
    void routesSemanticAudioDepthAndBossEventsAndUnsubscribesAtomically() throws Exception {
        EventBus bus = new EventBus();
        RecordingAudio audio = new RecordingAudio();
        AutoCloseable subscription = AudioEventSubscriber.bind(bus, audio);

        bus.publish(new GameEvent(GameEvent.Type.SOUND_REQUESTED, AudioCue.HERO_DAMAGED, 0, 0));
        bus.publish(new GameEvent(GameEvent.Type.SOUND_REQUESTED, "victory", 0, 0));
        bus.publish(new GameEvent(GameEvent.Type.SOUND_REQUESTED, new Object(), 0, 0));
        bus.publish(new GameEvent(GameEvent.Type.OCEAN_DEPTH_CHANGED, OceanDepthProfile.HADAL_TRENCH, 0, 0));
        bus.publish(new GameEvent(GameEvent.Type.BOSS_MUSIC_CHANGED, true, 0, 0));
        bus.publish(new GameEvent(GameEvent.Type.BOSS_PHASE_CHANGED, 2, 0, 0));

        assertEquals(List.of(AudioCue.HERO_DAMAGED, AudioCue.BOSS_PHASE), audio.cues);
        assertEquals(List.of(OceanDepthProfile.HADAL_TRENCH), audio.depths);
        assertEquals(List.of(true), audio.bossModes);

        subscription.close();
        bus.publish(new GameEvent(GameEvent.Type.SOUND_REQUESTED, AudioCue.VICTORY, 0, 0));
        bus.publish(new GameEvent(GameEvent.Type.OCEAN_DEPTH_CHANGED, OceanDepthProfile.COASTAL, 0, 0));
        bus.publish(new GameEvent(GameEvent.Type.BOSS_MUSIC_CHANGED, false, 0, 0));
        assertEquals(2, audio.cues.size());
        assertEquals(1, audio.depths.size());
        assertEquals(1, audio.bossModes.size());
    }

    private static final class RecordingAudio implements AudioPort {
        private final List<AudioCue> cues = new ArrayList<>();
        private final List<OceanDepthProfile> depths = new ArrayList<>();
        private final List<Boolean> bossModes = new ArrayList<>();
        @Override public void playSound(AudioCue cue) { cues.add(cue); }
        @Override public void setDepth(OceanDepthProfile profile) { depths.add(profile); }
        @Override public void setBossMode(boolean active) { bossModes.add(active); }
    }
}
