package br.apsu.core.map;

import com.google.gson.Gson;
import com.google.gson.JsonParseException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

/**
 * Lê os layouts JSON gerados pelo utilitário OpenMPI.
 *
 * <p>O jogo continua utilizável sem OpenMPI: a integração é opt-in por meio
 * de {@code -Dapsu.map.layoutDir=mpi/generated}. Isso impede que uma ausência
 * de ferramenta externa bloqueie a inicialização do JavaFX.</p>
 */
public final class MPIMapLoader {
    public static final String LAYOUT_DIRECTORY_PROPERTY = "apsu.map.layoutDir";
    private static final Gson GSON = new Gson();

    public record Layout(int version, int phase, int width, int height, List<String> tiles) {
        public char tileAt(int column, int row) {
            if (column < 0 || row < 0 || column >= width || row >= height) return ' ';
            return tiles.get(row).charAt(column);
        }
    }

    /** Carrega e valida um layout; entradas ruins retornam vazio, sem derrubar o jogo. */
    public Optional<Layout> load(Path file) {
        if (file == null || !Files.isRegularFile(file)) return Optional.empty();
        try {
            Layout layout = GSON.fromJson(Files.readString(file), Layout.class);
            return isValid(layout) ? Optional.of(layout) : Optional.empty();
        } catch (IOException | JsonParseException ex) {
            return Optional.empty();
        }
    }

    /** Procura {@code phase-N.json} no diretório definido para a execução atual. */
    public Optional<Layout> loadConfiguredPhase(int phase) {
        String directory = System.getProperty(LAYOUT_DIRECTORY_PROPERTY);
        if (directory == null || directory.isBlank() || phase < 1) return Optional.empty();
        return load(Path.of(directory, "phase-" + phase + ".json"));
    }

    private boolean isValid(Layout layout) {
        if (layout == null || layout.version() != 1 || layout.phase() < 1 || layout.phase() > 5
            || layout.width() <= 0 || layout.height() <= 0 || layout.tiles() == null
            || layout.tiles().size() != layout.height()) return false;
        return layout.tiles().stream().allMatch(row -> row != null && row.length() == layout.width());
    }
}
