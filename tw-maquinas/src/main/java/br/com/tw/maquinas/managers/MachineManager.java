package br.com.tw.maquinas.managers;

import br.com.tw.core.utils.TWFormat;
import br.com.tw.core.utils.TWItemTags;
import br.com.tw.core.utils.TWAsyncTaskQueue;
import br.com.tw.core.utils.TWConfigValidation;
import br.com.tw.maquinas.TWMaquinasPlugin;
import br.com.tw.maquinas.data.MachineRepository;
import br.com.tw.maquinas.models.MachineType;
import br.com.tw.maquinas.models.PlacedMachine;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Effect;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class MachineManager {

    private final TWMaquinasPlugin plugin;
    private final MachineRepository repository;
    private final TWAsyncTaskQueue saveQueue;

    private final Map<String, MachineType> machineTypes = new LinkedHashMap<>();
    private final Map<String, PlacedMachine> placedMachines = new ConcurrentHashMap<>();
    private final Set<String> loggedProcessingFailures = ConcurrentHashMap.newKeySet();

    private final List<String> rankHierarchy;

    public MachineManager(TWMaquinasPlugin plugin, MachineRepository repository) {
        this.plugin = plugin;
        this.repository = repository;
        this.saveQueue = new TWAsyncTaskQueue(plugin, "tw-machines-db");
        this.rankHierarchy = br.com.tw.core.TWCorePlugin.getInstance().getRankHierarchy();

        loadConfigTypes();
        this.placedMachines.putAll(repository.loadAllMachines());

        startEngineTask();

        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            for (PlacedMachine m : placedMachines.values()) {
                MachineType type = machineTypes.get(m.getTypeId());
                if (type != null) {
                    plugin.getHologramManager().updateOrCreateHologram(m, type);
                }
            }
        }, 40L);
    }

    public void loadConfigTypes() {
        machineTypes.clear();
        ConfigurationSection sec = plugin.getConfig().getConfigurationSection("maquinas");
        if (sec == null || sec.getKeys(false).isEmpty()) {
            throw new IllegalArgumentException("Configuração inválida em maquinas: é necessário definir ao menos um tipo.");
        }

        for (String key : sec.getKeys(false)) {
            String name = TWConfigValidation.requireText(plugin, "maquinas." + key + ".nome",
                    sec.getString(key + ".nome"));
            Material mat = Material.matchMaterial(sec.getString(key + ".material", "COAL_BLOCK"));
            if (mat == null || mat == Material.AIR) {
                throw new IllegalArgumentException("Material inválido em maquinas." + key + ".material.");
            }

            String reqRank = TWConfigValidation.requireText(plugin, "maquinas." + key + ".rank-necessario",
                    sec.getString(key + ".rank-necessario"));
            if (!rankHierarchy.contains(reqRank.toUpperCase(Locale.ROOT))) {
                throw new IllegalArgumentException("Rank desconhecido em maquinas." + key + ".rank-necessario: " + reqRank);
            }
            double cost = TWConfigValidation.requireFiniteAtLeast(plugin, "maquinas." + key + ".custo-compra",
                    sec.getDouble(key + ".custo-compra", Double.NaN), 0.0);
            double val = TWConfigValidation.requireFiniteAtLeast(plugin, "maquinas." + key + ".valor-por-ciclo",
                    sec.getDouble(key + ".valor-por-ciclo", Double.NaN), 0.0);
            int cycle = TWConfigValidation.requireIntRange(plugin, "maquinas." + key + ".ciclo-segundos",
                    sec.getInt(key + ".ciclo-segundos", -1), 1, 86400);

            machineTypes.put(key.toUpperCase(), new MachineType(key, name, mat, reqRank, cost, val, cycle));
        }
    }

    private void startEngineTask() {
        new BukkitRunnable() {
            @Override
            public void run() {
                long nowSec = System.currentTimeMillis() / 1000L;

                for (PlacedMachine machine : placedMachines.values()) {
                    try {
                        processMachine(machine, nowSec);
                    } catch (RuntimeException e) {
                        if (loggedProcessingFailures.add(machine.getLocationKey())) {
                            plugin.getLogger().log(java.util.logging.Level.SEVERE,
                                    "Falha ao processar máquina em " + machine.getLocationKey() + ".", e);
                        }
                    }
                }
            }
        }.runTaskTimer(plugin, 20L, 20L);

        new BukkitRunnable() {
            @Override
            public void run() {
                saveAllAsync();
            }
        }.runTaskTimer(plugin, 72000L, 72000L);
    }

    private void processMachine(PlacedMachine machine, long nowSec) {
        MachineType type = machineTypes.get(machine.getTypeId());
        if (type == null) return;

        Location loc = machine.getBukkitLocation();
        if (loc == null || !loc.getWorld().isChunkLoaded(loc.getBlockX() >> 4, loc.getBlockZ() >> 4)) return;

        if (machine.getFuel() > 0) {
            double realCycleSeconds = Math.max(1.0,
                    type.getCycleSeconds() - ((machine.getSpeedLevel() - 1) * 0.5));
            if (nowSec - machine.getLastTickEpochSeconds() >= realCycleSeconds) {
                machine.setLastTickEpochSeconds(nowSec);
                double fuelSaveChance = Math.min(1.0, Math.max(0.0,
                        (machine.getFuelEfficiencyLevel() - 1) * 0.05));
                boolean consumeFuel = Math.random() >= fuelSaveChance;

                if (!consumeFuel || machine.consumeFuel(1)) {
                    double generated = type.getValuePerCycle() * machine.getStackAmount();
                    double maxStorage = generated * (10 * machine.getStorageLevel());
                    if (!Double.isFinite(generated) || !Double.isFinite(maxStorage)) {
                        throw new IllegalStateException("Produção excedeu o limite numérico.");
                    }
                    if (machine.getStoredEarnings() < maxStorage) {
                        machine.addStoredEarnings(generated);
                        machine.addTotalGenerated(generated);
                        playMachineEffects(loc, type.getId());
                    }
                }
            }
        }
        plugin.getHologramManager().updateOrCreateHologram(machine, type);
    }

    private void playMachineEffects(Location loc, String typeId) {
        Location center = loc.clone().add(0.5, 1.0, 0.5);
        if (typeId.equals("NEVE") || typeId.equals("RENA") || typeId.equals("NOEL")) {
            loc.getWorld().playEffect(center, Effect.SNOW_SHOVEL, 0);
        } else if (typeId.equals("SINO") || typeId.equals("ESTRELA") || typeId.equals("DUENDE")) {
            loc.getWorld().playEffect(center, Effect.MAGIC_CRIT, 0);
        } else {
            loc.getWorld().playEffect(center, Effect.HAPPY_VILLAGER, 0);
        }
        loc.getWorld().playSound(center, Sound.CHICKEN_EGG_POP, 0.1f, 0.5f);
    }

    public boolean hasRequiredRank(String playerRank, String playerPrestige, String requiredRank) {
        if (playerPrestige != null && !playerPrestige.equalsIgnoreCase("NENHUM")) {
            return true;
        }
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

    public ItemStack createMachineItem(MachineType type, long amount) {
        ItemStack item = new ItemStack(type.getMaterial(), 1);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(type.getDisplayName());
        meta.setLore(Arrays.asList(
                ChatColor.GRAY + "Tipo: " + ChatColor.WHITE + type.getId(),
                ChatColor.GRAY + "Quantia (Stack): " + ChatColor.YELLOW + amount + "x",
                ChatColor.GRAY + "Produção Base: " + ChatColor.GREEN + "$" + TWFormat.formatarDinheiro(type.getValuePerCycle()) + " / " + type.getCycleSeconds() + "s",
                "",
                ChatColor.YELLOW + "Coloca no teu terreno para produzir!",
                ChatColor.BLACK + "TWMACHINE:" + type.getId() + ":" + amount
        ));
        item.setItemMeta(meta);
        Map<String, Object> tags = new HashMap<>();
        tags.put("tw_machine_type", type.getId());
        tags.put("tw_machine_stack", amount);
        return TWItemTags.set(item, tags);
    }

    public ItemStack createFuelItem(long fuelUnits) {
        ItemStack item = new ItemStack(Material.COAL, 1, (short) 1);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(ChatColor.GOLD + "" + ChatColor.BOLD + "Combustível Natalino");
        meta.setLore(Arrays.asList(
                ChatColor.GRAY + "Unidades: " + ChatColor.AQUA + fuelUnits,
                ChatColor.GRAY + "Abastece as tuas máquinas para",
                ChatColor.GRAY + "gerarem recursos automaticamente.",
                "",
                ChatColor.YELLOW + "Clica com o botão direito numa máquina!",
                ChatColor.BLACK + "TWFUEL:" + fuelUnits
        ));
        item.setItemMeta(meta);
        Map<String, Object> tags = new HashMap<>();
        tags.put("tw_fuel_units", fuelUnits);
        return TWItemTags.set(item, tags);
    }

    public PlacedMachine getMachineAt(Location loc) {
        return placedMachines.get(PlacedMachine.toKey(loc));
    }

    public PlacedMachine findNearbySameMachine(Location center, UUID owner, String typeId, int radius) {
        for (int x = -radius; x <= radius; x++) {
            for (int y = -radius; y <= radius; y++) {
                for (int z = -radius; z <= radius; z++) {
                    Location check = center.clone().add(x, y, z);
                    PlacedMachine existing = getMachineAt(check);
                    if (existing != null && existing.getOwnerUuid().equals(owner) && existing.getTypeId().equalsIgnoreCase(typeId)) {
                        return existing;
                    }
                }
            }
        }
        return null;
    }

    public void registerMachine(PlacedMachine machine) {
        placedMachines.put(machine.getLocationKey(), machine);
        saveMachineAsync(machine);
    }

    public void removeMachine(Location loc) {
        String key = PlacedMachine.toKey(loc);
        placedMachines.remove(key);
        saveQueue.submit("Falha ao remover máquina " + key + " do banco.", () -> repository.deleteMachine(key));
    }

    public void saveMachineAsync(PlacedMachine machine) {
        PlacedMachine snapshot = machine.snapshot();
        saveQueue.submit("Falha ao salvar máquina " + snapshot.getLocationKey() + " no banco.",
                () -> repository.saveMachine(snapshot));
    }

    public void saveAllSync() {
        for (PlacedMachine m : placedMachines.values()) {
            PlacedMachine snapshot = m.snapshot();
            saveQueue.submit("Falha ao salvar máquina " + snapshot.getLocationKey() + " no banco.",
                    () -> repository.saveMachine(snapshot));
        }
        saveQueue.shutdownAndAwait();
    }

    private void saveAllAsync() {
        for (PlacedMachine machine : placedMachines.values()) {
            PlacedMachine snapshot = machine.snapshot();
            saveQueue.submit("Falha ao salvar máquina " + snapshot.getLocationKey() + " no banco.",
                    () -> repository.saveMachine(snapshot));
        }
    }

    public Map<String, MachineType> getMachineTypes() {
        return machineTypes;
    }

    public Map<String, PlacedMachine> getPlacedMachines() {
        return placedMachines;
    }
}