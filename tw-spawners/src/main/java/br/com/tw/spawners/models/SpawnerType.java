package br.com.tw.spawners.models;

import org.bukkit.ChatColor;
import org.bukkit.entity.EntityType;

public class SpawnerType {
    private final String id;
    private final String displayName;
    private final EntityType entityType;
    private final String requiredRank;
    private final double buyCost;
    private final double valuePerKill;
    private final double tokenChance;

    public SpawnerType(String id, String displayName, EntityType entityType, String requiredRank, double buyCost, double valuePerKill, double tokenChance) {
        this.id = id.toUpperCase();
        this.displayName = ChatColor.translateAlternateColorCodes('&', displayName);
        this.entityType = entityType;
        this.requiredRank = requiredRank.toUpperCase();
        this.buyCost = buyCost;
        this.valuePerKill = valuePerKill;
        this.tokenChance = tokenChance;
    }

    public String getId() { return id; }
    public String getDisplayName() { return displayName; }
    public EntityType getEntityType() { return entityType; }
    public String getRequiredRank() { return requiredRank; }
    public double getBuyCost() { return buyCost; }
    public double getValuePerKill() { return valuePerKill; }
    public double getTokenChance() { return tokenChance; }
}