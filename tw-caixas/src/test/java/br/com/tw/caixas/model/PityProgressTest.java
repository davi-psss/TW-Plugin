package br.com.tw.caixas.model;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class PityProgressTest {
    @Test
    public void manualFiftiethOpeningGuaranteesMaximumReward() {
        PityProgress.Step step = PityProgress.advance(49 * PityProgress.UNITS_PER_MANUAL_OPEN,
                50, PityProgress.UNITS_PER_MANUAL_OPEN);

        assertTrue(step.isGuaranteed());
        assertEquals(0, step.getProgress());
    }

    @Test
    public void bulkOpeningAdvancesAtHalfRateAndGuaranteesAfterOneHundredCrates() {
        int progress = 0;
        for (int i = 0; i < 99; i++) {
            PityProgress.Step step = PityProgress.advance(progress, 50, 500);
            assertFalse(step.isGuaranteed());
            progress = step.getProgress();
        }
        PityProgress.Step last = PityProgress.advance(progress, 50, 500);

        assertTrue(last.isGuaranteed());
        assertEquals(0, last.getProgress());
    }

    @Test
    public void countdownRoundsUpAndNeverReturnsNegative() {
        assertEquals(50, PityProgress.openingsUntilGuaranteed(0, 50));
        assertEquals(1, PityProgress.openingsUntilGuaranteed(49 * 1000 + 1, 50));
        assertEquals(0, PityProgress.openingsUntilGuaranteed(50 * 1000, 50));
    }
}
