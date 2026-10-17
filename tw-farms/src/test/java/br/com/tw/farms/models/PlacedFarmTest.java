package br.com.tw.farms.models;

import org.junit.Test;

import java.util.UUID;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class PlacedFarmTest {
    @Test
    public void storesAndCollectsRegularAndGoldenHarvestSeparately() {
        PlacedFarm farm = newFarm(2, 100);
        farm.harvest(7, 3);

        assertEquals(10, farm.getStoredTotal());
        assertEquals(10, farm.getTotalHarvested());
        assertArrayEquals(new long[]{7, 3}, farm.collect());
        assertEquals(0, farm.getStoredTotal());
        assertEquals(10, farm.getTotalHarvested());
    }

    @Test
    public void snapshotPreservesConfigurationAndAccruedState() {
        PlacedFarm farm = newFarm(4, 12);
        farm.harvest(5, 2);
        farm.addFertilizer(20, 3);
        farm.upgradeSpeed();
        farm.upgradeStorage();
        farm.upgradeQuality();
        farm.setAutoSell(true);
        farm.setBooster(2.0, 900L);
        PlacedFarm copy = farm.snapshot();

        assertEquals(4, copy.getStackAmount());
        assertEquals(32, copy.getFertilizer());
        assertEquals(3, copy.getSoilLevel());
        assertEquals(5, copy.getStored());
        assertEquals(2, copy.getStoredGolden());
        assertEquals(2, copy.getSpeedLevel());
        assertEquals(2, copy.getStorageLevel());
        assertEquals(2, copy.getQualityLevel());
        assertTrue(copy.isAutoSell());
        assertEquals(2.0, copy.getBoosterMultiplier(), 0.0);
        assertEquals(900, copy.getBoosterEndTime());
    }

    @Test
    public void siloCapacityScalesByFarmStackAndStorageUpgrade() {
        PlacedFarm farm = newFarm(3, 0);
        farm.upgradeStorage();
        assertEquals(600, farm.getCapacity(100));
    }

    @Test
    public void mergingFarmsPreservesTheirResourcesAndBestUpgrades() {
        UUID owner = UUID.randomUUID();
        PlacedFarm target = newPlacedFarm(owner, 2, 10);
        PlacedFarm incoming = newPlacedFarm(owner, 4, 20);
        incoming.harvest(5, 2);
        incoming.upgradeSpeed();
        incoming.setAutoSellUnlocked(true);
        incoming.setAutoSell(true);

        target.mergeFrom(incoming);

        assertEquals(6, target.getStackAmount());
        assertEquals(30, target.getFertilizer());
        assertEquals(5, target.getStored());
        assertEquals(2, target.getStoredGolden());
        assertEquals(2, target.getSpeedLevel());
        assertTrue(target.isAutoSell());
        assertTrue(target.isAutoSellUnlocked());
    }

    @Test(expected = ArithmeticException.class)
    public void capacityRejectsLongOverflow() {
        newFarm(Long.MAX_VALUE, 0).getCapacity(2);
    }

    private PlacedFarm newFarm(long stack, long fertilizer) {
        return newPlacedFarm(UUID.randomUUID(), stack, fertilizer);
    }

    private PlacedFarm newPlacedFarm(UUID owner, long stack, long fertilizer) {
        return new PlacedFarm("plots;1;64;1", owner, "Tester", "WHEAT",
                stack, fertilizer, 1, 0, 0, 0, 0, 1, 1, 1,
                false, 1.0, 0, 0, 0);
    }
}
