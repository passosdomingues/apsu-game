package br.apsu.audio;

import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

class AudioCueTest {
    @Test
    void everySemanticCueHasAnAdapterMappingAndUsableGain() {
        Set<String> assets = Set.of("shoot", "collect", "hurt", "boss-hit", "victory",
            "menu-navigate", "menu-confirm", "enemy-damaged");
        for (AudioCue cue : AudioCue.values()) {
            assertTrue(JavaFxAudioOutput.gain(cue) > 0 && JavaFxAudioOutput.gain(cue) <= 1, cue.name());
            assertTrue(assets.contains(JavaFxAudioOutput.asset(cue)), cue.name());
        }
    }

    @Test
    void keepsGameplayEventsSemanticallyDistinct() {
        assertNotEquals(JavaFxAudioOutput.asset(AudioCue.MENU_NAVIGATE), JavaFxAudioOutput.asset(AudioCue.MENU_CONFIRM));
        assertNotEquals(JavaFxAudioOutput.asset(AudioCue.ATTACK), JavaFxAudioOutput.asset(AudioCue.ENEMY_DAMAGED));
        assertNotEquals(AudioCue.ATTACK, AudioCue.ENEMY_DEFEATED);
        assertNotEquals(AudioCue.BOSS_DAMAGED, AudioCue.BOSS_PHASE);
        assertEquals(Set.of("ATTACK", "ENEMY_DAMAGED", "ENEMY_DEFEATED"),
            java.util.Arrays.stream(AudioCue.values()).map(Enum::name)
                .filter(n -> n.startsWith("ENEMY") || n.equals("ATTACK"))
                .collect(Collectors.toSet()));
    }

}
