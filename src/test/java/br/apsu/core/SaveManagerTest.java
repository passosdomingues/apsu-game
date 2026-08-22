package br.apsu.core;

import br.apsu.model.environment.Difficulty;
import br.apsu.model.hero.HeroType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Testes de Unidade — SaveManager")
class SaveManagerTest {

    @TempDir
    Path tempDir;

    @Test
    @DisplayName("Persiste e recupera o progresso JSON")
    void savesAndLoadsProgress() throws IOException {
        SaveManager manager = new SaveManager(tempDir.resolve("profile/savegame.json"));
        SaveManager.SaveData expected = new SaveManager.SaveData(
            SaveManager.CURRENT_VERSION, Difficulty.DIFICIL.name(), HeroType.ABISSAL.name(),
            4, new boolean[]{true, true, false, true, false}, 14500);

        manager.save(expected);
        SaveManager.SaveData loaded = manager.load().orElseThrow();

        assertEquals(expected.version(), loaded.version());
        assertEquals(expected.difficulty(), loaded.difficulty());
        assertEquals(expected.selectedHero(), loaded.selectedHero());
        assertEquals(4, loaded.unlockedPhase());
        assertArrayEquals(expected.tabletsCollected(), loaded.tabletsCollected());
        assertEquals(14500, loaded.highScore());
    }

    @Test
    @DisplayName("Save inexistente ou JSON inválido não quebra a sessão")
    void missingOrInvalidSaveIsIgnored() throws IOException {
        Path file = tempDir.resolve("savegame.json");
        SaveManager manager = new SaveManager(file);

        assertTrue(manager.load().isEmpty());
        Files.writeString(file, "{ json inválido }");
        assertTrue(manager.load().isEmpty());
    }

    @Test
    @DisplayName("Dados corrompidos são normalizados em limites seguros")
    void normalizesInvalidData() {
        SaveManager.SaveData data = new SaveManager.SaveData(null, "IMPOSSIVEL", "NENHUM", 99, null, -3);

        assertEquals(Difficulty.MEDIO.name(), data.difficulty());
        assertEquals(HeroType.GUARDIAO.name(), data.selectedHero());
        assertEquals(5, data.unlockedPhase());
        assertEquals(5, data.tabletsCollected().length);
        assertEquals(0, data.highScore());
    }
}
