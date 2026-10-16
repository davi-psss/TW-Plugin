package br.com.tw.maquinas.models;

import org.bukkit.ChatColor;
import org.bukkit.Material;

public class MachineType {
    private final String id;
    private final String displayName;
    private final Material material;
    private final String requiredRank;
    private final double buyCost;
    private final double valuePerCycle;
    private final int cycleSeconds;

    public MachineType(String id, String displayName, Material material, String requiredRank, double buyCost, double valuePerCycle, int cycleSeconds) {
        this.id = id.toUpperCase();
        this.displayName = ChatColor.translateAlternateColorCodes('&', displayName);
        this.material = material;
        this.requiredRank = requiredRank.toUpperCase();
        this.buyCost = buyCost;
        this.valuePerCycle = valuePerCycle;
        this.cycleSeconds = cycleSeconds;
    }

    public String getId() { return id; }
    public String getDisplayName() { return displayName; }
    public Material getMaterial() { return material; }
    public String getRequiredRank() { return requiredRank; }
    public double getBuyCost() { return buyCost; }
    public double getValuePerCycle() { return valuePerCycle; }
    public int getCycleSeconds() { return cycleSeconds; }
}