package br.com.tw.farms.managers;

import br.com.tw.core.TWCorePlugin;
import br.com.tw.core.data.TWUser;
import br.com.tw.core.utils.TWAsyncTaskQueue;
import br.com.tw.core.utils.TWConfigValidation;
import br.com.tw.core.utils.TWItemTags;
import br.com.tw.economia.TWEconomiaPlugin;
import br.com.tw.farms.TWFarmsPlugin;
import br.com.tw.farms.data.FarmRepository;
import br.com.tw.farms.models.FarmStorage;
import br.com.tw.farms.models.FarmType;
import br.com.tw.farms.models.PhysicalCrop;
import br.com.tw.farms.models.PlacedFarm;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.data.Ageable;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

public final class FarmManager {
    private final TWFarmsPlugin plugin;
    private final FarmRepository repository;
    private final TWAsyncTaskQueue saveQueue;
    private final Map<String, FarmType> farmTypes = new java.util.LinkedHashMap<>();
    private final Map<String, PlacedFarm> placedFarms = new ConcurrentHashMap<>();
    private final Map<String, PhysicalCrop> physicalCrops = new ConcurrentHashMap<>();
    private final Map<String, FarmStorage> storage = new ConcurrentHashMap<>();
    private final Map<String, PlacedFarm> pendingFarmSaves = new ConcurrentHashMap<>();
    private final Map<String, FarmStorage> pendingStorageSaves = new ConcurrentHashMap<>();
    private final java.util.Set<String> loggedFailures = ConcurrentHashMap.newKeySet();
    private Iterator<PlacedFarm> hologramIterator;
    private Iterator<PhysicalCrop> cropCleanupIterator;
    private Iterator<PlacedFarm> autoSellFarmIterator;
    private Iterator<FarmStorage> autoSellStorageIterator;
    private long lastAutoSell;
    private long nextCropCleanup;
    private boolean autoSellPassActive;

    public FarmManager(TWFarmsPlugin plugin, FarmRepository repository) {
        this.plugin = plugin;
        this.repository = repository;
        this.saveQueue = new TWAsyncTaskQueue(plugin, "tw-farms-db");
        loadFarmTypes();
        validateMechanicsConfig();
        placedFarms.putAll(repository.loadFarms());
        physicalCrops.putAll(repository.loadCrops());
        storage.putAll(repository.loadStorage());
        hologramIterator = placedFarms.values().iterator();
        startTasks();
    }

    private void loadFarmTypes() {
        farmTypes.clear();
        ConfigurationSection section = plugin.getConfig().getConfigurationSection("farms");
        if (section == null || section.getKeys(false).isEmpty()) {
            throw new IllegalArgumentException("Configuração farms inválida: defina ao menos uma cultura.");
        }
        List<String> ranks = TWCorePlugin.getInstance().getRankHierarchy();
        for (String id : section.getKeys(false)) {
            String path = "farms." + id + ".";
            String name = TWConfigValidation.requireText(plugin, path + "nome", section.getString(id + ".nome"));
            Material material = requireMaterial(path + "material", section.getString(id + ".material"));
            Material plantingMaterial = requireMaterial(path + "planting-item",
                    section.getString(id + ".planting-item", defaultPlantingItem(id, material)));
            Material product = requireMaterial(path + "produto", section.getString(id + ".produto"));
            List<Material> plantMaterials = readMaterials(path + "plant-materials",
                    section.getStringList(id + ".plant-materials"));
            List<Material> harvestMaterials = readMaterials(path + "harvest-materials",
                    section.getStringList(id + ".harvest-materials"));
            String rank = TWConfigValidation.requireText(plugin, path + "rank-necessario",
                    section.getString(id + ".rank-necessario"));
            if (!ranks.contains(rank.toUpperCase(Locale.ROOT))) {
                throw new IllegalArgumentException("Rank desconhecido em " + path + "rank-necessario: " + rank);
            }
            double cost = TWConfigValidation.requireFiniteAtLeast(plugin, path + "custo-compra",
                    section.getDouble(id + ".custo-compra", Double.NaN), 0.0);
            double sellValue = TWConfigValidation.requireFiniteAtLeast(plugin, path + "valor-venda",
                    section.getDouble(id + ".valor-venda", Double.NaN), 0.0);
            int cycle = TWConfigValidation.requireIntRange(plugin, path + "ciclo-segundos",
                    section.getInt(id + ".ciclo-segundos", -1), 1, 86400);
            long fertilizer = section.getLong(id + ".fertilizante-por-ciclo", 1L);
            if (fertilizer < 1) throw new IllegalArgumentException("fertilizante-por-ciclo deve ser positivo em " + path);
            farmTypes.put(id.toUpperCase(Locale.ROOT), new FarmType(id, name, material, plantingMaterial, product,
                    plantMaterials, harvestMaterials, rank, cost, sellValue, cycle, fertilizer));
        }
    }

    private String defaultPlantingItem(String id, Material legacyMaterial) {
        switch (id.toUpperCase(Locale.ROOT)) {
            case "TRIGO": return "WHEAT_SEEDS";
            case "CENOURA": return "CARROT";
            case "BATATA": return "POTATO";
            case "CANA": return "SUGAR_CANE";
            case "CACTO": return "CACTUS";
            case "MELANCIA": return "MELON_SEEDS";
            case "FUNGUS": return "NETHER_WART";
            default: return legacyMaterial.name();
        }
    }

    private Material requireMaterial(String path, String name) {
        String normalizedName = name == null ? null : normalizeLegacyMaterial(path, name);
        Material material = normalizedName == null ? null : Material.matchMaterial(normalizedName);
        if (material == null || material == Material.AIR) {
            throw new IllegalArgumentException("Material inválido em " + path + ": " + name);
        }
        return material;
    }

