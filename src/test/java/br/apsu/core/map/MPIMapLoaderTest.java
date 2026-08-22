package br.apsu.core.map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class MPIMapLoaderTest {
    @TempDir Path tempDirectory;

    @Test
    void loadsAValidParallelMap() throws IOException {
        Path layout = tempDirectory.resolve("phase-2.json");
        Files.writeString(layout, """
            {"version":1,"phase":2,"width":3,"height":2,"tiles":["~C~","..P"]}
            """);

        MPIMapLoader.Layout loaded = new MPIMapLoader().load(layout).orElseThrow();
        assertEquals('C', loaded.tileAt(1, 0));
        assertEquals('P', loaded.tileAt(2, 1));
        assertEquals(' ', loaded.tileAt(3, 1));
    }

    @Test
    void rejectsMalformedOrInconsistentMaps() throws IOException {
        Path layout = tempDirectory.resolve("broken.json");
        Files.writeString(layout, "{\"version\":1,\"phase\":6,\"width\":2,\"height\":1,\"tiles\":[\"x\"]}");
        assertTrue(new MPIMapLoader().load(layout).isEmpty());
    }
}
