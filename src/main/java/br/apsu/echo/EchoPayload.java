package br.apsu.echo;

/**
 * Payload do evento semantico {@code ECHO_PINGED} (US-034).
 *
 * Contrato:
 * - Produtor: GameContext ao ativar o eco (tecla Q em fases ativas P1-P5).
 * - Cooldown: 8.0 segundos.
 * - Consumidor: AudioEventSubscriber (reproduz AudioCue.ECHO_PING) e subsistemas de UI/telemetria.
 *
 * @param duration duracao da onda/eco ativo em segundos (4.0 s)
 * @param cooldown tempo de recarga da habilidade em segundos (8.0 s)
 */
public record EchoPayload(double duration, double cooldown) {}