    private String normalizeLegacyMaterial(String path, String name) {
        String normalized = name.toUpperCase(Locale.ROOT);
        boolean cropBlock = path.contains(".plant-materials") || path.contains(".harvest-materials");
        switch (normalized) {
            case "SEEDS":
                return "WHEAT_SEEDS";
            case "CARROT_ITEM":
                return "CARROT";
            case "POTATO_ITEM":
                return "POTATO";
            case "CROPS":
                return "WHEAT";
            case "CARROT":
                return cropBlock ? "CARROTS" : "CARROT";
            case "POTATO":
                return cropBlock ? "POTATOES" : "POTATO";
            case "NETHER_STALK":
            case "NETHER_WARTS":
                return "NETHER_WART";
            case "SUGAR_CANE_BLOCK":
                return "SUGAR_CANE";
            case "MELON_BLOCK":
                return "MELON";
            case "MELON":
                return path.endsWith(".material") || path.endsWith(".produto") ? "MELON_SLICE" : "MELON";
            default:
                return normalized;
        }
    }

    private List<Material> readMaterials(String path, List<String> names) {
        if (names == null || names.isEmpty()) throw new IllegalArgumentException(path + " não pode ficar vazio.");
        List<Material> result = new ArrayList<>();
        for (String name : names) result.add(requireMaterial(path, name));
        return result;
    }

    private void validateMechanicsConfig() {
        TWConfigValidation.requireFiniteAtLeast(plugin, "farm.base-silo-capacity",
                plugin.getConfig().getDouble("farm.base-silo-capacity", 50000.0), 1.0);
        TWConfigValidation.requireIntRange(plugin, "farm.max-process-per-cycle",
                plugin.getConfig().getInt("farm.max-process-per-cycle", 250), 1, 10000);
        TWConfigValidation.requireIntRange(plugin, "farm.autosave-ticks",
                plugin.getConfig().getInt("farm.autosave-ticks", 1200), 20, 72000);
        TWConfigValidation.requireIntRange(plugin, "farm.physical-crop-harvest-radius",
                plugin.getConfig().getInt("farm.physical-crop-harvest-radius", 1), 0, 5);
        TWConfigValidation.requireProbability(plugin, "farm.physical-golden-chance",
                plugin.getConfig().getDouble("farm.physical-golden-chance", 0.01));
        TWConfigValidation.requireIntRange(plugin, "farm.growth-booster-max-duration-seconds",
                plugin.getConfig().getInt("farm.growth-booster-max-duration-seconds", 3600), 1, 86400);

        TWConfigValidation.requireIntRange(plugin, "upgrades.max-level",
                plugin.getConfig().getInt("upgrades.max-level", 25), 1, 1000);
        for (String key : Arrays.asList("speed-token-base-cost", "storage-token-base-cost",
                "quality-token-base-cost", "auto-sell-token-cost")) {
            TWConfigValidation.requireFiniteAtLeast(plugin, "upgrades." + key,
                    plugin.getConfig().getDouble("upgrades." + key, Double.NaN), 1.0);
        }
        for (int soil = 1; soil <= 3; soil++) {
            TWConfigValidation.requireFiniteAtLeast(plugin, "fertilizer.growth-multiplier-by-soil." + soil,
                    plugin.getConfig().getDouble("fertilizer.growth-multiplier-by-soil." + soil, Double.NaN), 0.01);
            TWConfigValidation.requireProbability(plugin, "fertilizer.golden-chance-by-soil." + soil,
                    plugin.getConfig().getDouble("fertilizer.golden-chance-by-soil." + soil, Double.NaN));
        }
        TWConfigValidation.requireFiniteAtLeast(plugin, "shop.confirmation-price-threshold",
                plugin.getConfig().getDouble("shop.confirmation-price-threshold", 250000.0), 0.0);
        TWConfigValidation.requireFiniteAtLeast(plugin, "shop.harvester-hoe.price",
                plugin.getConfig().getDouble("shop.harvester-hoe.price", Double.NaN), 0.0);
        ConfigurationSection fertilizer = plugin.getConfig().getConfigurationSection("shop.fertilizer");
        if (fertilizer == null || fertilizer.getKeys(false).isEmpty()) {
            throw new IllegalArgumentException("shop.fertilizer deve ter ao menos um pacote.");
        }
        for (String key : fertilizer.getKeys(false)) {
            String path = "shop.fertilizer." + key;
            TWConfigValidation.requireFiniteAtLeast(plugin, path + ".price",
                    plugin.getConfig().getDouble(path + ".price", Double.NaN), 0.0);
            TWConfigValidation.requireIntRange(plugin, path + ".soil-level",
                    plugin.getConfig().getInt(path + ".soil-level", 0), 1, 3);
            if (plugin.getConfig().getLong(path + ".units", 0L) < 1) {
                throw new IllegalArgumentException(path + ".units deve ser positivo.");
            }
        }
        ConfigurationSection boosters = plugin.getConfig().getConfigurationSection("shop.growth-boosters");
        if (boosters == null || boosters.getKeys(false).isEmpty()) {
            throw new IllegalArgumentException("shop.growth-boosters deve ter ao menos um booster.");
        }
        for (String key : boosters.getKeys(false)) {
            String path = "shop.growth-boosters." + key;
            double multiplier = TWConfigValidation.requireFiniteAtLeast(plugin, path + ".multiplier",
                    plugin.getConfig().getDouble(path + ".multiplier", Double.NaN), 1.01);
            long duration = plugin.getConfig().getLong(path + ".duration-seconds", 0L);
            if (multiplier > 10.0 || duration < 1 || duration > Integer.MAX_VALUE) {
                throw new IllegalArgumentException("Booster inválido em " + path + ".");
            }
            TWConfigValidation.requireFiniteAtLeast(plugin, path + ".price",
                    plugin.getConfig().getDouble(path + ".price", Double.NaN), 0.0);
        }
        for (String recipe : Arrays.asList("xp", "souls")) {
            String path = "alchemy." + recipe + ".";
            String resource = "souls".equals(recipe) ? "souls" : "xp";
            if (plugin.getConfig().getLong(path + resource, 0L) < 1
                    || plugin.getConfig().getLong(path + "material-amount", 0L) < 1
                    || plugin.getConfig().getLong(path + "fertilizer-units", 0L) < 1) {
                throw new IllegalArgumentException("Receita de alquimia incompleta em " + path + ".");
            }
            requireMaterial(path + "material", plugin.getConfig().getString(path + "material"));
            TWConfigValidation.requireIntRange(plugin, path + "soil-level",
                    plugin.getConfig().getInt(path + "soil-level", 0), 1, 3);
        }
    }

