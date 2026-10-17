package br.com.tw.core;

import br.com.tw.core.data.UserManager;
import br.com.tw.core.data.UserRepository;
import br.com.tw.core.database.DatabaseManager;
import br.com.tw.core.managers.TWScoreboardManager;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;
import java.util.ArrayList;
import java.util.Collections;

public class TWCorePlugin extends JavaPlugin {

    private static TWCorePlugin instance;
    private DatabaseManager databaseManager;
    private UserManager userManager;
    private TWScoreboardManager scoreboardManager;

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();

        databaseManager = new DatabaseManager(this);
        try {
            validateConfiguration();
            databaseManager.connect();
            UserRepository userRepository = new UserRepository(databaseManager);
            userManager = new UserManager(this, userRepository);
        } catch (RuntimeException e) {
            getLogger().log(java.util.logging.Level.SEVERE, "Falha ao inicializar o banco e os dados dos jogadores.", e);
            if (databaseManager != null) databaseManager.disconnect();
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        // Registra os eventos de Join e Quit
        getServer().getPluginManager().registerEvents(userManager, this);

        getServer().getPluginManager().registerEvents(new br.com.tw.core.menus.MenuListener(), this);

        scoreboardManager = new TWScoreboardManager(this);

        getLogger().info("[TW-Core] Nucleo de dados carregado com sucesso!");
    }

    @Override
    public void onDisable() {
        if (userManager != null) {
            getLogger().info("[TW-Core] Salvando dados de todos os jogadores...");
            userManager.saveAllSync(); // Salva de forma síncrona para não cortar o processo no meio
        }
        if (scoreboardManager != null) {
            scoreboardManager.stop();
        }
        if (databaseManager != null) {
            databaseManager.disconnect();
        }
    }

    public static TWCorePlugin getInstance() {
        return instance;
    }

    public DatabaseManager getDatabaseManager() {
        return databaseManager;
    }

    public UserManager getUserManager() {
        return userManager;
    }

    public String getMineWorldName() {
        return getConfig().getString("worlds.mines", "minas");
    }

    public String getPlotsWorldName() {
        return getConfig().getString("worlds.plots", "terrenos");
    }

    public List<String> getAdditionalPlotsWorldNames() {
        return getConfig().getStringList("worlds.plots-aliases");
    }

    public boolean isMineWorld(String worldName) {
        return worldName != null && worldName.equalsIgnoreCase(getMineWorldName());
    }

    public boolean isPlotsWorld(String worldName) {
        if (worldName == null) return false;
        if (worldName.equalsIgnoreCase(getPlotsWorldName())) return true;
        for (String alias : getAdditionalPlotsWorldNames()) {
            if (worldName.equalsIgnoreCase(alias)) return true;
        }
        return false;
    }

    public List<String> getRankHierarchy() {
        return getConfiguredOrder("progression.rank-order");
    }

    public List<String> getPrestigeHierarchy() {
        return getConfiguredOrder("progression.prestige-order");
    }

    private List<String> getConfiguredOrder(String path) {
        List<String> configuredRanks = getConfig().getStringList(path);
        List<String> normalized = new ArrayList<>();
        for (String rank : configuredRanks) {
            if (rank != null && !rank.trim().isEmpty()) normalized.add(rank.trim().toUpperCase(java.util.Locale.ROOT));
        }
        if (normalized.isEmpty()) {
            throw new IllegalStateException(path + " deve conter ao menos um valor.");
        }
        if (new java.util.HashSet<>(normalized).size() != normalized.size()) {
            throw new IllegalStateException(path + " não pode conter valores duplicados.");
        }
        return Collections.unmodifiableList(normalized);
    }

    public double getPrestigeMultiplier(String prestigeId) {
        if (prestigeId == null) return 1.0;
        double multiplier = getConfig().getDouble("progression.prestige-multipliers."
                + prestigeId.toUpperCase(java.util.Locale.ROOT), 1.0);
        if (!Double.isFinite(multiplier) || multiplier < 1.0) {
            throw new IllegalStateException("Multiplicador inválido em progression.prestige-multipliers."
                    + prestigeId + ".");
        }
        return multiplier;
    }

    private void validateConfiguration() {
        if (getMineWorldName().trim().isEmpty() || getPlotsWorldName().trim().isEmpty()) {
            throw new IllegalStateException("Os nomes dos mundos mines e plots não podem estar vazios.");
        }
        getRankHierarchy();
        getPrestigeHierarchy();
        org.bukkit.configuration.ConfigurationSection multipliers =
                getConfig().getConfigurationSection("progression.prestige-multipliers");
        if (multipliers != null) {
            for (String key : multipliers.getKeys(false)) {
                double value = multipliers.getDouble(key, Double.NaN);
                if (!Double.isFinite(value) || value < 1.0) {
                    throw new IllegalStateException("Multiplicador inválido em progression.prestige-multipliers." + key + ".");
                }
            }
        }
    }
}