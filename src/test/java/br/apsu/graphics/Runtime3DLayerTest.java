package br.apsu.graphics;

import br.apsu.model.environment.SceneryElement;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertEquals;

class Runtime3DLayerTest {
    @Test
    void fullReefSchoolSetIsNotRepeatedForEachCoralCollisionColumn() {
        assertNull(Runtime3DLayer.sceneryModel(SceneryElement.Type.CORAL),
            "O OBJ contém a composição completa do recife e do cardume, não uma peça de coral reutilizável");
        assertEquals("16_corrente_abissal_3d",
            Runtime3DLayer.sceneryModel(SceneryElement.Type.CURRENT));
    }
}