    private void startTasks() {
        new BukkitRunnable() {
            @Override
            public void run() {
                if (!autoSellPassActive && System.currentTimeMillis() / 1000L - lastAutoSell >= 10) {
                    lastAutoSell = System.currentTimeMillis() / 1000L;
                    autoSellPassActive = true;
                    autoSellFarmIterator = placedFarms.values().iterator();
                    autoSellStorageIterator = storage.values().iterator();
                }
                processAutoSellBatch();
                updateHolograms();
                cleanupPhysicalCrops();
            }
        }.runTaskTimer(plugin, 20L, 20L);
        long autosaveTicks = TWConfigValidation.requireIntRange(plugin, "farm.autosave-ticks",
                plugin.getConfig().getInt("farm.autosave-ticks", 1200), 20, 72000);
        new BukkitRunnable() {
            @Override
            public void run() { saveAllAsync(); }
        }.runTaskTimer(plugin, autosaveTicks, autosaveTicks);
    }

    private void processAutoSellBatch() {
        if (!autoSellPassActive) return;
        int limit = plugin.getConfig().getInt("farm.max-process-per-cycle", 250);
        int processed = 0;
        while (processed < limit && autoSellFarmIterator != null && autoSellFarmIterator.hasNext()) {
            PlacedFarm farm = autoSellFarmIterator.next();
            if (farm.isAutoSell() && farm.getStoredTotal() > 0) {
                Player player = Bukkit.getPlayer(farm.getOwnerUuid());
                if (player != null && player.isOnline()) {
                    try {
                        sellFarmStorage(player, farm);
                    } catch (RuntimeException e) {
                        logSaleFailure(farm.getLocationKey(), e);
                    }
                }
            }
            processed++;
        }
        while (processed < limit && autoSellStorageIterator != null && autoSellStorageIterator.hasNext()) {
            FarmStorage silo = autoSellStorageIterator.next();
            if (silo.getStoredTotal() > 0 && silo.isAutoSell()) {
                Player player = Bukkit.getPlayer(silo.getOwnerUuid());
                if (player != null && player.isOnline()) {
                    try {
                        sellPhysicalStorage(player, silo);
                    } catch (RuntimeException e) {
                        logSaleFailure(silo.getOwnerUuid() + ";" + silo.getTypeId(), e);
                    }
                }
            }
            processed++;
        }
        if ((autoSellFarmIterator == null || !autoSellFarmIterator.hasNext())
                && (autoSellStorageIterator == null || !autoSellStorageIterator.hasNext())) {
            autoSellPassActive = false;
        }
    }

    private void logSaleFailure(String key, RuntimeException error) {
        if (loggedFailures.add("sale;" + key)) {
            plugin.getLogger().log(Level.SEVERE, "Falha ao vender automaticamente a safra " + key + ".", error);
        }
    }

    private void updateHolograms() {
        int limit = plugin.getConfig().getInt("farm.max-process-per-cycle", 250);
        int updated = 0;
        while (updated < limit && !placedFarms.isEmpty()) {
            if (hologramIterator == null || !hologramIterator.hasNext()) {
                hologramIterator = placedFarms.values().iterator();
                if (!hologramIterator.hasNext()) break;
            }
            PlacedFarm farm = hologramIterator.next();
            if (placedFarms.get(farm.getLocationKey()) != farm) continue;
            FarmType type = farmTypes.get(farm.getTypeId());
            if (type != null) plugin.getHologramManager().updateOrCreate(farm, type);
            updated++;
        }
    }

    private void cleanupPhysicalCrops() {
        long now = System.currentTimeMillis() / 1000L;
        if (cropCleanupIterator == null) {
            if (now < nextCropCleanup) return;
            cropCleanupIterator = physicalCrops.values().iterator();
        }
        int limit = plugin.getConfig().getInt("farm.max-process-per-cycle", 250);
        int checked = 0;
        while (checked++ < limit && cropCleanupIterator.hasNext()) {
            PhysicalCrop crop = cropCleanupIterator.next();
            if (!isPhysicalCropPresent(crop)) unregisterCrop(crop);
        }
        if (!cropCleanupIterator.hasNext()) {
            cropCleanupIterator = null;
            nextCropCleanup = now + 60L;
        }
    }

    private boolean isPhysicalCropPresent(PhysicalCrop crop) {
        String[] parts = crop.getLocationKey().split(";", 4);
        if (parts.length != 4) return true;
        org.bukkit.World world = Bukkit.getWorld(parts[0]);
        if (world == null) return true;
        try {
            int x = Integer.parseInt(parts[1]);
            int y = Integer.parseInt(parts[2]);
            int z = Integer.parseInt(parts[3]);
            if (!world.isChunkLoaded(x >> 4, z >> 4)) return true;
            Material material = world.getBlockAt(x, y, z).getType();
            FarmType type = farmTypes.get(crop.getTypeId());
            return type != null && (type.getPlantMaterials().contains(material)
                    || type.getHarvestMaterials().contains(material));
        } catch (NumberFormatException e) {
            plugin.getLogger().log(Level.WARNING, "Localização de cultura inválida: "
                    + crop.getLocationKey(), e);
            return true;
        }
    }

    public boolean hasRequiredRank(String rank, String prestige, String required) {
        if (prestige != null && !prestige.equalsIgnoreCase("NENHUM")) return true;
        List<String> hierarchy = TWCorePlugin.getInstance().getRankHierarchy();
        int playerRank = hierarchy.indexOf(rank.toUpperCase(Locale.ROOT));
        int requiredRank = hierarchy.indexOf(required.toUpperCase(Locale.ROOT));
        return requiredRank >= 0 && playerRank >= requiredRank;
    }

    public long getStorageCapacity(PlacedFarm farm) {
        long base = plugin.getConfig().getLong("farm.base-silo-capacity", 50000L);
        if (base < 1) throw new IllegalStateException("farm.base-silo-capacity deve ser positivo.");
        return farm.getCapacity(base);
    }

