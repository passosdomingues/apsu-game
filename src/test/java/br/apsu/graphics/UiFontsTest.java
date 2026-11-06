package br.apsu.graphics;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Testes de Unidade — UiFonts")
class UiFontsTest {

    @Test
    @DisplayName("Todas as fontes pre-alocadas devem ser validas e nao nulas")
    void testFontsNotNull() {
        assertNotNull(UiFonts.TITLE_80);
        assertNotNull(UiFonts.TITLE_64);
        assertNotNull(UiFonts.TEXT_26);
        assertNotNull(UiFonts.OPT_SEL_28);
        assertNotNull(UiFonts.SUBTITLE_22);
        assertNotNull(UiFonts.OPT_UNSEL_22);
        assertNotNull(UiFonts.TITLE_20);
        assertNotNull(UiFonts.TEXT_20);
        assertNotNull(UiFonts.ROW_SEL_19);
        assertNotNull(UiFonts.ROW_UNSEL_19);
        assertNotNull(UiFonts.TEXT_19);
        assertNotNull(UiFonts.ALERT_18);
        assertNotNull(UiFonts.TEXT_16);
        assertNotNull(UiFonts.HINT_15);
        assertNotNull(UiFonts.LABEL_BOLD_14);
        assertNotNull(UiFonts.HINT_BOLD_13);
        assertNotNull(UiFonts.TEXT_13);
        assertNotNull(UiFonts.TEXT_BOLD_12);
        assertNotNull(UiFonts.TEXT_12);
        assertNotNull(UiFonts.TEXT_BOLD_11);
        assertNotNull(UiFonts.TEXT_11);
        assertNotNull(UiFonts.RADAR_BOLD_10);
        assertNotNull(UiFonts.TEXT_10);
        assertNotNull(UiFonts.DAMAGE_BOLD_9);
    }

    @Test
    @DisplayName("Tamanhos das fontes devem corresponder as constantes")
    void testFontSizes() {
        assertEquals(80.0, UiFonts.TITLE_80.getSize());
        assertEquals(64.0, UiFonts.TITLE_64.getSize());
        assertEquals(28.0, UiFonts.OPT_SEL_28.getSize());
        assertEquals(22.0, UiFonts.SUBTITLE_22.getSize());
        assertEquals(14.0, UiFonts.LABEL_BOLD_14.getSize());
        assertEquals(11.0, UiFonts.TEXT_11.getSize());
        assertEquals(9.0, UiFonts.DAMAGE_BOLD_9.getSize());
    }
}
