package br.com.tw.caixas.model;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class PlayerCrateEffectsTest {
    @Test
    public void activatesChargesAndConsumesOnlyWhatTheEffectUses() {
        PlayerCrateEffects effects = new PlayerCrateEffects();

        assertEquals(5000L, effects.add(CrateEffect.MINING_CONTRACT, 5000L));
        assertEquals(3L, effects.consume(CrateEffect.MINING_CONTRACT, 3L));
        assertEquals(4997L, effects.get(CrateEffect.MINING_CONTRACT));
    }

    @Test
    public void consumingMoreThanAvailableNeverProducesNegativeCharges() {
        PlayerCrateEffects effects = new PlayerCrateEffects();
        effects.set(CrateEffect.SOUL_MAGNET, 2L);

        assertEquals(2L, effects.consume(CrateEffect.SOUL_MAGNET, 10L));
        assertEquals(0L, effects.get(CrateEffect.SOUL_MAGNET));
    }

    @Test(expected = ArithmeticException.class)
    public void rejectsChargeOverflowWithoutChangingTheBalance() {
        PlayerCrateEffects effects = new PlayerCrateEffects();
        effects.set(CrateEffect.SEISMIC_CHARGE, Long.MAX_VALUE);

        effects.add(CrateEffect.SEISMIC_CHARGE, 1L);
    }
}