    public ItemStack createFarmItem(FarmType type, long stack) {
        ItemStack item = new ItemStack(type.getMaterial());
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', type.getDisplayName()));
        meta.setLore(Arrays.asList("§7Tipo: §f" + type.getId(), "§7Stack: §e" + stack + "x",
                "§eColoca no teu terreno para produzir.", "§0TWFARM:" + type.getId() + ":" + stack));
        item.setItemMeta(meta);
        Map<String, Object> tags = new HashMap<>();
        tags.put("tw_farm_type", type.getId());
        tags.put("tw_farm_stack", stack);
        return TWItemTags.set(item, tags);
    }

    public ItemStack createPlantingItems(FarmType type, long amount) {
        if (amount < 1 || amount > 64) throw new IllegalArgumentException("Quantidade de sementes inválida.");
        return new ItemStack(type.getPlantingMaterial(), (int) amount);
    }

    public ItemStack createFarmItem(PlacedFarm farm) {
        FarmType type = farmTypes.get(farm.getTypeId());
        if (type == null) throw new IllegalArgumentException("Tipo de farm desconhecido: " + farm.getTypeId());
        ItemStack item = createFarmItem(type, farm.getStackAmount());
        Map<String, Object> tags = new HashMap<>();
        tags.put("tw_farm_type", farm.getTypeId());
        tags.put("tw_farm_stack", farm.getStackAmount());
        tags.put("tw_farm_fertilizer", farm.getFertilizer());
        tags.put("tw_farm_soil", farm.getSoilLevel());
        tags.put("tw_farm_stored", farm.getStored());
        tags.put("tw_farm_golden", farm.getStoredGolden());
        tags.put("tw_farm_harvested", farm.getTotalHarvested());
        tags.put("tw_farm_sold", farm.getTotalSold());
        tags.put("tw_farm_speed", farm.getSpeedLevel());
        tags.put("tw_farm_storage", farm.getStorageLevel());
        tags.put("tw_farm_quality", farm.getQualityLevel());
        tags.put("tw_farm_auto_sell", farm.isAutoSell());
        tags.put("tw_farm_auto_sell_unlocked", farm.isAutoSellUnlocked());
        tags.put("tw_farm_booster", farm.getBoosterMultiplier());
        tags.put("tw_farm_booster_end", farm.getBoosterEndTime());
        tags.put("tw_farm_last_harvest", farm.getLastHarvestTime());
        return TWItemTags.set(item, tags);
    }

    public PlacedFarm farmFromItem(ItemStack item, Location location, Player owner) {
        String typeId = TWItemTags.getString(item, "tw_farm_type");
        long stack = TWItemTags.getLong(item, "tw_farm_stack", 0);
        if (typeId == null || stack <= 0 || !farmTypes.containsKey(typeId.toUpperCase(Locale.ROOT))) return null;
        PlacedFarm farm = new PlacedFarm(PlacedFarm.toKey(location), owner.getUniqueId(), owner.getName(), typeId, stack,
                TWItemTags.getLong(item, "tw_farm_fertilizer", 0),
                TWItemTags.getInt(item, "tw_farm_soil", 1),
                TWItemTags.getLong(item, "tw_farm_stored", 0),
                TWItemTags.getLong(item, "tw_farm_golden", 0),
                TWItemTags.getLong(item, "tw_farm_harvested", 0),
                TWItemTags.getLong(item, "tw_farm_sold", 0),
                TWItemTags.getInt(item, "tw_farm_speed", 1),
                TWItemTags.getInt(item, "tw_farm_storage", 1),
                TWItemTags.getInt(item, "tw_farm_quality", 1),
                TWItemTags.getBoolean(item, "tw_farm_auto_sell", false),
                TWItemTags.getBoolean(item, "tw_farm_auto_sell_unlocked", false),
                TWItemTags.getDouble(item, "tw_farm_booster", 1.0),
                TWItemTags.getLong(item, "tw_farm_booster_end", 0),
                TWItemTags.getLong(item, "tw_farm_last_harvest", 0),
                System.currentTimeMillis() / 1000L);
        getStorageCapacity(farm);
        return farm;
    }

    public ItemStack createFertilizerItem(long units, int soil) {
        ItemStack item = new ItemStack(Material.BONE_MEAL);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(soil == 3 ? "§dSolo Mágico" : soil == 2 ? "§6Adubo Enriquecido" : "§aAdubo Normal");
        meta.setLore(Arrays.asList("§7Unidades: §e" + units, "§7Nível de solo: §f" + soil,
                "§eUsa na farm para abastecer.", "§0TWFERTILIZER:" + units + ":" + soil));
        item.setItemMeta(meta);
        Map<String, Object> tags = new HashMap<>();
        tags.put("tw_farm_fertilizer", units);
        tags.put("tw_farm_soil", soil);
        return TWItemTags.set(item, tags);
    }

    public ItemStack createBoosterItem(double multiplier, long durationSeconds) {
        ItemStack item = new ItemStack(Material.SUGAR);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName("§bBooster de Crescimento §ex" + multiplier);
        meta.setLore(Arrays.asList("§7Duração: §f" + durationSeconds / 60 + " minutos",
                "§eUsa na farm para ativar.", "§0TWFARMBOOSTER:" + multiplier + ":" + durationSeconds));
        item.setItemMeta(meta);
        Map<String, Object> tags = new HashMap<>();
        tags.put("tw_farm_booster_mult", multiplier);
        tags.put("tw_farm_booster_seconds", (int) durationSeconds);
        return TWItemTags.set(item, tags);
    }

