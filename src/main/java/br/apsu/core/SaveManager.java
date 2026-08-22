package br.apsu.core;

import com.google.gson.Gson;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSyntaxException;
import br.apsu.model.environment.Difficulty;
import br.apsu.model.hero.HeroType;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

/**
 * Persistência local e versionada do progresso do jogador.
 *
 * O caminho pode ser injetado para testes; no jogo, o save fica em
 * {@code ~/.apsu/savegame.json}. Falhas de I/O ou JSON inválido não interrompem
 * a partida: {@link #load()} retorna vazio para que o chamador use um novo jogo.
 */
public final class SaveManager {
    public static final String CURRENT_VERSION = "1.0";

    private final Path savePath;
    private final Gson gson = new Gson();

    public SaveManager() {
        this(Path.of(System.getProperty("user.home"), ".apsu", "savegame.json"));
    }

    public SaveManager(Path savePath) {
        this.savePath = savePath;
    }

    public record SaveData(
        String version,
        String difficulty,
        String selectedHero,
        int unlockedPhase,
        boolean[] tabletsCollected,
        int highScore
    ) {
        public SaveData {
            version = (version == null || version.isBlank()) ? CURRENT_VERSION : version;
            difficulty = isDifficulty(difficulty) ? difficulty : Difficulty.MEDIO.name();
            selectedHero = isHero(selectedHero) ? selectedHero : HeroType.GUARDIAO.name();
            unlockedPhase = Math.max(1, Math.min(5, unlockedPhase));
            tabletsCollected = tabletsCollected == null ? new boolean[5] : tabletsCollected.clone();
            highScore = Math.max(0, highScore);
        }

        @Override
        public boolean[] tabletsCollected() {
            return tabletsCollected.clone();
        }

        private static boolean isDifficulty(String value) {
            try {
                Difficulty.valueOf(value);
                return true;
            } catch (IllegalArgumentException | NullPointerException ignored) {
                return false;
            }
        }

        private static boolean isHero(String value) {
            try {
                HeroType.valueOf(value);
                return true;
            } catch (IllegalArgumentException | NullPointerException ignored) {
                return false;
            }
        }
    }

    public SaveData newGame() {
        return new SaveData(CURRENT_VERSION, Difficulty.MEDIO.name(), HeroType.GUARDIAO.name(), 1, new boolean[5], 0);
    }

    public void save(SaveData data) throws IOException {
        if (data == null) throw new IllegalArgumentException("Dados de save não podem ser nulos");
        Path parent = savePath.getParent();
        if (parent != null) Files.createDirectories(parent);
        Files.writeString(savePath, gson.toJson(data), StandardCharsets.UTF_8);
    }

    public Optional<SaveData> load() {
        if (!Files.isRegularFile(savePath)) return Optional.empty();
        try {
            SaveData data = gson.fromJson(Files.readString(savePath, StandardCharsets.UTF_8), SaveData.class);
            return Optional.ofNullable(data);
        } catch (IOException | JsonParseException ignored) {
            return Optional.empty();
        }
    }

    public Path getSavePath() {
        return savePath;
    }
}
