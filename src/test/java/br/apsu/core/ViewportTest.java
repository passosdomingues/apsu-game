package br.apsu.core;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ViewportTest {
    @Test
    void fitsWithoutCroppingOnAStandardWideScreen() {
        Viewport viewport = Viewport.fit(1920, 1080, 1366, 768);
        assertEquals(1920.0 / 1366.0, viewport.scale(), 0.0001);
        assertEquals(0, viewport.offsetX(), 0.0001);
        assertEquals((1080 - 768 * viewport.scale()) / 2, viewport.offsetY(), 0.0001);
    }

    @Test
    void letterboxesInsteadOfCroppingOnATallerScreen() {
        Viewport viewport = Viewport.fit(1280, 1024, 1366, 768);
        assertEquals(1280.0 / 1366.0, viewport.scale(), 0.0001);
        assertEquals(0, viewport.offsetX(), 0.0001);
        assertEquals((1024 - 768 * viewport.scale()) / 2, viewport.offsetY(), 0.0001);
    }
}
