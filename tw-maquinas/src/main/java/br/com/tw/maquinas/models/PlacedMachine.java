package br.com.tw.maquinas.models;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;

import java.util.UUID;

public class PlacedMachine {
    private final String locationKey;
    private final UUID ownerUuid;
    private final String ownerName;
    private final String typeId;
    private long stackAmount;
    private int level;
    private long fuel;
    private double storedEarnings;
    private long lastTickEpochSeconds;

    public PlacedMachine(String locationKey, UUID ownerUuid, String ownerName, String typeId, long stackAmount, int level, long fuel, double storedEarnings) {
        this.locationKey = locationKey;
        this.ownerUuid = ownerUuid;
        this.ownerName = ownerName;
        this.typeId = typeId.toUpperCase();
        this.stackAmount = stackAmount;
        this.level = level;
        this.fuel = fuel;
        this.storedEarnings = storedEarnings;
        this.lastTickEpochSeconds = System.currentTimeMillis() / 1000L;
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
    public int getLevel() { return level; }
    public void upgradeLevel() { this.level++; }
    public long getFuel() { return fuel; }
    public void addFuel(long amount) { this.fuel += amount; }
    public boolean consumeFuel(long amount) {
        if (this.fuel >= amount) {
            this.fuel -= amount;
            return true;
        }
        return false;
    }
    public double getStoredEarnings() { return storedEarnings; }
    public void addStoredEarnings(double amount) { this.storedEarnings += amount; }
    public void setStoredEarnings(double storedEarnings) { this.storedEarnings = storedEarnings; }
    public long getLastTickEpochSeconds() { return lastTickEpochSeconds; }
    public void setLastTickEpochSeconds(long lastTickEpochSeconds) { this.lastTickEpochSeconds = lastTickEpochSeconds; }
}