    public boolean depositWithdrawnItems(Player player, FarmStorage onlyStorage) {
        long deposited = 0;
        for (int slot = 0; slot < player.getInventory().getSize(); slot++) {
            ItemStack item = player.getInventory().getItem(slot);
            if (item == null || item.getType() == Material.AIR) continue;
            for (FarmStorage silo : getPlayerStorage(player.getUniqueId())) {
                if (onlyStorage != null && silo != onlyStorage) continue;
                FarmType type = farmTypes.get(silo.getTypeId());
                if (type == null || item.getType() != type.getProduct()) continue;
                boolean tagged = TWItemTags.getBoolean(item, "tw_farm_withdrawn", false)
                        && type.getId().equalsIgnoreCase(TWItemTags.getString(item, "tw_farm_crop_type"));
                boolean legacy = isLegacyWithdrawnItem(item, type);
                if (!tagged && !legacy) continue;
                boolean golden = tagged
                        ? TWItemTags.getBoolean(item, "tw_farm_golden", false)
                        : item.getItemMeta().getDisplayName().startsWith("§6Safra Dourada:");
                int amount = item.getAmount();
                silo.restoreStored(golden ? 0 : amount, golden ? amount : 0);
                deposited = Math.addExact(deposited, amount);
                player.getInventory().setItem(slot, null);
                saveStorageAsync(silo);
                break;
            }
        }
        if (deposited > 0) {
            player.updateInventory();
            player.sendMessage(ChatColor.GREEN + "Devolveste " + deposited
                    + " item(ns) recolhido(s) ao armazém.");
            return true;
        }
        player.sendMessage(ChatColor.YELLOW + "Não tens itens recolhidos do armazém para devolver.");
        return false;
    }

    private boolean isLegacyWithdrawnItem(ItemStack item, FarmType type) {
        if (!item.hasItemMeta()) return false;
        ItemMeta meta = item.getItemMeta();
        if (!meta.hasDisplayName() || !meta.hasLore() || meta.getLore().size() != 1) return false;
        String name = ChatColor.stripColor(ChatColor.translateAlternateColorCodes('&', type.getDisplayName()));
        String displayName = meta.getDisplayName();
        return (displayName.equals("§eColheita: " + name) || displayName.equals("§6Safra Dourada: " + name))
                && (meta.getLore().get(0).equals("§7Colheita recolhida do silo virtual.")
                || meta.getLore().get(0).equals("§7Valiosa: vale 5x na venda da farm."));
    }

    public ItemStack createHarvesterHoe() {
        ItemStack hoe = new ItemStack(Material.DIAMOND_HOE);
        ItemMeta meta = hoe.getItemMeta();
        meta.setDisplayName("§a§lEnxada do Fazendeiro");
        meta.setLore(Arrays.asList("§7Colhe as culturas maduras registadas próximas.",
                "§7A colheita segue para o Armazém da Farm."));
        hoe.setItemMeta(meta);
        Map<String, Object> tags = new HashMap<>();
        tags.put("tw_farm_harvester", true);
        tags.put("tw_farm_hoe_harvests", 0L);
        return TWItemTags.set(hoe, tags);
    }

    public ItemStack createCropItem(FarmType type, long amount) {
        ItemStack item = new ItemStack(type.getProduct(), (int) Math.min(64L, amount));
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName("§eColheita: " + ChatColor.stripColor(
                ChatColor.translateAlternateColorCodes('&', type.getDisplayName())));
        meta.setLore(Collections.singletonList("§7Fragmento de safra recolhido no armazém."));
        item.setItemMeta(meta);
        return item;
    }

    public void registerFarm(PlacedFarm farm) {
        placedFarms.put(farm.getLocationKey(), farm);
        saveFarmAsync(farm);
    }

    public PlacedFarm getFarmAt(Location location) {
        return placedFarms.get(PlacedFarm.toKey(location));
    }

    public PlacedFarm findNearbySameFarm(Location center, UUID owner, String type, int radius) {
        for (int x = -radius; x <= radius; x++) for (int y = -radius; y <= radius; y++) {
            for (int z = -radius; z <= radius; z++) {
                PlacedFarm farm = getFarmAt(center.clone().add(x, y, z));
                if (farm != null && farm.getOwnerUuid().equals(owner) && farm.getTypeId().equalsIgnoreCase(type)) {
                    return farm;
                }
            }
        }
        return null;
    }

    public void removeFarm(Location location) {
        String key = PlacedFarm.toKey(location);
        placedFarms.remove(key);
        pendingFarmSaves.remove(key);
        saveQueue.submit("Falha ao remover farm " + key + ".", () -> repository.deleteFarm(key));
    }

    public void registerCrop(PhysicalCrop crop) {
        physicalCrops.put(crop.getLocationKey(), crop);
        saveQueue.submit("Falha ao salvar planta " + crop.getLocationKey() + ".", () -> repository.saveCrop(crop));
    }

    public boolean capturePhysicalDrop(Location location, ItemStack drop) {
        PhysicalCrop crop = findCropNear(location);
        if (crop == null) return false;
        FarmType type = farmTypes.get(crop.getTypeId());
        if (type == null) return false;
        if (drop.getType() == type.getProduct()) {
            org.bukkit.Bukkit.getPluginManager().callEvent(
                    new br.com.tw.core.events.TWFarmHarvestEvent(crop.getOwnerUuid(), type.getId(),
                            drop.getAmount(), crop.getLocationKey()));
            FarmStorage silo = getStorage(crop.getOwnerUuid(), type.getId());
            double chance = plugin.getConfig().getDouble("farm.physical-golden-chance", 0.01);
            long golden = 0;
            for (int i = 0; i < drop.getAmount(); i++) {
                if (Math.random() < chance) golden++;
            }
            silo.harvest(drop.getAmount() - golden, golden);
            saveStorageAsync(silo);
            return true;
        }
        return drop.getType() == type.getPlantingMaterial();
    }

    private PhysicalCrop findCropNear(Location location) {
        if (location.getWorld() == null) return null;
        int blockX = location.getBlockX();
        int blockY = location.getBlockY();
        int blockZ = location.getBlockZ();
        for (int y = 1; y >= -4; y--) {
            for (int x = -1; x <= 1; x++) {
                for (int z = -1; z <= 1; z++) {
                    PhysicalCrop crop = physicalCrops.get(location.getWorld().getName() + ";"
                            + (blockX + x) + ";" + (blockY + y) + ";" + (blockZ + z));
                    if (crop != null) return crop;
                }
            }
        }
        return null;
    }

