package br.apsu.echo;

import br.apsu.audio.AudioCue;
import br.apsu.audio.AudioEventSubscriber;
import br.apsu.audio.AudioPort;
import br.apsu.core.GameContext;
import br.apsu.core.SaveManager;
import br.apsu.core.events.EventBus;
import br.apsu.core.events.GameEvent;
import javafx.scene.input.KeyCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("US-034: Evento semantico e som do eco")
class EchoAudioEventTest {

    private GameContext context;
    private EventBus eventBus;
    private List<GameEvent> echoEvents;

    @BeforeEach
    void setUp() {
        context = new GameContext(new SaveManager(Path.of("target/test-echo-audio-save.json")));
        context.startP1();
        eventBus = context.getEventBus();
        echoEvents = new ArrayList<>();
        eventBus.subscribe(GameEvent.Type.ECHO_PINGED, echoEvents::add);
    }

    @Test
    @DisplayName("AC1: Quando eco e ativado, ECHO_PINGED e publicado uma unica vez com payload e cooldown")
    void testEchoPingedPublishedOnceOnActivation() {
        assertEquals(0, echoEvents.size(), "Nenhum evento antes do acionamento");

        // Pressiona Q para ativar o eco
        context.onKey(KeyCode.Q);

        assertEquals(1, echoEvents.size(), "ECHO_PINGED deve ser publicado exatamente uma vez");
        GameEvent event = echoEvents.get(0);
        assertEquals(GameEvent.Type.ECHO_PINGED, event.type());

        // Valida payload documentado
        assertInstanceOf(EchoPayload.class, event.payload(), "Payload deve ser EchoPayload");
        EchoPayload payload = (EchoPayload) event.payload();
        assertEquals(EchoController.ECHO_DURATION_SECONDS, payload.duration(), 0.001, "Duracao deve ser 4.0s");
        assertEquals(EchoController.COOLDOWN_DURATION_SECONDS, payload.cooldown(), 0.001, "Cooldown deve ser 8.0s");

        // Valida coordenadas do heroi
        assertEquals(context.getHero().getX(), event.worldX(), 0.001);
        assertEquals(context.getHero().getY(), event.worldY(), 0.001);
    }

    @Test
    @DisplayName("AC2: AudioCue ECHO_PING e acionado pelo subscriber e resolvido apenas no adaptador")
    void testAudioCueEchoPingResolvedInAdapterOnly() {
        // Mock de AudioPort para verificar que o consumidor AudioEventSubscriber reage a ECHO_PINGED
        List<AudioCue> playedCues = new ArrayList<>();
        AudioPort mockAudio = new AudioPort() {
            @Override public void playSound(AudioCue cue) { playedCues.add(cue); }
            @Override public void setDepth(br.apsu.model.environment.OceanDepthProfile depth) {}
            @Override public void setBossMode(boolean bossMode) {}
        };

        // Conecta o subscriber do audio ao eventBus
        AutoCloseable binding = AudioEventSubscriber.bind(eventBus, mockAudio);

        // Dispara eco
        context.onKey(KeyCode.Q);

        // AudioEventSubscriber deve ter recebido ECHO_PINGED e solicitado AudioCue.ECHO_PING
        assertTrue(playedCues.contains(AudioCue.ECHO_PING),
            "Consumidor AudioEventSubscriber deve solicitar AudioCue.ECHO_PING ao receber ECHO_PINGED");

        try {
            binding.close();
        } catch (Exception ignored) {}
    }

    @Test
    @DisplayName("AC3: Em recarga, quando Q e pressionado, nenhum ECHO_PINGED e publicado")
    void testNoEchoPingedPublishedDuringCooldown() {
        // Primeira ativacao: 1 evento publicado
        context.onKey(KeyCode.Q);
        assertEquals(1, echoEvents.size());

        // Simula passagem de 4.0 s: eco expira e entra em recarga
        context.getEchoController().update(4.0, true);
        assertTrue(context.getEchoController().isOnCooldown(), "Eco deve estar em recarga");

        // Pressiona Q varias vezes durante a recarga
        context.onKey(KeyCode.Q);
        context.onKey(KeyCode.Q);
        context.onKey(KeyCode.Q);

        // Quantidade de eventos deve permanecer 1 (nenhum novo evento publicado em recarga)
        assertEquals(1, echoEvents.size(),
            "Nenhum ECHO_PINGED deve ser publicado enquanto a recarga estiver em andamento");
    }
}
