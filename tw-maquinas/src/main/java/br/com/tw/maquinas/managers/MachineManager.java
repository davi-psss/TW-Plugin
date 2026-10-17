package br.com.tw.maquinas.managers;

import br.com.tw.core.TWCorePlugin;
import br.com.tw.core.utils.TWFormat;
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

    private final Map<String, MachineType> machineTypes = new LinkedHashMap<>();
    private final Map<String, PlacedMachine> placedMachines = new ConcurrentHashMap<>();

    private final List<String> rankHierarchy = Arrays.asList(
            "CARVAO", "NEVE", "SINO", "BISCOITO", "DUENDE", "RENA", "TRENO", "QUEBRANOZES", "ESTRELA", "NOEL", "MAX"
    );

    public MachineManager(TWMaquinasPlugin plugin, MachineRepository repository) {
        this.plugin = plugin;
        this.repository = repository;

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
        if (sec == null) return;

        for (String key : sec.getKeys(false)) {
            String name = sec.getString(key + ".nome", "&aMáquina");
            Material mat = Material.matchMaterial(sec.getString(key + ".material", "COAL_BLOCK"));
            if (mat == null) mat = Material.COAL_BLOCK;

            String reqRank = sec.getString(key + ".rank-necessario", "CARVAO");
            double cost = sec.getDouble(key + ".custo-compra", 10000.0);
            double val = sec.getDouble(key + ".valor-por-ciclo", 500.0);
            int cycle = sec.getInt(key + ".ciclo-segundos", 10);

            machineTypes.put(key.toUpperCase(), new MachineType(key, name, mat, reqRank, cost, val, cycle));
        }
    }

    private void startEngineTask() {
        new BukkitRunnable() {
            @Override
            public void run() {
                long nowSec = System.currentTimeMillis() / 1000L;

                for (PlacedMachine machine : placedMachines.values()) {
                    MachineType type = machineTypes.get(machine.getTypeId());
                    if (type == null) continue;

                    Location loc = machine.getBukkitLocation();
                    if (loc == null || !loc.getWorld().isChunkLoaded(loc.getBlockX() >> 4, loc.getBlockZ() >> 4)) {
                        continue;
                    }

                    if (machine.getFuel() <= 0) {
                        plugin.getHologramManager().updateOrCreateHologram(machine, type);
                        continue;
                    }

                    double realCycleSeconds = Math.max(1.0, type.getCycleSeconds() - ((machine.getSpeedLevel() - 1) * 0.5));

                    if (nowSec - machine.getLastTickEpochSeconds() >= realCycleSeconds) {
                        machine.setLastTickEpochSeconds(nowSec);

                        double fuelSaveChance = (machine.getFuelEfficiencyLevel() - 1) * 0.05;
                        boolean consumeFuel = Math.random() >= fuelSaveChance;

                        if (!consumeFuel || machine.consumeFuel(1)) {
                            double maxStorage = (type.getValuePerCycle() * machine.getStackAmount()) * (10 * machine.getStorageLevel());
                            if (machine.getStoredEarnings() < maxStorage) {
                                double generated = type.getValuePerCycle() * machine.getStackAmount();
                                machine.addStoredEarnings(generated);
                                machine.addTotalGenerated(generated);
                                playMachineEffects(loc, type.getId());
                            }
                        }
                    }

                    // Fora do if do ciclo, para animar a barra a cada segundo
                    plugin.getHologramManager().updateOrCreateHologram(machine, type);
                }
            }
        }.runTaskTimer(plugin, 20L, 20L);

        new BukkitRunnable() {
            @Override
            public void run() {
                saveAllSync();
            }
        }.runTaskTimerAsynchronously(plugin, 72000L, 72000L);
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
        return item;
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
        return item;
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
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> repository.deleteMachine(key));
    }

    public void saveMachineAsync(PlacedMachine machine) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> repository.saveMachine(machine));
    }

    public void saveAllSync() {
        for (PlacedMachine m : placedMachines.values()) {
            repository.saveMachine(m);
        }
    }

    public Map<String, MachineType> getMachineTypes() {
        return machineTypes;
    }

    public Map<String, PlacedMachine> getPlacedMachines() {
        return placedMachines;
    }
}