package br.com.tw.caixas.config;

import br.com.tw.caixas.model.CrateDefinition;
import br.com.tw.caixas.model.CrateReward;
import br.com.tw.caixas.model.CrateType;
import br.com.tw.caixas.model.RewardKind;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class CrateCatalog {
    private final Map<CrateType, CrateDefinition> definitions;
    private final int pityThreshold;
    private final int manualSeconds;
    private final int bulkLimit;
    private final int forgeCost;
    private final int divineForgeCost;
    private final int bulkPityUnits;
    private final Map<CrateType, Double> dropChances;
    private final Map<String, Double> prestigeMultipliers;

    private CrateCatalog(Map<CrateType, CrateDefinition> definitions, FileConfiguration config) {
        this.definitions = Collections.unmodifiableMap(new EnumMap<>(definitions));
        pityThreshold = requireInt(config, "opening.pity-threshold", 1, 10000);
        manualSeconds = requireInt(config, "opening.manual-seconds", 1, 60);
        bulkLimit = requireInt(config, "opening.bulk-limit", 1, 1000);
        forgeCost = requireInt(config, "opening.forge-cost", 2, 100000);
        divineForgeCost = requireInt(config, "opening.forge-divine-cost", 2, 100000);
        Map<CrateType, Double> chances = new EnumMap<>(CrateType.class);
        chances.put(CrateType.MINEIRA, requireDouble(config, "drops.mineira-per-block", 0.0, 0.1));
        chances.put(CrateType.SANGUINARIA, requireDouble(config, "drops.sanguinaria-per-mob", 0.0, 0.1));
        chances.put(CrateType.TECNOLOGICA, requireDouble(config, "drops.tecnologica-per-cycle", 0.0, 0.1));
        chances.put(CrateType.NATUREZA, requireDouble(config, "drops.natureza-per-crop", 0.0, 0.1));
        chances.put(CrateType.DIVINA, 0.0);
        dropChances = Collections.unmodifiableMap(chances);
        double bulkRate = requireDouble(config, "opening.bulk-pity-rate", 0.01, 1.0);
        bulkPityUnits = Math.max(1, (int) Math.round(bulkRate * 1000.0));
        if (bulkPityUnits >= 1000) {
            throw new IllegalArgumentException("opening.bulk-pity-rate deve ser menor que 1.0.");
        }
        ConfigurationSection prestigeSection = config.getConfigurationSection("opening.prestige-multipliers");
        Map<String, Double> multipliers = new java.util.HashMap<>();
        if (prestigeSection != null) {
            for (String key : prestigeSection.getKeys(false)) {
                multipliers.put(key.toUpperCase(Locale.ROOT),
                        requireDouble(config, "opening.prestige-multipliers." + key, 1.0, 1000000.0));
            }
        }
        prestigeMultipliers = Collections.unmodifiableMap(multipliers);
    }

    public static CrateCatalog load(JavaPlugin plugin) {
        FileConfiguration config = plugin.getConfig();
        Map<CrateType, CrateDefinition> definitions = new EnumMap<>(CrateType.class);
        for (CrateType type : CrateType.values()) {
            String path = "crates." + type.getId();
            ConfigurationSection section = config.getConfigurationSection(path);
            if (section == null) throw new IllegalArgumentException("Configuração ausente: " + path);
            String displayName = section.getString("name");
            Material icon = Material.matchMaterial(section.getString("icon", type.getDefaultIcon().name()));
            if (icon == null || icon == Material.AIR) {
                throw new IllegalArgumentException("Ícone inválido em " + path + ".icon.");
            }
            List<CrateReward> rewards = new ArrayList<>();
            for (Map<?, ?> raw : section.getMapList("rewards")) {
                rewards.add(parseReward(path, raw));
            }
            definitions.put(type, new CrateDefinition(displayName, icon, rewards));
        }
        return new CrateCatalog(definitions, config);
    }

    private static CrateReward parseReward(String path, Map<?, ?> raw) {
        double weight = number(raw.get("weight"), path + ".rewards.weight").doubleValue();
        CrateReward.Rarity rarity = CrateReward.Rarity.valueOf(string(raw.get("rarity"),
                path + ".rewards.rarity").toUpperCase(Locale.ROOT));
        RewardKind kind = RewardKind.valueOf(string(raw.get("kind"),
                path + ".rewards.kind").toUpperCase(Locale.ROOT));
        long minimum = optionalNumber(raw.get("min"), 0L).longValue();
        long maximum = optionalNumber(raw.get("max"), minimum).longValue();
        Material material = null;
        if (kind == RewardKind.ITEM) {
            material = Material.matchMaterial(string(raw.get("material"), path + ".rewards.material"));
            if (material == null || material == Material.AIR) {
                throw new IllegalArgumentException("Material inválido na recompensa de " + path + ".");
            }
        }
        String name = raw.get("display-name") == null ? null : raw.get("display-name").toString();
        String effect = raw.get("effect") == null ? null : raw.get("effect").toString();
        int charges = optionalNumber(raw.get("charges"), 0L).intValue();
        return new CrateReward(weight, rarity, kind, minimum, maximum, material, name, effect, charges);
    }

    private static Number number(Object value, String path) {
        if (!(value instanceof Number)) throw new IllegalArgumentException("Número ausente ou inválido: " + path);
        return (Number) value;
    }

    private static Number optionalNumber(Object value, long fallback) {
        return value instanceof Number ? (Number) value : fallback;
    }

    private static String string(Object value, String path) {
        if (value == null || value.toString().trim().isEmpty()) {
            throw new IllegalArgumentException("Texto ausente ou inválido: " + path);
        }
        return value.toString();
    }

    private static int requireInt(FileConfiguration config, String path, int minimum, int maximum) {
        int value = config.getInt(path, Integer.MIN_VALUE);
        if (value < minimum || value > maximum) {
            throw new IllegalArgumentException("Valor inválido em " + path + "; permitido: "
                    + minimum + " a " + maximum + ".");
        }
        return value;
    }

    private static double requireDouble(FileConfiguration config, String path, double minimum, double maximum) {
        double value = config.getDouble(path, Double.NaN);
        if (!Double.isFinite(value) || value < minimum || value > maximum) {
            throw new IllegalArgumentException("Valor inválido em " + path + "; permitido: "
                    + minimum + " a " + maximum + ".");
        }
        return value;
    }

    public CrateDefinition get(CrateType type) { return definitions.get(type); }
    public Map<CrateType, CrateDefinition> getDefinitions() { return definitions; }
    public int getPityThreshold() { return pityThreshold; }
    public int getManualSeconds() { return manualSeconds; }
    public int getBulkLimit() { return bulkLimit; }
    public int getForgeCost() { return forgeCost; }
    public int getDivineForgeCost() { return divineForgeCost; }
    public int getBulkPityUnits() { return bulkPityUnits; }
    public double getDropChance(CrateType type) { return dropChances.get(type); }
    public double getPrestigeMultiplier(String prestige) {
        return prestigeMultipliers.getOrDefault(prestige == null ? "" : prestige.toUpperCase(Locale.ROOT), 1.0);
    }
}
