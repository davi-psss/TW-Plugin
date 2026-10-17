package br.com.tw.farms.models;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;

import java.util.UUID;

public final class PlacedFarm {
    private final String locationKey;
    private final UUID ownerUuid;
    private final String ownerName;
    private final String typeId;
    private long stackAmount;
    private long fertilizer;
    private int soilLevel;
    private long stored;
    private long storedGolden;
    private long totalHarvested;
    private long totalSold;
    private int speedLevel;
    private int storageLevel;
    private int qualityLevel;
    private boolean autoSell;
    private boolean autoSellUnlocked;
    private double boosterMultiplier;
    private long boosterEndTime;
    private long lastHarvestTime;
    private long lastTickEpochSeconds;

    public PlacedFarm(String locationKey, UUID ownerUuid, String ownerName, String typeId,
                      long stackAmount, long fertilizer, int soilLevel, long stored, long storedGolden,
                      long totalHarvested, long totalSold, int speedLevel, int storageLevel,
                      int qualityLevel, boolean autoSell, double boosterMultiplier,
                      long boosterEndTime, long lastHarvestTime, long lastTickEpochSeconds) {
        this(locationKey, ownerUuid, ownerName, typeId, stackAmount, fertilizer, soilLevel, stored, storedGolden,
                totalHarvested, totalSold, speedLevel, storageLevel, qualityLevel, autoSell, autoSell,
                boosterMultiplier, boosterEndTime, lastHarvestTime, lastTickEpochSeconds);
    }

    public PlacedFarm(String locationKey, UUID ownerUuid, String ownerName, String typeId,
                      long stackAmount, long fertilizer, int soilLevel, long stored, long storedGolden,
                      long totalHarvested, long totalSold, int speedLevel, int storageLevel,
                      int qualityLevel, boolean autoSell, boolean autoSellUnlocked, double boosterMultiplier,
                      long boosterEndTime, long lastHarvestTime, long lastTickEpochSeconds) {
        if (locationKey == null || ownerUuid == null || typeId == null) {
            throw new IllegalArgumentException("Localização, dono e tipo da farm são obrigatórios.");
        }
        if (stackAmount < 1 || fertilizer < 0 || soilLevel < 1 || soilLevel > 3
                || stored < 0 || storedGolden < 0 || totalHarvested < 0 || totalSold < 0
                || speedLevel < 1 || storageLevel < 1 || qualityLevel < 1
                || !Double.isFinite(boosterMultiplier) || boosterMultiplier < 1.0) {
            throw new IllegalArgumentException("Estado inválido para a farm " + locationKey + ".");
        }
        Math.addExact(stored, storedGolden);
        this.locationKey = locationKey;
        this.ownerUuid = ownerUuid;
        this.ownerName = ownerName;
        this.typeId = typeId.toUpperCase(java.util.Locale.ROOT);
        this.stackAmount = stackAmount;
        this.fertilizer = fertilizer;
        this.soilLevel = soilLevel;
        this.stored = stored;
        this.storedGolden = storedGolden;
        this.totalHarvested = totalHarvested;
        this.totalSold = totalSold;
        this.speedLevel = speedLevel;
        this.storageLevel = storageLevel;
        this.qualityLevel = qualityLevel;
        this.autoSell = autoSell;
        this.autoSellUnlocked = autoSellUnlocked;
        this.boosterMultiplier = boosterMultiplier;
        this.boosterEndTime = boosterEndTime;
        this.lastHarvestTime = lastHarvestTime;
        this.lastTickEpochSeconds = lastTickEpochSeconds;
    }

    public static String toKey(Location loc) {
        return loc.getWorld().getName() + ";" + loc.getBlockX() + ";" + loc.getBlockY() + ";" + loc.getBlockZ();
    }

