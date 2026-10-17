package br.com.tw.spawners;

import br.com.tw.core.TWCorePlugin;
import br.com.tw.core.managers.TWScoreboardStatsProvider;
import br.com.tw.spawners.commands.SpawnersCommand;
import br.com.tw.spawners.data.SpawnerRepository;
import br.com.tw.spawners.listeners.SpawnerListener;
import br.com.tw.spawners.managers.SpawnerHologramManager;
import br.com.tw.spawners.managers.SpawnerManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.plugin.ServicePriority;

import java.util.Collections;

public class TWSpawnersPlugin extends JavaPlugin {

    private static TWSpawnersPlugin instance;
    private SpawnerManager spawnerManager;
    private SpawnerHologramManager hologramManager;

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();

        this.hologramManager = new SpawnerHologramManager(this);

        SpawnerRepository repo = new SpawnerRepository(TWCorePlugin.getInstance().getDatabaseManager());
        this.spawnerManager = new SpawnerManager(this, repo);
        hologramManager.startOrphanCleanup(key -> spawnerManager.getPlacedSpawners().containsKey(key));

        getServer().getServicesManager().register(TWScoreboardStatsProvider.class, player -> {
            String worldPrefix = player.getWorld().getName() + ";";
            long spawnerCount = spawnerManager.getPlacedSpawners().values().stream()
                    .filter(spawner -> spawner.getOwnerUuid().equals(player.getUniqueId())
                            && spawner.getLocationKey().startsWith(worldPrefix))
                    .mapToLong(spawner -> spawner.getStackAmount())
                    .sum();
            return Collections.singletonList("§fSpawners: §e" + spawnerCount);
        }, this, ServicePriority.Normal);

        getCommand("spawners").setExecutor(new SpawnersCommand());
        getServer().getPluginManager().registerEvents(new SpawnerListener(spawnerManager), this);

        getLogger().info("[TW-Spawners] Sistema de spawners, stacking e hologramas iniciado com sucesso!");
    }

    @Override
    public void onDisable() {
        if (spawnerManager != null) {
            spawnerManager.saveAllSync();
        }
        if (hologramManager != null) {
            hologramManager.clearAll();
        }
    }

    public static TWSpawnersPlugin getInstance() {
        return instance;
    }

    public SpawnerManager getSpawnerManager() {
        return spawnerManager;
    }

    public SpawnerHologramManager getHologramManager() {
        return hologramManager;
    }
}