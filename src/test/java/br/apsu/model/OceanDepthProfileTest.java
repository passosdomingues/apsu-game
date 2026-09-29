package br.apsu.model;

import br.apsu.model.environment.OceanDepthProfile;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class OceanDepthProfileTest {
    @Test
    void phaseLookupClampsAndProfilesBecomeDeeperAndDenser() {
        assertSame(OceanDepthProfile.COASTAL, OceanDepthProfile.forPhase(Integer.MIN_VALUE));
        assertSame(OceanDepthProfile.HADAL_TRENCH, OceanDepthProfile.forPhase(Integer.MAX_VALUE));
        assertSame(OceanDepthProfile.ABYSSAL_PLAIN, OceanDepthProfile.forPhase(3));

        OceanDepthProfile previous = OceanDepthProfile.COASTAL;
        for (OceanDepthProfile current : OceanDepthProfile.values()) {
            assertTrue(current.getDepthMeters() >= previous.getDepthMeters());
            assertTrue(current.getPressureBar() >= previous.getPressureBar());
            assertTrue(current.getBuoyancyFactor() <= previous.getBuoyancyFactor());
            assertEquals(current.ordinal() + 1, current.getPhase());
            previous = current;
        }
    }
}
