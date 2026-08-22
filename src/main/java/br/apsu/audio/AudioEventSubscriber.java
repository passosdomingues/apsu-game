package br.apsu.audio;

import br.apsu.core.events.EventBus;
import br.apsu.core.events.GameEvent;

/** Conecta pedidos de áudio do jogo ao adaptador JavaFX AudioClip. */
public final class AudioEventSubscriber {
    private AudioEventSubscriber() {}

    public static AutoCloseable bind(EventBus eventBus, AudioManager audioManager) {
        return eventBus.subscribe(GameEvent.Type.SOUND_REQUESTED, event -> {
            if (event.payload() instanceof String soundName) audioManager.playSound(soundName);
        });
    }
}
