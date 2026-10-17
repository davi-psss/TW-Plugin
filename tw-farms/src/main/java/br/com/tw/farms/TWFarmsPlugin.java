package br.com.tw.farms;

import br.com.tw.core.TWCorePlugin;
import br.com.tw.core.managers.TWScoreboardStatsProvider;
import br.com.tw.farms.commands.FarmCommand;
import br.com.tw.farms.data.FarmRepository;
import br.com.tw.farms.listeners.FarmListener;
import br.com.tw.farms.managers.FarmHologramManager;
import br.com.tw.farms.managers.FarmManager;
import org.bukkit.plugin.ServicePriority;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Collections;

public final class TWFarmsPlugin extends JavaPlugin {
    private static TWFarmsPlugin instance;
    private FarmManager farmManager;
    private FarmHologramManager hologramManager;

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();
        if (TWCorePlugin.getInstance() == null || TWCorePlugin.getInstance().getDatabaseManager() == null) {
            throw new IllegalStateException("tw-core precisa iniciar antes de tw-farms.");
        }
        if (br.com.tw.economia.TWEconomiaPlugin.getInstance() == null) {
            throw new IllegalStateException("tw-economia precisa iniciar antes de tw-farms.");
        }
        hologramManager = new FarmHologramManager(this);
        farmManager = new FarmManager(this,
                new FarmRepository(TWCorePlugin.getInstance().getDatabaseManager()));
        hologramManager.startOrphanCleanup(key -> farmManager.getPlacedFarms().containsKey(key));

        getServer().getServicesManager().register(TWScoreboardStatsProvider.class, player -> {
            long count = farmManager.getPhysicalCrops().values().stream()
                    .filter(crop -> crop.getOwnerUuid().equals(player.getUniqueId())
                            && crop.getLocationKey().startsWith(player.getWorld().getName() + ";"))
                    .count();
            return Collections.singletonList("§fPlantações: §e" + count);
        }, this, ServicePriority.Normal);

        getCommand("farm").setExecutor(new FarmCommand(farmManager));
        getServer().getPluginManager().registerEvents(new FarmListener(farmManager), this);
        getLogger().info("[TW-Farms] Plantações físicas, boosters de crescimento e armazéns ativados.");
    }

    @Override
    public void onDisable() {
        if (farmManager != null) farmManager.saveAllSync();
        if (hologramManager != null) hologramManager.clearAll();
    }

    public static TWFarmsPlugin getInstance() { return instance; }
    public FarmManager getFarmManager() { return farmManager; }
    public FarmHologramManager getHologramManager() { return hologramManager; }
}
