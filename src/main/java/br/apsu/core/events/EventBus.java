package br.apsu.core.events;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

/** Barramento síncrono e thread-safe para comunicação entre lógica e apresentação. */
public final class EventBus {
    private final Map<GameEvent.Type, List<Consumer<GameEvent>>> listeners = new EnumMap<>(GameEvent.Type.class);

    public EventBus() {
        for (GameEvent.Type type : GameEvent.Type.values()) {
            listeners.put(type, new CopyOnWriteArrayList<>());
        }
    }

    public AutoCloseable subscribe(GameEvent.Type type, Consumer<GameEvent> listener) {
        listeners.get(type).add(listener);
        return () -> listeners.get(type).remove(listener);
    }

    public void publish(GameEvent event) {
        for (Consumer<GameEvent> listener : listeners.get(event.type())) {
            listener.accept(event);
        }
    }
}