    public Location getBukkitLocation() {
        String[] parts = locationKey.split(";");
        if (parts.length != 4) return null;
        World world = Bukkit.getWorld(parts[0]);
        if (world == null) return null;
        try {
            return new Location(world, Integer.parseInt(parts[1]), Integer.parseInt(parts[2]),
                    Integer.parseInt(parts[3]));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    public String getLocationKey() { return locationKey; }
    public UUID getOwnerUuid() { return ownerUuid; }
    public String getOwnerName() { return ownerName; }
    public String getTypeId() { return typeId; }
    public long getStackAmount() { return stackAmount; }
    public void addStack(long amount) {
        if (amount < 1) throw new IllegalArgumentException("O stack a adicionar deve ser positivo.");
        stackAmount = Math.addExact(stackAmount, amount);
    }

    public void mergeFrom(PlacedFarm other) {
        if (other == null || !ownerUuid.equals(other.ownerUuid) || !typeId.equals(other.typeId)) {
            throw new IllegalArgumentException("Só é possível agrupar farms do mesmo tipo e proprietário.");
        }
        long mergedStack = Math.addExact(stackAmount, other.stackAmount);
        long mergedFertilizer = Math.addExact(fertilizer, other.fertilizer);
        long mergedStored = Math.addExact(stored, other.stored);
        long mergedGolden = Math.addExact(storedGolden, other.storedGolden);
        Math.addExact(mergedStored, mergedGolden);
        long mergedHarvested = Math.addExact(totalHarvested, other.totalHarvested);
        long mergedSold = Math.addExact(totalSold, other.totalSold);
        stackAmount = mergedStack;
        fertilizer = mergedFertilizer;
        soilLevel = Math.max(soilLevel, other.soilLevel);
        stored = mergedStored;
        storedGolden = mergedGolden;
        totalHarvested = mergedHarvested;
        totalSold = mergedSold;
        speedLevel = Math.max(speedLevel, other.speedLevel);
        storageLevel = Math.max(storageLevel, other.storageLevel);
        qualityLevel = Math.max(qualityLevel, other.qualityLevel);
        autoSellUnlocked |= other.autoSellUnlocked;
        autoSell |= other.autoSell;
        if (other.boosterEndTime > boosterEndTime
                || (other.boosterEndTime == boosterEndTime && other.boosterMultiplier > boosterMultiplier)) {
            boosterMultiplier = other.boosterMultiplier;
            boosterEndTime = other.boosterEndTime;
        }
        lastHarvestTime = Math.max(lastHarvestTime, other.lastHarvestTime);
        lastTickEpochSeconds = Math.max(lastTickEpochSeconds, other.lastTickEpochSeconds);
    }
    public long getFertilizer() { return fertilizer; }
    public void addFertilizer(long amount, int level) {
        if (amount < 1 || level < 1 || level > 3) {
            throw new IllegalArgumentException("Quantidade ou nível de fertilizante inválido.");
        }
        fertilizer = Math.addExact(fertilizer, amount);
        soilLevel = Math.max(soilLevel, Math.min(3, Math.max(1, level)));
    }
    public boolean consumeFertilizer(long amount) {
        if (fertilizer < amount) return false;
        fertilizer -= amount;
        return true;
    }
    public int getSoilLevel() { return soilLevel; }
    public long getStored() { return stored; }
    public long getStoredGolden() { return storedGolden; }
    public long getTotalHarvested() { return totalHarvested; }
    public long getTotalSold() { return totalSold; }
    public int getSpeedLevel() { return speedLevel; }
    public int getStorageLevel() { return storageLevel; }
    public int getQualityLevel() { return qualityLevel; }
    public boolean isAutoSell() { return autoSell; }
    public void setAutoSell(boolean autoSell) { this.autoSell = autoSell; }
    public boolean isAutoSellUnlocked() { return autoSellUnlocked; }
    public void setAutoSellUnlocked(boolean autoSellUnlocked) { this.autoSellUnlocked = autoSellUnlocked; }
    public double getBoosterMultiplier() { return boosterMultiplier; }
    public long getBoosterEndTime() { return boosterEndTime; }
    public long getLastHarvestTime() { return lastHarvestTime; }
    public void setLastHarvestTime(long lastHarvestTime) { this.lastHarvestTime = lastHarvestTime; }
    public long getLastTickEpochSeconds() { return lastTickEpochSeconds; }
    public void setLastTickEpochSeconds(long lastTickEpochSeconds) { this.lastTickEpochSeconds = lastTickEpochSeconds; }

    public void setBooster(double multiplier, long endTime) {
        if (!Double.isFinite(multiplier) || multiplier < 1.0 || endTime < 0) {
            throw new IllegalArgumentException("Booster de farm inválido.");
        }
        boosterMultiplier = multiplier;
        boosterEndTime = endTime;
    }

    public void upgradeSpeed() { speedLevel++; }
    public void upgradeStorage() { storageLevel++; }
    public void upgradeQuality() { qualityLevel++; }

    public long getCapacity(long baseCapacity) {
        return Math.multiplyExact(Math.multiplyExact(baseCapacity, stackAmount), storageLevel);
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
        long[] result = {stored, storedGolden};
        stored = 0;
        storedGolden = 0;
        return result;
    }

    public void addTotalSold(long amount) { totalSold = Math.addExact(totalSold, amount); }

    public PlacedFarm snapshot() {
        return new PlacedFarm(locationKey, ownerUuid, ownerName, typeId, stackAmount, fertilizer, soilLevel,
                stored, storedGolden, totalHarvested, totalSold, speedLevel, storageLevel, qualityLevel,
                autoSell, autoSellUnlocked, boosterMultiplier, boosterEndTime, lastHarvestTime, lastTickEpochSeconds);
    }
}
