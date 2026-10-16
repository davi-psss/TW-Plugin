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
    private int level;
    private boolean enabled;
    private long lastSpawnTime;
    private long totalKills;

    // Construtor completo com todos os parâmetros (incluindo totalKills)
    public PlacedSpawner(String locationKey, UUID ownerUuid, String ownerName, String typeId, long stackAmount, int level, boolean enabled, long lastSpawnTime, long totalKills) {
        this.locationKey = locationKey;
        this.ownerUuid = ownerUuid;
        this.ownerName = ownerName;
        this.typeId = typeId.toUpperCase();
        this.stackAmount = stackAmount;
        this.level = level;
        this.enabled = enabled;
        this.lastSpawnTime = lastSpawnTime;
        this.totalKills = totalKills;
    }

    // Construtor com 8 parâmetros
    public PlacedSpawner(String locationKey, UUID ownerUuid, String ownerName, String typeId, long stackAmount, int level, boolean enabled, long lastSpawnTime) {
        this(locationKey, ownerUuid, ownerName, typeId, stackAmount, level, enabled, lastSpawnTime, 0L);
    }

    // Sobrecarga de construtor simplificada
    public PlacedSpawner(String locationKey, UUID ownerUuid, String ownerName, String typeId, long stackAmount) {
        this(locationKey, ownerUuid, ownerName, typeId, stackAmount, 1, true, System.currentTimeMillis(), 0L);
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

    public int getLevel() { return level; }
    public void setLevel(int level) { this.level = level; }
    public void upgradeLevel() { this.level++; }

    public boolean getEnabled() { return enabled; }
    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }

    public long getLastSpawnTime() { return lastSpawnTime; }
    public void setLastSpawnTime(long lastSpawnTime) { this.lastSpawnTime = lastSpawnTime; }

    // Métodos para resolver getTotalKills()
    public long getTotalKills() { return totalKills; }
    public void setTotalKills(long totalKills) { this.totalKills = totalKills; }
    public void addTotalKills(long amount) { this.totalKills += amount; }
}