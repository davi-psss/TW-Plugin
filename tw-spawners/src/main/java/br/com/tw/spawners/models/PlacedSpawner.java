package br.com.tw.spawners.models;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;

import java.util.UUID;

public class PlacedSpawner {

    private final String locationKey;
    private final UUID ownerUuid;
    private final String ownerName;
    private final String typeId;

    private long stackAmount;
    private long lastSpawnTime;

    private int speedLevel;
    private int lootLevel;
    private int amountLevel;

    private boolean virtualMode;
    private boolean autoSell;

    private double storedMoney;
    private long storedTokens;
    private long storedXp;
    private long storedSouls;
    private long totalKills;

    private double boosterMultiplier;
    private long boosterEndTime;

    public PlacedSpawner(String locationKey, UUID ownerUuid, String ownerName, String typeId, long stackAmount, long lastSpawnTime, int speedLevel, int lootLevel, int amountLevel, boolean virtualMode, boolean autoSell, double storedMoney, long storedTokens, long storedXp, long storedSouls, long totalKills, double boosterMultiplier, long boosterEndTime) {
        this.locationKey = locationKey;
        this.ownerUuid = ownerUuid;
        this.ownerName = ownerName;
        this.typeId = typeId.toUpperCase();
        this.stackAmount = stackAmount;
        this.lastSpawnTime = lastSpawnTime;
        this.speedLevel = speedLevel;
        this.lootLevel = lootLevel;
        this.amountLevel = amountLevel;
        this.virtualMode = virtualMode;
        this.autoSell = autoSell;
        this.storedMoney = storedMoney;
        this.storedTokens = storedTokens;
        this.storedXp = storedXp;
        this.storedSouls = storedSouls;
        this.totalKills = totalKills;
        this.boosterMultiplier = boosterMultiplier;
        this.boosterEndTime = boosterEndTime;
    }

    public PlacedSpawner(String locationKey, UUID ownerUuid, String ownerName, String typeId, long stackAmount) {
        this(locationKey, ownerUuid, ownerName, typeId, stackAmount, System.currentTimeMillis() / 1000L, 1, 1, 1, false, false, 0.0, 0L, 0L, 0L, 0L, 1.0, 0L);
    }

    public static String toKey(Location loc) {
        return loc.getWorld().getName() + ";" + loc.getBlockX() + ";" + loc.getBlockY() + ";" + loc.getBlockZ();
    }

    public Location getBukkitLocation() {
        String[] parts = locationKey.split(";");
        World w = Bukkit.getWorld(parts[0]);
        if (w == null) return null;
        return new Location(w, Integer.parseInt(parts[1]), Integer.parseInt(parts[2]), Integer.parseInt(parts[3]));
    }

    public String getLocationKey() { return locationKey; }
    public UUID getOwnerUuid() { return ownerUuid; }
    public String getOwnerName() { return ownerName; }
    public String getTypeId() { return typeId; }
    public long getStackAmount() { return stackAmount; }
    public void addStack(long amount) { this.stackAmount += amount; }
    public void removeStack(long amount) { this.stackAmount = Math.max(0, this.stackAmount - amount); }
    public long getLastSpawnTime() { return lastSpawnTime; }
    public void setLastSpawnTime(long lastSpawnTime) { this.lastSpawnTime = lastSpawnTime; }

    public int getSpeedLevel() { return speedLevel; }
    public void upgradeSpeedLevel() { this.speedLevel++; }
    public void setSpeedLevel(int speedLevel) { this.speedLevel = speedLevel; }

    public int getLootLevel() { return lootLevel; }
    public void upgradeLootLevel() { this.lootLevel++; }
    public void setLootLevel(int lootLevel) { this.lootLevel = lootLevel; }

    public int getAmountLevel() { return amountLevel; }
    public void upgradeAmountLevel() { this.amountLevel++; }
    public void setAmountLevel(int amountLevel) { this.amountLevel = amountLevel; }

    public boolean isVirtualMode() { return virtualMode; }
    public void setVirtualMode(boolean virtualMode) { this.virtualMode = virtualMode; }
    public boolean isAutoSell() { return autoSell; }
    public void setAutoSell(boolean autoSell) { this.autoSell = autoSell; }

    public double getStoredMoney() { return storedMoney; }
    public void addStoredMoney(double amount) { this.storedMoney += amount; }
    public void setStoredMoney(double storedMoney) { this.storedMoney = storedMoney; }
    public long getStoredTokens() { return storedTokens; }
    public void addStoredTokens(long amount) { this.storedTokens += amount; }
    public void setStoredTokens(long storedTokens) { this.storedTokens = storedTokens; }
    public long getStoredXp() { return storedXp; }
    public void addStoredXp(long amount) { this.storedXp += amount; }
    public void setStoredXp(long storedXp) { this.storedXp = storedXp; }
    public long getStoredSouls() { return storedSouls; }
    public void addStoredSouls(long amount) { this.storedSouls += amount; }
    public void setStoredSouls(long storedSouls) { this.storedSouls = storedSouls; }

    public long getTotalKills() { return totalKills; }
    public void addTotalKills(long amount) { this.totalKills += amount; }

    public double getBoosterMultiplier() { return boosterMultiplier; }
    public void setBoosterMultiplier(double boosterMultiplier) { this.boosterMultiplier = boosterMultiplier; }
    public long getBoosterEndTime() { return boosterEndTime; }
    public void setBoosterEndTime(long boosterEndTime) { this.boosterEndTime = boosterEndTime; }
    public boolean hasActiveBooster() { return (System.currentTimeMillis() / 1000L) < boosterEndTime; }
}