package br.com.tw.farms.models;

import java.util.UUID;

public final class FarmStorage {
    private final UUID ownerUuid;
    private final String typeId;
    private long stored;
    private long storedGolden;
    private long totalHarvested;
    private long totalSold;
    private boolean autoSell;
    private boolean autoSellUnlocked;
    private double growthMultiplier = 1.0;
    private long growthBoostEndTime;

    public FarmStorage(UUID ownerUuid, String typeId, long stored, long storedGolden,
                       long totalHarvested, long totalSold) {
        this(ownerUuid, typeId, stored, storedGolden, totalHarvested, totalSold, false);
    }

    public FarmStorage(UUID ownerUuid, String typeId, long stored, long storedGolden,
                       long totalHarvested, long totalSold, boolean autoSell) {
        this(ownerUuid, typeId, stored, storedGolden, totalHarvested, totalSold, autoSell, autoSell);
    }

    public FarmStorage(UUID ownerUuid, String typeId, long stored, long storedGolden,
                       long totalHarvested, long totalSold, boolean autoSell, boolean autoSellUnlocked) {
        this(ownerUuid, typeId, stored, storedGolden, totalHarvested, totalSold,
                autoSell, autoSellUnlocked, 1.0, 0L);
    }

    public FarmStorage(UUID ownerUuid, String typeId, long stored, long storedGolden,
                       long totalHarvested, long totalSold, boolean autoSell, boolean autoSellUnlocked,
                       double growthMultiplier, long growthBoostEndTime) {
        if (ownerUuid == null || typeId == null || stored < 0 || storedGolden < 0
                || totalHarvested < 0 || totalSold < 0 || !Double.isFinite(growthMultiplier)
                || growthMultiplier < 1.0 || growthBoostEndTime < 0) {
            throw new IllegalArgumentException("Estado inválido no armazém de colheitas.");
        }
        Math.addExact(stored, storedGolden);
        this.ownerUuid = ownerUuid;
        this.typeId = typeId.toUpperCase(java.util.Locale.ROOT);
        this.stored = stored;
        this.storedGolden = storedGolden;
        this.totalHarvested = totalHarvested;
        this.totalSold = totalSold;
        this.autoSell = autoSell;
        this.autoSellUnlocked = autoSellUnlocked;
        this.growthMultiplier = growthMultiplier;
        this.growthBoostEndTime = growthBoostEndTime;
    }

    public UUID getOwnerUuid() { return ownerUuid; }
    public String getTypeId() { return typeId; }
    public long getStored() { return stored; }
    public long getStoredGolden() { return storedGolden; }
    public long getTotalHarvested() { return totalHarvested; }
    public long getTotalSold() { return totalSold; }
    public boolean isAutoSell() { return autoSell; }
    public void setAutoSell(boolean autoSell) { this.autoSell = autoSell; }
    public boolean isAutoSellUnlocked() { return autoSellUnlocked; }
    public void setAutoSellUnlocked(boolean autoSellUnlocked) { this.autoSellUnlocked = autoSellUnlocked; }
    public double getGrowthMultiplier() { return growthMultiplier; }
    public long getGrowthBoostEndTime() { return growthBoostEndTime; }

    public void setGrowthBooster(double multiplier, long endTime) {
        if (!Double.isFinite(multiplier) || multiplier < 1.0 || endTime < 0) {
            throw new IllegalArgumentException("Booster de crescimento inválido.");
        }
        growthMultiplier = multiplier;
        growthBoostEndTime = endTime;
    }

    public boolean activateGrowthBooster(double multiplier, long durationSeconds, long now,
                                         long maximumDurationSeconds) {
        if (!Double.isFinite(multiplier) || multiplier <= 1.0 || durationSeconds <= 0
                || now < 0 || maximumDurationSeconds < 1) {
            throw new IllegalArgumentException("Booster de crescimento inválido.");
        }
        boolean active = growthBoostEndTime > now;
        if (active && multiplier <= growthMultiplier) return false;
        double nextMultiplier = multiplier;
        long nextEnd = active ? growthBoostEndTime
                : Math.addExact(now, Math.min(durationSeconds, maximumDurationSeconds));
        boolean changed = nextMultiplier != growthMultiplier || nextEnd != growthBoostEndTime;
        if (changed) setGrowthBooster(nextMultiplier, nextEnd);
        return changed;
    }

    public long getStoredTotal() { return Math.addExact(stored, storedGolden); }

    public void harvest(long regular, long golden) {
        if (regular < 0 || golden < 0) throw new IllegalArgumentException("Colheita não pode ser negativa.");
        stored = Math.addExact(stored, regular);
        storedGolden = Math.addExact(storedGolden, golden);
        totalHarvested = Math.addExact(totalHarvested, Math.addExact(regular, golden));
    }

    public void restoreStored(long regular, long golden) {
        stored = Math.addExact(stored, regular);
        storedGolden = Math.addExact(storedGolden, golden);
    }

    public long[] collect() {
        long[] values = {stored, storedGolden};
        stored = 0;
        storedGolden = 0;
        return values;
    }

    public void addTotalSold(long amount) { totalSold = Math.addExact(totalSold, amount); }

    public FarmStorage snapshot() {
        return new FarmStorage(ownerUuid, typeId, stored, storedGolden, totalHarvested, totalSold,
                autoSell, autoSellUnlocked, growthMultiplier, growthBoostEndTime);
    }
}
