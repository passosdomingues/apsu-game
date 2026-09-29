package br.apsu.core.events;

/** Evento imutável emitido pela lógica de jogo para consumidores desacoplados. */
public record GameEvent(Type type, Object payload, double worldX, double worldY) {
    public enum Type {
        SOUND_REQUESTED,
        HERO_DAMAGED,
        HERO_SHOT,
        ENEMY_DEFEATED,
        TABLET_COLLECTED,
        BOSS_DEFEATED,
        BOSS_PHASE_CHANGED,
        OCEAN_DEPTH_CHANGED,
        BOSS_MUSIC_CHANGED
    }
}
