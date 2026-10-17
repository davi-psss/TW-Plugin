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
    private long fuel;
    private double storedEarnings;
    private long lastTickEpochSeconds;

    // Novos atributos para a Árvore de Upgrades e Histórico
    private int speedLevel;
    private int storageLevel;
    private int fuelEfficiencyLevel;
    private double totalGenerated;

    // Construtor completo (Usado pelo MachineRepository ao carregar do banco de dados)
    public PlacedMachine(String locationKey, UUID ownerUuid, String ownerName, String typeId, long stackAmount, long fuel, double storedEarnings, int speedLevel, int storageLevel, int fuelEfficiencyLevel, double totalGenerated) {
        this.locationKey = locationKey;
        this.ownerUuid = ownerUuid;
        this.ownerName = ownerName;
        this.typeId = typeId.toUpperCase();
        this.stackAmount = stackAmount;
        this.fuel = fuel;
        this.storedEarnings = storedEarnings;
        this.speedLevel = speedLevel;
        this.storageLevel = storageLevel;
        this.fuelEfficiencyLevel = fuelEfficiencyLevel;
        this.totalGenerated = totalGenerated;
        this.lastTickEpochSeconds = System.currentTimeMillis() / 1000L;
    }

    // Construtor simplificado (Usado pelo MachineListener ao colocar uma máquina nova no chão)
    public PlacedMachine(String locationKey, UUID ownerUuid, String ownerName, String typeId, long stackAmount, long fuel, double storedEarnings) {
        // Por padrão, todos os upgrades começam no nível 1 e o total gerado em 0.0
        this(locationKey, ownerUuid, ownerName, typeId, stackAmount, fuel, storedEarnings, 1, 1, 1, 0.0);
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

    // --- GETTERS E SETTERS DOS UPGRADES ---

    public int getSpeedLevel() { return speedLevel; }
    public void upgradeSpeedLevel() { this.speedLevel++; }

    public int getStorageLevel() { return storageLevel; }
    public void upgradeStorageLevel() { this.storageLevel++; }

    public int getFuelEfficiencyLevel() { return fuelEfficiencyLevel; }
    public void upgradeFuelEfficiencyLevel() { this.fuelEfficiencyLevel++; }

    public double getTotalGenerated() { return totalGenerated; }
    public void addTotalGenerated(double amount) { this.totalGenerated += amount; }

    public PlacedMachine snapshot() {
        PlacedMachine copy = new PlacedMachine(locationKey, ownerUuid, ownerName, typeId, stackAmount, fuel,
                storedEarnings, speedLevel, storageLevel, fuelEfficiencyLevel, totalGenerated);
        copy.setLastTickEpochSeconds(lastTickEpochSeconds);
        return copy;
    }
}