    public double getGrowthMultiplier(PhysicalCrop crop) {
        FarmStorage silo = getStorage(crop.getOwnerUuid(), crop.getTypeId());
        long now = System.currentTimeMillis() / 1000L;
        if (silo.getGrowthBoostEndTime() <= now) {
            if (silo.getGrowthBoostEndTime() != 0L || silo.getGrowthMultiplier() != 1.0) {
                silo.setGrowthBooster(1.0, 0L);
                saveStorageAsync(silo);
            }
            return 1.0;
        }
        return silo.getGrowthMultiplier();
    }

    public boolean applyGrowthBooster(PhysicalCrop crop, double multiplier, long durationSeconds) {
        if (!Double.isFinite(multiplier) || multiplier <= 1.0 || durationSeconds <= 0) {
            throw new IllegalArgumentException("Booster de crescimento inválido.");
        }
        FarmStorage silo = getStorage(crop.getOwnerUuid(), crop.getTypeId());
        long now = System.currentTimeMillis() / 1000L;
        long maximumDuration = plugin.getConfig().getLong("farm.growth-booster-max-duration-seconds", 3600L);
        boolean changed = silo.activateGrowthBooster(multiplier, durationSeconds, now, maximumDuration);
        if (!changed) return false;
        saveStorageAsync(silo);
        return true;
    }

    public boolean accelerateGrowth(Block block, BlockState proposedState) {
        if (isColumnCrop(proposedState.getType())) {
            PhysicalCrop crop = findCropNear(block.getLocation());
            if (crop == null) return false;
            double multiplier = getGrowthMultiplier(crop);
            if (multiplier <= 1.0) return false;
            int heightBefore = getColumnHeight(block, proposedState.getType());
            scheduleColumnGrowthBoost(block, proposedState.getType(), heightBefore,
                    Math.min(3, heightBefore + (int) Math.ceil(multiplier)));
            return false;
        }

        PhysicalCrop crop = getCropAt(block.getLocation());
        if (crop == null || !isAgeCrop(block.getType())) return false;
        double multiplier = getGrowthMultiplier(crop);
        if (multiplier <= 1.0) return false;
        if (!(block.getBlockData() instanceof Ageable) || !(proposedState.getBlockData() instanceof Ageable)) {
            return false;
        }
        Ageable currentData = (Ageable) block.getBlockData();
        Ageable proposedData = (Ageable) proposedState.getBlockData();
        int currentAge = currentData.getAge();
        int proposedAge = proposedData.getAge();
        int advancedAge = Math.min(currentData.getMaximumAge(),
                currentAge + (int) Math.ceil(Math.max(1, proposedAge - currentAge) * multiplier));
        currentData.setAge(advancedAge);
        block.setBlockData(currentData, false);
        return true;
    }

    private boolean isColumnCrop(Material material) {
        return material == Material.CACTUS || material == Material.SUGAR_CANE;
    }

    private int getColumnHeight(Block block, Material material) {
        Block base = block;
        while (base.getRelative(0, -1, 0).getType() == material) base = base.getRelative(0, -1, 0);
        int height = 0;
        while (base.getRelative(0, height, 0).getType() == material) height++;
        return height;
    }

    private void scheduleColumnGrowthBoost(Block block, Material material, int heightBefore, int targetHeight) {
        if (heightBefore < 1 || targetHeight <= heightBefore) return;
        Location location = block.getLocation();
        Bukkit.getScheduler().runTask(plugin, () -> {
            if (!location.getWorld().isChunkLoaded(location.getBlockX() >> 4, location.getBlockZ() >> 4)) return;
            Block growthBlock = location.getBlock();
            int heightAfter = getColumnHeight(growthBlock, material);
            if (heightAfter <= heightBefore || heightAfter >= targetHeight) return;
            Block top = growthBlock;
            while (top.getRelative(0, 1, 0).getType() == material) top = top.getRelative(0, 1, 0);
            Block next = top.getRelative(0, 1, 0);
            while (heightAfter < targetHeight && next.getType() == Material.AIR) {
                if (material == Material.CACTUS && !hasClearCactusSides(next)) return;
                next.setType(material);
                heightAfter++;
                top = next;
                next = top.getRelative(0, 1, 0);
            }
        });
    }

    private boolean hasClearCactusSides(Block block) {
        return block.getRelative(1, 0, 0).getType() == Material.AIR
                && block.getRelative(-1, 0, 0).getType() == Material.AIR
                && block.getRelative(0, 0, 1).getType() == Material.AIR
                && block.getRelative(0, 0, -1).getType() == Material.AIR;
    }

    public PhysicalCrop getCropAt(Location location) {
        return physicalCrops.get(PlacedFarm.toKey(location));
    }

    public void unregisterCrop(PhysicalCrop crop) {
        physicalCrops.remove(crop.getLocationKey());
        saveQueue.submit("Falha ao remover planta " + crop.getLocationKey() + ".",
                () -> repository.deleteCrop(crop.getLocationKey()));
    }

    public PhysicalCrop findCropFor(Block target) {
        PhysicalCrop exact = getCropAt(target.getLocation());
        if (matchesHarvestMaterial(exact, target.getType())) return exact;
        for (int y = 0; y >= -3; y--) {
            PhysicalCrop below = getCropAt(target.getLocation().clone().add(0, y, 0));
            if (matchesHarvestMaterial(below, target.getType())) return below;
        }
        for (int x = -1; x <= 1; x++) for (int z = -1; z <= 1; z++) {
            if (Math.abs(x) + Math.abs(z) != 1) continue;
            PhysicalCrop nearby = getCropAt(target.getLocation().clone().add(x, 0, z));
            if (matchesHarvestMaterial(nearby, target.getType())) return nearby;
        }
        return null;
    }

    private boolean matchesHarvestMaterial(PhysicalCrop crop, Material material) {
        FarmType type = crop == null ? null : farmTypes.get(crop.getTypeId());
        return type != null && (type.getHarvestMaterials().contains(material)
                || type.getPlantMaterials().contains(material));
    }

    public boolean isMature(Block block) {
        Material material = block.getType();
        BlockState state = block.getState();
        if (isAgeCrop(material)) return isMature(state);
        if (material == Material.CACTUS || material == Material.SUGAR_CANE) {
            Block base = block;
            while (base.getRelative(0, -1, 0).getType() == material) base = base.getRelative(0, -1, 0);
            int height = 1;
            while (base.getRelative(0, height, 0).getType() == material) height++;
            return height >= 2;
        }
        return true;
    }

