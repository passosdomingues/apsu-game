package br.apsu.core.events;

import br.apsu.audio.AudioCue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;

@DisplayName("Testes de Unidade — EventBus")
class EventBusTest {

    @Test
    @DisplayName("Entrega eventos ao assinante e permite cancelamento")
    void publishesAndUnsubscribes() throws Exception {
        EventBus eventBus = new EventBus();
        AtomicInteger delivered = new AtomicInteger();
        AutoCloseable subscription = eventBus.subscribe(GameEvent.Type.SOUND_REQUESTED,
            event -> delivered.incrementAndGet());

        eventBus.publish(new GameEvent(GameEvent.Type.SOUND_REQUESTED, AudioCue.ATTACK, 0, 0));
        subscription.close();
        eventBus.publish(new GameEvent(GameEvent.Type.SOUND_REQUESTED, AudioCue.VICTORY, 0, 0));

        assertEquals(1, delivered.get());
    }
}
