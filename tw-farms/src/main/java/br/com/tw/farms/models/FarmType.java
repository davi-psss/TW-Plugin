package br.com.tw.farms.models;

import org.bukkit.Material;

import java.util.Collections;
import java.util.List;

public final class FarmType {
    private final String id;
    private final String displayName;
    private final Material material;
    private final Material plantingMaterial;
    private final Material product;
    private final List<Material> plantMaterials;
    private final List<Material> harvestMaterials;
    private final String requiredRank;
    private final double buyCost;
    private final double sellValue;
    private final int cycleSeconds;
    private final long fertilizerPerCycle;

    public FarmType(String id, String displayName, Material material, Material plantingMaterial, Material product,
                    List<Material> plantMaterials, List<Material> harvestMaterials, String requiredRank,
                    double buyCost, double sellValue, int cycleSeconds, long fertilizerPerCycle) {
        this.id = id.toUpperCase(java.util.Locale.ROOT);
        this.displayName = displayName;
        this.material = material;
        this.plantingMaterial = plantingMaterial;
        this.product = product;
        this.plantMaterials = Collections.unmodifiableList(plantMaterials);
        this.harvestMaterials = Collections.unmodifiableList(harvestMaterials);
        this.requiredRank = requiredRank.toUpperCase(java.util.Locale.ROOT);
        this.buyCost = buyCost;
        this.sellValue = sellValue;
        this.cycleSeconds = cycleSeconds;
        this.fertilizerPerCycle = fertilizerPerCycle;
    }

    public String getId() { return id; }
    public String getDisplayName() { return displayName; }
    public Material getMaterial() { return material; }
    public Material getPlantingMaterial() { return plantingMaterial; }
    public Material getProduct() { return product; }
    public List<Material> getPlantMaterials() { return plantMaterials; }
    public List<Material> getHarvestMaterials() { return harvestMaterials; }
    public String getRequiredRank() { return requiredRank; }
    public double getBuyCost() { return buyCost; }
    public double getSellValue() { return sellValue; }
    public int getCycleSeconds() { return cycleSeconds; }
    public long getFertilizerPerCycle() { return fertilizerPerCycle; }
}
