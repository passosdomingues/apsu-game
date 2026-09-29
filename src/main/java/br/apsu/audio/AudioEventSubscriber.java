package br.apsu.audio;

import br.apsu.core.events.EventBus;
import br.apsu.core.events.GameEvent;
import br.apsu.model.environment.OceanDepthProfile;

/** Conecta pedidos de áudio do jogo ao adaptador JavaFX AudioClip. */
public final class AudioEventSubscriber {
    private AudioEventSubscriber() {}

    public static AutoCloseable bind(EventBus eventBus, AudioPort audioManager) {
        AutoCloseable sounds = eventBus.subscribe(GameEvent.Type.SOUND_REQUESTED, event -> {
            if (event.payload() instanceof AudioCue cue) audioManager.playSound(cue);
        });
        AutoCloseable depth = eventBus.subscribe(GameEvent.Type.OCEAN_DEPTH_CHANGED, event -> {
            if (event.payload() instanceof OceanDepthProfile profile) audioManager.setDepth(profile);
        });
        AutoCloseable bossPhases = eventBus.subscribe(GameEvent.Type.BOSS_PHASE_CHANGED,
            event -> audioManager.playSound(AudioCue.BOSS_PHASE));
        AutoCloseable bossMusic = eventBus.subscribe(GameEvent.Type.BOSS_MUSIC_CHANGED,
            event -> audioManager.setBossMode(Boolean.TRUE.equals(event.payload())));
        return () -> { sounds.close(); depth.close(); bossPhases.close(); bossMusic.close(); };
    }
}
