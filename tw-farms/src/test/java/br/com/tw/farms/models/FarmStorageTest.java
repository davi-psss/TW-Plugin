package br.com.tw.farms.models;

import org.junit.Test;

import java.util.UUID;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class FarmStorageTest {
    @Test
    public void restoreDoesNotCountTheSameHarvestTwice() {
        FarmStorage storage = new FarmStorage(UUID.randomUUID(), "TRIGO", 0, 0, 0, 0);
        storage.harvest(10, 2);
        long[] collected = storage.collect();
        storage.restoreStored(collected[0], collected[1]);

        assertEquals(12, storage.getStoredTotal());
        assertEquals(12, storage.getTotalHarvested());
        assertArrayEquals(new long[]{10, 2}, storage.collect());
    }

    @Test
    public void autosellSettingIsCopiedInSnapshot() {
        FarmStorage storage = new FarmStorage(UUID.randomUUID(), "TRIGO", 1, 0, 1, 0);
        storage.setAutoSell(true);
        storage.setGrowthBooster(2.0, 1800L);

        assertTrue(storage.snapshot().isAutoSell());
        assertEquals(2.0, storage.snapshot().getGrowthMultiplier(), 0.0);
        assertEquals(1800L, storage.snapshot().getGrowthBoostEndTime());
    }

    @Test
    public void activeBoosterCannotBeExtendedByRepeatedUse() {
        FarmStorage storage = new FarmStorage(UUID.randomUUID(), "TRIGO", 0, 0, 0, 0);

        assertTrue(storage.activateGrowthBooster(1.5, 1800, 1000, 3600));
        assertEquals(1.5, storage.getGrowthMultiplier(), 0.0);
        assertEquals(2800, storage.getGrowthBoostEndTime());

        assertFalse(storage.activateGrowthBooster(1.5, 1800, 1200, 3600));
        assertEquals(2800, storage.getGrowthBoostEndTime());
    }

    @Test
    public void strongerBoosterImprovesPowerWithoutResettingTheTimer() {
        FarmStorage storage = new FarmStorage(UUID.randomUUID(), "TRIGO", 0, 0, 0, 0);
        storage.activateGrowthBooster(1.5, 1800, 1000, 3600);

        assertTrue(storage.activateGrowthBooster(2.0, 900, 1100, 3600));
        assertEquals(2.0, storage.getGrowthMultiplier(), 0.0);
        assertEquals(2800, storage.getGrowthBoostEndTime());
    }
}
