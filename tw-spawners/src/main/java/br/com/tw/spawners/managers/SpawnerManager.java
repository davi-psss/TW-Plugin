package br.com.tw.spawners.managers;

import br.com.tw.core.utils.TWFormat;
import br.com.tw.spawners.TWSpawnersPlugin;
import br.com.tw.spawners.data.SpawnerRepository;
import br.com.tw.spawners.models.PlacedSpawner;
import br.com.tw.spawners.models.SpawnerType;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class SpawnerManager {

    public static final String META_STACK = "TW_MOB_STACK";
    public static final String META_TYPE = "TW_MOB_TYPE";

    private final TWSpawnersPlugin plugin;
    private final SpawnerRepository repository;
    private final Map<String, SpawnerType> spawnerTypes = new LinkedHashMap<>();
    private final Map<String, PlacedSpawner> placedSpawners = new ConcurrentHashMap<>();

    private final List<String> rankHierarchy = Arrays.asList(
            "CARVAO", "NEVE", "SINO", "BISCOITO", "DUENDE", "RENA", "TRENO", "QUEBRANOZES", "ESTRELA", "NOEL", "MAX"
    );

    public SpawnerManager(TWSpawnersPlugin plugin, SpawnerRepository repository) {
        this.plugin = plugin;
        this.repository = repository;
        loadConfigTypes();
        this.placedSpawners.putAll(repository.loadAllSpawners());
        startSpawnerTask();

        // Carrega os hologramas dos spawners existentes 2 segundos após o servidor ligar
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            for (PlacedSpawner s : placedSpawners.values()) {
                SpawnerType type = spawnerTypes.get(s.getTypeId());
                if (type != null) {
                    plugin.getHologramManager().updateOrCreateHologram(s, type);
                }
            }
        }, 40L);
    }

    public void loadConfigTypes() {
        spawnerTypes.clear();
        ConfigurationSection sec = plugin.getConfig().getConfigurationSection("spawners");
        if (sec == null) return;

        for (String key : sec.getKeys(false)) {
            String name = sec.getString(key + ".nome", "&eGerador");
            EntityType entityType = EntityType.valueOf(sec.getString(key + ".entidade", "PIG").toUpperCase());
            String reqRank = sec.getString(key + ".rank-necessario", "CARVAO");
            double cost = sec.getDouble(key + ".custo-compra", 100000.0);
            double valKill = sec.getDouble(key + ".valor-por-kill", 500.0);
            double tokenChance = sec.getDouble(key + ".chance-token", 0.15);

            spawnerTypes.put(key.toUpperCase(), new SpawnerType(key, name, entityType, reqRank, cost, valKill, tokenChance));
        }
    }

    private void startSpawnerTask() {
        new BukkitRunnable() {
            @Override
            public void run() {
                for (PlacedSpawner spawner : placedSpawners.values()) {
                    Location loc = spawner.getBukkitLocation();
                    if (loc == null || !loc.getWorld().isChunkLoaded(loc.getBlockX() >> 4, loc.getBlockZ() >> 4)) {
                        continue;
                    }

                    SpawnerType type = spawnerTypes.get(spawner.getTypeId());
                    if (type == null) continue;

                    // Garante que o holograma acima do bloco do spawner está ativo
                    plugin.getHologramManager().updateOrCreateHologram(spawner, type);

                    boolean playerNear = false;
                    for (Entity e : loc.getWorld().getNearbyEntities(loc, 16, 16, 16)) {
                        if (e.getType() == EntityType.PLAYER) {
                            playerNear = true;
                            break;
                        }
                    }
                    if (!playerNear) continue;

                    spawnOrStackEntity(loc, type, spawner.getStackAmount());
                }
            }
        }.runTaskTimer(plugin, 100L, 100L);
    }

    public void spawnOrStackEntity(Location spawnerLoc, SpawnerType type, long amountToAdd) {
        Location spawnPoint = spawnerLoc.clone().add(0.5, 1.0, 0.5);

        for (Entity nearby : spawnPoint.getWorld().getNearbyEntities(spawnPoint, 6, 4, 6)) {
            if (nearby instanceof LivingEntity && nearby.getType() == type.getEntityType() && !nearby.isDead()) {
                if (nearby.hasMetadata(META_STACK)) {
                    long currentStack = nearby.getMetadata(META_STACK).get(0).asLong();
                    long newStack = currentStack + amountToAdd;
                    updateEntityStack((LivingEntity) nearby, type, newStack);
                    return;
                }
            }
        }

        LivingEntity entity = (LivingEntity) spawnPoint.getWorld().spawnEntity(spawnPoint, type.getEntityType());
        entity.setRemoveWhenFarAway(true);
        entity.setCanPickupItems(false);
        entity.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, Integer.MAX_VALUE, 100, false, false));
        updateEntityStack(entity, type, amountToAdd);
    }

    public void updateEntityStack(LivingEntity entity, SpawnerType type, long stack) {
        entity.setMetadata(META_STACK, new FixedMetadataValue(plugin, stack));
        entity.setMetadata(META_TYPE, new FixedMetadataValue(plugin, type.getId()));
        entity.setCustomName(ChatColor.YELLOW + "" + ChatColor.BOLD + stack + "x " + type.getDisplayName());
        entity.setCustomNameVisible(true);
    }

    public boolean hasRequiredRank(String playerRank, String playerPrestige, String requiredRank) {
        if (playerPrestige != null && !playerPrestige.equalsIgnoreCase("NENHUM")) return true;
        int playerIdx = rankHierarchy.indexOf(playerRank.toUpperCase());
        int reqIdx = rankHierarchy.indexOf(requiredRank.toUpperCase());
        return playerIdx >= reqIdx && reqIdx != -1;
    }

    public double getPrestigeMultiplier(String prestigeId) {
        if (prestigeId == null) return 1.0;
        switch (prestigeId.toUpperCase()) {
            case "KRAMPUS": return 3.0;
            case "YETI": return 2.0;
            case "GRINCH": return 1.5;
            default: return 1.0;
        }
    }

    public ItemStack createSpawnerItem(SpawnerType type, long amount) {
        ItemStack item = new ItemStack(Material.MOB_SPAWNER, 1);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(type.getDisplayName());
        meta.setLore(Arrays.asList(
                ChatColor.GRAY + "Entidade: " + ChatColor.WHITE + type.getEntityType().name(),
                ChatColor.GRAY + "Quantia (Stack): " + ChatColor.YELLOW + amount + "x",
                ChatColor.GRAY + "Recompensa por Abate: " + ChatColor.GREEN + "$" + TWFormat.formatarDinheiro(type.getValuePerKill()),
                "",
                ChatColor.YELLOW + "Coloca no teu terreno para gerar mobs!",
                ChatColor.BLACK + "TWSPAWNER:" + type.getId() + ":" + amount
        ));
        item.setItemMeta(meta);
        return item;
    }

    public PlacedSpawner getSpawnerAt(Location loc) {
        return placedSpawners.get(PlacedSpawner.toKey(loc));
    }

    public PlacedSpawner findNearbySameSpawner(Location center, UUID owner, String typeId, int radius) {
        for (int x = -radius; x <= radius; x++) {
            for (int y = -radius; y <= radius; y++) {
                for (int z = -radius; z <= radius; z++) {
                    PlacedSpawner existing = getSpawnerAt(center.clone().add(x, y, z));
                    if (existing != null && existing.getOwnerUuid().equals(owner) && existing.getTypeId().equalsIgnoreCase(typeId)) {
                        return existing;
                    }
                }
            }
        }
        return null;
    }

    public void registerSpawner(PlacedSpawner spawner) {
        placedSpawners.put(spawner.getLocationKey(), spawner);
        saveSpawnerAsync(spawner);
    }

    public void removeSpawner(Location loc) {
        String key = PlacedSpawner.toKey(loc);
        placedSpawners.remove(key);
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> repository.deleteSpawner(key));
    }

    public void saveSpawnerAsync(PlacedSpawner spawner) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> repository.saveSpawner(spawner));
    }

    public void saveAllSync() {
        for (PlacedSpawner s : placedSpawners.values()) {
            repository.saveSpawner(s);
        }
    }

    public Map<String, SpawnerType> getSpawnerTypes() {
        return spawnerTypes;
    }

    public Map<String, PlacedSpawner> getPlacedSpawners() {
        return placedSpawners;
    }
}