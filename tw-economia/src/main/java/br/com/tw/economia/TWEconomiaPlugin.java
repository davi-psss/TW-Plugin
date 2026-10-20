package br.com.tw.economia;

import br.com.tw.economia.commands.BoosterCommand;
import br.com.tw.economia.commands.MoneyCommand;
import br.com.tw.economia.data.ChequeRepository;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.plugin.java.JavaPlugin;

public class TWEconomiaPlugin extends JavaPlugin {
    private static TWEconomiaPlugin instance;
    private double globalBooster = 1.0;
    private double maximumBooster = 100.0;
    private ChequeRepository chequeRepository;

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();
        maximumBooster = requireFiniteAtLeast("booster.maximum-multiplier",
                getConfig().getDouble("booster.maximum-multiplier", 100.0), 1.0);
        globalBooster = requireRange("booster.initial-multiplier",
                getConfig().getDouble("booster.initial-multiplier", 1.0), 0.0, maximumBooster);
        validatePrices();
        chequeRepository = new ChequeRepository(br.com.tw.core.TWCorePlugin.getInstance().getDatabaseManager());

        getCommand("booster").setExecutor(new BoosterCommand(this));
        getCommand("money").setExecutor(new MoneyCommand());
        getCommand("pay").setExecutor(new br.com.tw.economia.commands.PayCommand());
        getCommand("baltop").setExecutor(new br.com.tw.economia.commands.BaltopCommand(this));
        getCommand("eco").setExecutor(new br.com.tw.economia.commands.EcoAdminCommand());
        getCommand("cheque").setExecutor(new br.com.tw.economia.commands.ChequeCommand());
        getServer().getPluginManager().registerEvents(new br.com.tw.economia.listeners.ChequeListener(), this);
        if (getServer().getPluginManager().getPlugin("PlaceholderAPI") != null) {
            new br.com.tw.economia.hooks.TWEcoExpansion().register();
        }
        getLogger().info("[TW-Economia] Motor de precos e multiplicadores iniciado com sucesso de forma independente!");
    }

    public static TWEconomiaPlugin getInstance() {
        return instance;
    }

    public double getGlobalBooster() {
        return globalBooster;
    }

    public void setGlobalBooster(double globalBooster) {
        requireRange("booster", globalBooster, 0.0, maximumBooster);
        this.globalBooster = globalBooster;
        getConfig().set("booster.initial-multiplier", globalBooster);
        saveConfig();
    }

    public ChequeRepository getChequeRepository() {
        return chequeRepository;
    }

    public double getBlockPrice(Material material) {
        String materialPath = "prices." + material.name();
        if (getConfig().contains(materialPath)) {
            return requireFiniteAtLeast(materialPath, getConfig().getDouble(materialPath), 0.0);
        }

        String[] fallbackPaths = material.name().endsWith("_WOOL")
                ? new String[]{"prices.ALL_WOOL", "prices.WOOL"}
                : material.name().endsWith("_LOG")
                ? new String[]{"prices.ALL_LOGS", "prices.LOG"}
                : material == Material.TERRACOTTA
                ? new String[]{"prices.STAINED_CLAY"}
                : material == Material.PRISMARINE_BRICKS || material == Material.DARK_PRISMARINE
                ? new String[]{"prices.PRISMARINE"}
                : new String[0];
        for (String fallbackPath : fallbackPaths) {
            if (getConfig().contains(fallbackPath)) {
                return requireFiniteAtLeast(fallbackPath, getConfig().getDouble(fallbackPath), 0.0);
            }
        }
        return 0.0;
    }

    private void validatePrices() {
        ConfigurationSection prices = getConfig().getConfigurationSection("prices");
        if (prices == null) throw new IllegalArgumentException("Configuração prices ausente.");
        for (String key : prices.getKeys(false)) {
            String materialName = key.contains(":") ? key.substring(0, key.indexOf(':')) : key;
            if (!materialName.equals("ALL_WOOL") && !materialName.equals("ALL_LOGS")
                    && !materialName.equals("WOOL") && !materialName.equals("LOG")
                    && !materialName.equals("STAINED_CLAY")
                    && Material.matchMaterial(materialName) == null) {
                throw new IllegalArgumentException("Material inválido em prices." + key + ".");
            }
            requireFiniteAtLeast("prices." + key, prices.getDouble(key, Double.NaN), 0.0);
        }
    }

    private double requireFiniteAtLeast(String path, double value, double minimum) {
        if (!Double.isFinite(value) || value < minimum) {
            throw new IllegalArgumentException("Valor inválido em " + path + ".");
        }
        return value;
    }

    private double requireRange(String path, double value, double minimum, double maximum) {
        if (!Double.isFinite(value) || value < minimum || value > maximum) {
            throw new IllegalArgumentException("Valor inválido em " + path + "; permitido: "
                    + minimum + " a " + maximum + ".");
        }
        return value;
    }
}