    public boolean isMature(BlockState state) {
        if (!(state.getBlockData() instanceof Ageable)) return false;
        Ageable ageable = (Ageable) state.getBlockData();
        return ageable.getAge() >= ageable.getMaximumAge();
    }

    private boolean isAgeCrop(Material material) {
        return material == Material.WHEAT || material == Material.CARROTS
                || material == Material.POTATOES || material == Material.NETHER_WART
                || material == Material.MELON_STEM || material == Material.PUMPKIN_STEM;
    }

    public boolean harvestPhysical(Block target, boolean force) {
        return harvestPhysical(target, force, 1);
    }

    public boolean harvestPhysical(Block target, boolean force, int yieldMultiplier) {
        if (yieldMultiplier < 1 || yieldMultiplier > 2) {
            throw new IllegalArgumentException("Multiplicador de colheita inválido.");
        }
        PhysicalCrop crop = findCropFor(target);
        if (crop == null || (!force && !isMature(target))) return false;
        FarmType type = farmTypes.get(crop.getTypeId());
        if (type == null || !type.getHarvestMaterials().contains(target.getType())) return false;
        FarmStorage silo = getStorage(crop.getOwnerUuid(), type.getId());
        org.bukkit.Bukkit.getPluginManager().callEvent(
                new br.com.tw.core.events.TWFarmHarvestEvent(crop.getOwnerUuid(), type.getId(),
                        yieldMultiplier, crop.getLocationKey()));
        long golden = Math.random() < plugin.getConfig().getDouble("farm.physical-golden-chance", 0.01)
                ? yieldMultiplier : 0L;
        silo.harvest(yieldMultiplier - golden, golden);
        saveStorageAsync(silo);

        if (isAgeCrop(target.getType())) {
            BlockState state = target.getState();
            Ageable data = (Ageable) state.getBlockData();
            data.setAge(0);
            state.setBlockData(data);
            state.update(true, false);
        } else if (target.getType() == Material.MELON) {
            target.setType(Material.AIR);
        } else if (target.getType() == Material.CACTUS || target.getType() == Material.SUGAR_CANE) {
            Block harvestBlock = target;
            while (harvestBlock.getRelative(0, 1, 0).getType() == target.getType()) {
                harvestBlock = harvestBlock.getRelative(0, 1, 0);
            }
            harvestBlock.setType(Material.AIR);
        }
        return true;
    }

    public PhysicalCrop getCropFromPlant(Block block) {
        PhysicalCrop crop = getCropAt(block.getLocation());
        if (crop != null) return crop;
        for (int y = -1; y >= -3; y--) {
            crop = getCropAt(block.getLocation().clone().add(0, y, 0));
            if (crop != null) return crop;
        }
        return null;
    }

    public FarmStorage getStorage(UUID owner, String typeId) {
        String key = FarmRepository.storageKey(owner, typeId);
        return storage.computeIfAbsent(key, ignored -> new FarmStorage(owner, typeId, 0, 0, 0, 0));
    }

    public List<FarmStorage> getPlayerStorage(UUID owner) {
        List<FarmStorage> result = new ArrayList<>();
        for (FarmStorage item : storage.values()) {
            if (item.getOwnerUuid().equals(owner)) result.add(item);
        }
        return result;
    }

    public void sellFarmStorage(Player player, PlacedFarm farm) {
        FarmType type = farmTypes.get(farm.getTypeId());
        if (type == null || farm.getStoredTotal() == 0) return;
        creditSale(player, type, farm.getStored(), farm.getStoredGolden());
        long[] collected = farm.collect();
        farm.addTotalSold(collected[0] + collected[1]);
        persistFarmAsync(farm);
        plugin.getHologramManager().updateOrCreate(farm, type);
    }

    public void sellPhysicalStorage(Player player, FarmStorage silo) {
        FarmType type = farmTypes.get(silo.getTypeId());
        if (type == null || silo.getStoredTotal() == 0) return;
        creditSale(player, type, silo.getStored(), silo.getStoredGolden());
        long[] collected = silo.collect();
        silo.addTotalSold(collected[0] + collected[1]);
        persistStorageAsync(silo);
    }

    public boolean withdrawFarmStorage(Player player, PlacedFarm farm) {
        FarmType type = farmTypes.get(farm.getTypeId());
        if (type == null) return false;
        boolean withdrawn = giveStoredItems(player, type, farm.getStored(), farm.getStoredGolden(), farm::collect,
                amounts -> farm.restoreStored(amounts[0], amounts[1]));
        persistFarmAsync(farm);
        return withdrawn;
    }

    public boolean withdrawPhysicalStorage(Player player, FarmStorage silo) {
        FarmType type = farmTypes.get(silo.getTypeId());
        if (type == null) return false;
        boolean withdrawn = giveStoredItems(player, type, silo.getStored(), silo.getStoredGolden(), silo::collect,
                amounts -> silo.restoreStored(amounts[0], amounts[1]));
        persistStorageAsync(silo);
        return withdrawn;
    }

    private boolean giveStoredItems(Player player, FarmType type, long regular, long golden,
                                    java.util.function.Supplier<long[]> collect,
                                    java.util.function.Consumer<long[]> restore) {
        long total = Math.addExact(regular, golden);
        if (total <= 0) {
            player.sendMessage(ChatColor.YELLOW + "Este silo não tem colheitas para recolher.");
            return false;
        }
        long[] collected = collect.get();
        long maxTransfer = 64L * 36L;
        long regularToGive = Math.min(collected[0], maxTransfer);
        long goldenToGive = Math.min(collected[1], maxTransfer - regularToGive);
        List<ItemStack> items = new ArrayList<>();
        appendItemStacks(items, type, regularToGive, false);
        appendItemStacks(items, type, goldenToGive, true);
        Map<Integer, ItemStack> leftovers = player.getInventory().addItem(items.toArray(new ItemStack[0]));
        long restoredRegular = collected[0] - regularToGive;
        long restoredGolden = collected[1] - goldenToGive;
        if (!leftovers.isEmpty()) {
            for (ItemStack leftover : leftovers.values()) {
                long amount = leftover.getAmount();
                if (leftover.hasItemMeta() && leftover.getItemMeta().hasDisplayName()
                        && leftover.getItemMeta().getDisplayName().startsWith("§6Safra Dourada")) {
                    restoredGolden += amount;
                } else {
                    restoredRegular += amount;
                }
            }
        }
        restore.accept(new long[]{restoredRegular, restoredGolden});
        if (restoredRegular + restoredGolden > 0) {
            player.sendMessage(ChatColor.YELLOW + "Parte da safra foi recolhida; o restante continua no silo.");
        }
        return regularToGive + goldenToGive > 0;
    }

