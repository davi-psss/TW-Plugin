package br.com.tw.spawners.managers;

import br.com.tw.core.TWCorePlugin;
import br.com.tw.core.data.TWUser;
import br.com.tw.core.utils.TWFormat;
import br.com.tw.economia.TWEconomiaPlugin;
import br.com.tw.spawners.TWSpawnersPlugin;
import br.com.tw.spawners.data.SpawnerRepository;
import br.com.tw.spawners.models.PlacedSpawner;
import br.com.tw.spawners.models.SpawnerType;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Effect;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
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
    public static final String META_LOOT = "TW_MOB_LOOT";
    public static final String META_LOC = "TW_MOB_LOC";

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
        startAutoSellTask();

        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            for (PlacedSpawner s : placedSpawners.values()) {
                SpawnerType type = spawnerTypes.get(s.getTypeId());
                if (type != null) plugin.getHologramManager().updateOrCreateHologram(s, type);
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
                long nowSec = System.currentTimeMillis() / 1000L;

                for (PlacedSpawner spawner : placedSpawners.values()) {
                    SpawnerType type = spawnerTypes.get(spawner.getTypeId());
                    if (type == null) continue;

                    Location loc = spawner.getBukkitLocation();
                    if (loc == null || !loc.getWorld().isChunkLoaded(loc.getBlockX() >> 4, loc.getBlockZ() >> 4)) continue;

                    double currentBooster = spawner.hasActiveBooster() ? spawner.getBoosterMultiplier() : 1.0;
                    double baseCycle = 10.0 - ((spawner.getSpeedLevel() - 1) * 0.5);
                    double realCycleSeconds = Math.max(1.0, baseCycle / currentBooster);

                    if (nowSec - spawner.getLastSpawnTime() >= realCycleSeconds) {
                        spawner.setLastSpawnTime(nowSec);
                        long spawnAmount = spawner.getStackAmount() * spawner.getAmountLevel();

                        if (spawner.isVirtualMode()) {
                            double lootBonus = 1.0 + ((spawner.getLootLevel() - 1) * 0.10);
                            double moneyGenerated = type.getValuePerKill() * spawnAmount * lootBonus * currentBooster;

                            long tokensGenerated = 0;
                            long soulsGenerated = 0;
                            long xpGenerated = (long) (spawnAmount * 2 * currentBooster);

                            double tokenChanceBase = type.getTokenChance() * (1.0 + ((spawner.getLootLevel() - 1) * 0.05)) * currentBooster;
                            double soulChanceBase = 0.001 * currentBooster;

                            for (int i = 0; i < spawnAmount; i++) {
                                if (Math.random() < tokenChanceBase) tokensGenerated++;
                                if (Math.random() < soulChanceBase) soulsGenerated++;
                            }

                            spawner.addStoredMoney(moneyGenerated);
                            spawner.addStoredTokens(tokensGenerated);
                            spawner.addStoredXp(xpGenerated);
                            spawner.addStoredSouls(soulsGenerated);
                            spawner.addTotalKills(spawnAmount);

                            playVirtualEffects(loc, type);
                        } else {
                            boolean playerNear = false;
                            for (Entity e : loc.getWorld().getNearbyEntities(loc, 16, 16, 16)) {
                                if (e instanceof Player) { playerNear = true; break; }
                            }
                            if (playerNear) {
                                spawnOrStackEntity(loc, type, spawnAmount, spawner.getLootLevel());
                                spawner.addTotalKills(spawnAmount);
                            }
                        }
                    }
                    plugin.getHologramManager().updateOrCreateHologram(spawner, type);
                }
            }
        }.runTaskTimer(plugin, 20L, 20L);

        new BukkitRunnable() {
            @Override
            public void run() { saveAllSync(); }
        }.runTaskTimerAsynchronously(plugin, 72000L, 72000L);
    }

    private void startAutoSellTask() {
        new BukkitRunnable() {
            @Override
            public void run() {
                for (PlacedSpawner spawner : placedSpawners.values()) {
                    if (spawner.isVirtualMode() && spawner.isAutoSell() && (spawner.getStoredMoney() > 0 || spawner.getStoredTokens() > 0)) {
                        Player p = Bukkit.getPlayer(spawner.getOwnerUuid());
                        if (p != null && p.isOnline()) {
                            TWUser user = TWCorePlugin.getInstance().getUserManager().getUser(p.getUniqueId());
                            if (user != null) {
                                double prestigeMult = getPrestigeMultiplier(user.getPrestigeId());
                                double globalBooster = TWEconomiaPlugin.getInstance().getGlobalBooster();

                                user.addBalance(spawner.getStoredMoney() * prestigeMult * globalBooster);
                                user.addTokens(spawner.getStoredTokens());

                                spawner.setStoredMoney(0);
                                spawner.setStoredTokens(0);
                            }
                        }
                    }
                }
            }
        }.runTaskTimer(plugin, 200L, 200L);
    }

    private void playVirtualEffects(Location loc, SpawnerType type) {
        Location center = loc.clone().add(0.5, 1.0, 0.5);
        loc.getWorld().playEffect(center, Effect.LAVA_POP, 0);
        loc.getWorld().playSound(center, Sound.ORB_PICKUP, 0.05f, 1.5f);
    }

    public void spawnOrStackEntity(Location spawnerLoc, SpawnerType type, long amountToAdd, int lootLevel) {
        Location spawnPoint = spawnerLoc.clone().add(1.5, 0.0, 0.5);
        for (Entity nearby : spawnPoint.getWorld().getNearbyEntities(spawnPoint, 6, 4, 6)) {
            if (nearby instanceof LivingEntity && nearby.getType() == type.getEntityType() && !nearby.isDead()) {
                if (nearby.hasMetadata(META_STACK)) {
                    long currentStack = nearby.getMetadata(META_STACK).get(0).asLong();
                    updateEntityStack((LivingEntity) nearby, type, currentStack + amountToAdd, lootLevel, spawnPoint);
                    return;
                }
            }
        }
        LivingEntity entity = (LivingEntity) spawnPoint.getWorld().spawnEntity(spawnPoint, type.getEntityType());
        entity.setRemoveWhenFarAway(true);
        entity.setCanPickupItems(false);
        entity.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, Integer.MAX_VALUE, 250, false, false));
        entity.addPotionEffect(new PotionEffect(PotionEffectType.JUMP, Integer.MAX_VALUE, 250, false, false));
        updateEntityStack(entity, type, amountToAdd, lootLevel, spawnPoint);
    }

    public void updateEntityStack(LivingEntity entity, SpawnerType type, long stack, int lootLevel, Location anchor) {
        entity.setMetadata(META_STACK, new FixedMetadataValue(plugin, stack));
        entity.setMetadata(META_TYPE, new FixedMetadataValue(plugin, type.getId()));
        entity.setMetadata(META_LOOT, new FixedMetadataValue(plugin, lootLevel));
        entity.setMetadata(META_LOC, new FixedMetadataValue(plugin, anchor));
        entity.setCustomName(ChatColor.YELLOW + "§l" + stack + "x " + type.getDisplayName());
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

    // =========================================================================
    // MÉTODOS DE CRIAÇÃO DO ITEM DE SPAWNER (COM OVERLOADS PARA EVITAR ERROS)
    // =========================================================================

    // Método original (usado pelas compras na loja e /spawners - spawners virgens nível 1)
    public ItemStack createSpawnerItem(SpawnerType type, long amount) {
        return createSpawnerItem(type, amount, 1, 1, 1, false);
    }

    // Método que salva o Spawner atual (usado ao Desmontar)
    public ItemStack createSpawnerItem(PlacedSpawner spawner, SpawnerType type) {
        return createSpawnerItem(type, spawner.getStackAmount(), spawner.getSpeedLevel(), spawner.getLootLevel(), spawner.getAmountLevel(), spawner.isAutoSell());
    }

    // O gerador real de Itens com os dados ocultos
    private ItemStack createSpawnerItem(SpawnerType type, long amount, int speed, int loot, int amtLvl, boolean autoSell) {
        ItemStack item = new ItemStack(Material.MOB_SPAWNER, 1);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(type.getDisplayName());

        List<String> lore = new ArrayList<>();
        lore.add("§7Entidade: §f" + type.getEntityType().name());
        lore.add("§7Quantia (Stack): §e" + amount + "x");
        lore.add("§7Base por Abate: §a$" + TWFormat.formatarDinheiro(type.getValuePerKill()));
        lore.add("");

        if (speed > 1 || loot > 1 || amtLvl > 1 || autoSell) {
            lore.add("§d§lUpgrades Salvos:");
            if (speed > 1) lore.add("§7 ➥ Velocidade: §aLv." + speed);
            if (loot > 1) lore.add("§7 ➥ Fortuna: §aLv." + loot);
            if (amtLvl > 1) lore.add("§7 ➥ Quantidade: §aLv." + amtLvl);
            if (autoSell) lore.add("§7 ➥ Auto-Sell: §aAtivo");
            lore.add("");
        }

        lore.add("§eColoca no teu terreno para gerar mobs!");
        lore.add("§0TWSPAWNER:" + type.getId() + ":" + amount + ":" + speed + ":" + loot + ":" + amtLvl + ":" + autoSell);

        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    public ItemStack createBoosterItem(double multiplier, int minutes) {
        ItemStack item = new ItemStack(Material.NETHER_STAR, 1);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName("§b§lBooster de Spawner §7(" + multiplier + "x)");
        meta.setLore(Arrays.asList(
                "§7Acelera o tempo de geração e",
                "§7multiplica os lucros de um",
                "§7gerador específico.",
                "",
                "§7Duração: §f" + minutes + " minutos",
                "",
                "§eClica com o botão direito num Spawner!",
                "§0TWBOOSTER:" + multiplier + ":" + minutes
        ));
        item.setItemMeta(meta);
        return item;
    }

    public ItemStack createSoulItem(long amount) {
        ItemStack item = new ItemStack(Material.NETHER_STAR, 1);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName("§d§lAlma de Monstro");
        meta.setLore(Arrays.asList(
                "§7Uma essência raríssima dropada",
                "§7pelos monstros nos spawners.",
                "§7Usa-a para trocar por itens místicos!",
                "",
                "§7Quantia: §f" + amount + "x"
        ));
        item.setItemMeta(meta);
        return item;
    }

    public PlacedSpawner getSpawnerAt(Location loc) { return placedSpawners.get(PlacedSpawner.toKey(loc)); }

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

    public Map<String, SpawnerType> getSpawnerTypes() { return spawnerTypes; }
    public Map<String, PlacedSpawner> getPlacedSpawners() { return placedSpawners; }
}