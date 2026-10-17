package br.com.tw.caixas.model;

import org.bukkit.Material;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public final class CrateDefinition {
    private final String displayName;
    private final Material icon;
    private final List<CrateReward> rewards;
    private final List<CrateReward> legendaryRewards;
    private final double totalWeight;
    private final double legendaryWeight;

    public CrateDefinition(String displayName, Material icon, List<CrateReward> rewards) {
        if (displayName == null || displayName.trim().isEmpty() || icon == null || rewards == null
                || rewards.isEmpty()) {
            throw new IllegalArgumentException("Definição de caixa incompleta.");
        }
        this.displayName = displayName;
        this.icon = icon;
        this.rewards = CrateReward.immutableCopy(rewards);
        List<CrateReward> legendary = new ArrayList<>();
        double allWeight = 0.0;
        double maxWeight = 0.0;
        for (CrateReward reward : rewards) {
            allWeight += reward.getWeight();
            if (reward.getRarity() == CrateReward.Rarity.LEGENDARY) {
                legendary.add(reward);
                maxWeight += reward.getWeight();
            }
        }
        if (!Double.isFinite(allWeight) || allWeight <= 0.0) {
            throw new IllegalArgumentException("Peso total das recompensas inválido.");
        }
        if (legendary.isEmpty()) {
            throw new IllegalArgumentException("Cada caixa precisa de ao menos uma recompensa LEGENDARY para o pity.");
        }
        this.legendaryRewards = CrateReward.immutableCopy(legendary);
        this.totalWeight = allWeight;
        this.legendaryWeight = maxWeight;
    }

    public String getDisplayName() { return displayName; }
    public Material getIcon() { return icon; }
    public List<CrateReward> getRewards() { return rewards; }

    public CrateReward draw() {
        return drawFrom(rewards, totalWeight);
    }

    public CrateReward drawLegendary() {
        return drawFrom(legendaryRewards, legendaryWeight);
    }

    private CrateReward drawFrom(List<CrateReward> pool, double weight) {
        double roll = ThreadLocalRandom.current().nextDouble(weight);
        for (CrateReward reward : pool) {
            roll -= reward.getWeight();
            if (roll < 0.0) return reward;
        }
        return pool.get(pool.size() - 1);
    }
}