    private void appendItemStacks(List<ItemStack> items, FarmType type, long amount, boolean golden) {
        while (amount > 0) {
            int stack = (int) Math.min(64L, amount);
            ItemStack item = new ItemStack(type.getProduct(), stack);
            ItemMeta meta = item.getItemMeta();
            String name = ChatColor.stripColor(ChatColor.translateAlternateColorCodes('&', type.getDisplayName()));
            meta.setDisplayName(golden ? "§6Safra Dourada: " + name : "§eColheita: " + name);
            meta.setLore(Collections.singletonList(golden ? "§7Valiosa: vale 5x na venda da farm."
                    : "§7Colheita recolhida do silo virtual."));
            item.setItemMeta(meta);
            Map<String, Object> tags = new HashMap<>();
            tags.put("tw_farm_withdrawn", true);
            tags.put("tw_farm_crop_type", type.getId());
            tags.put("tw_farm_golden", golden);
            item = TWItemTags.set(item, tags);
            items.add(item);
            amount -= stack;
        }
    }

    private void creditSale(Player player, FarmType type, long regular, long golden) {
        TWUser user = TWCorePlugin.getInstance().getUserManager().getUser(player.getUniqueId());
        if (user == null) throw new IllegalStateException("Dados do jogador não estão carregados: " + player.getName());
        double prestige = TWCorePlugin.getInstance().getPrestigeMultiplier(user.getPrestigeId());
        double global = TWEconomiaPlugin.getInstance().getGlobalBooster();
        double total = ((regular * type.getSellValue()) + (golden * type.getSellValue() * 5.0)) * prestige * global;
        if (!Double.isFinite(total) || total < 0.0) throw new IllegalStateException("Valor de venda inválido.");
        if (total > 0.0) {
            user.addBalance(total);
            TWCorePlugin.getInstance().getUserManager().saveUserAsync(user);
            player.sendMessage("§aSafra vendida: §e" + (regular + golden) + " §aitens por §2$"
                    + br.com.tw.core.utils.TWFormat.formatarDinheiro(total) + "§a.");
        }
    }

    public void saveFarmAsync(PlacedFarm farm) {
        PlacedFarm snapshot = farm.snapshot();
        pendingFarmSaves.put(snapshot.getLocationKey(), snapshot);
    }

    private void persistFarmAsync(PlacedFarm farm) {
        PlacedFarm snapshot = farm.snapshot();
        pendingFarmSaves.remove(snapshot.getLocationKey());
        saveQueue.submit("Falha ao salvar farm " + snapshot.getLocationKey() + ".",
                () -> repository.saveFarm(snapshot));
    }

    public void saveStorageAsync(FarmStorage silo) {
        FarmStorage snapshot = silo.snapshot();
        pendingStorageSaves.put(FarmRepository.storageKey(snapshot.getOwnerUuid(), snapshot.getTypeId()), snapshot);
    }

    private void persistStorageAsync(FarmStorage silo) {
        FarmStorage snapshot = silo.snapshot();
        String key = FarmRepository.storageKey(snapshot.getOwnerUuid(), snapshot.getTypeId());
        pendingStorageSaves.remove(key);
        saveQueue.submit("Falha ao salvar silo de " + snapshot.getOwnerUuid() + ".", () -> repository.saveStorage(snapshot));
    }

    public void saveAllSync() {
        List<PlacedFarm> farms = new ArrayList<>();
        for (PlacedFarm farm : placedFarms.values()) farms.add(farm.snapshot());
        List<FarmStorage> silos = new ArrayList<>();
        for (FarmStorage silo : storage.values()) silos.add(silo.snapshot());
        pendingFarmSaves.clear();
        pendingStorageSaves.clear();
        saveQueue.submit("Falha ao salvar o estado final das farms.", () -> {
            for (PlacedFarm farm : farms) repository.saveFarm(farm);
            for (FarmStorage silo : silos) repository.saveStorage(silo);
        });
        saveQueue.shutdownAndAwait();
    }

    private void saveAllAsync() {
        List<PlacedFarm> farms = new ArrayList<>(pendingFarmSaves.values());
        List<FarmStorage> silos = new ArrayList<>(pendingStorageSaves.values());
        pendingFarmSaves.clear();
        pendingStorageSaves.clear();
        if (farms.isEmpty() && silos.isEmpty()) return;
        saveQueue.submit("Falha ao salvar o ciclo de autosave das farms.", () -> {
            for (PlacedFarm farm : farms) repository.saveFarm(farm);
            for (FarmStorage silo : silos) repository.saveStorage(silo);
        });
    }

    public Map<String, FarmType> getFarmTypes() { return farmTypes; }
    public Map<String, PlacedFarm> getPlacedFarms() { return placedFarms; }
    public Map<String, PhysicalCrop> getPhysicalCrops() { return physicalCrops; }

    public FarmType getTypeForPlantMaterial(Material material) {
        for (FarmType type : farmTypes.values()) {
            if (type.getPlantMaterials().contains(material)) return type;
        }
        return null;
    }

    public FarmType getTypeForHarvestMaterial(Material material) {
        for (FarmType type : farmTypes.values()) {
            if (type.getHarvestMaterials().contains(material)) return type;
        }
        return null;
    }

    public boolean isFarmCropMaterial(Material material) {
        return getTypeForPlantMaterial(material) != null || getTypeForHarvestMaterial(material) != null;
    }
}
