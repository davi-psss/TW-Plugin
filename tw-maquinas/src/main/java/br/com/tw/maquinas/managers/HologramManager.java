package br.com.tw.maquinas.managers;

import br.com.tw.core.utils.TWFormat;
import br.com.tw.maquinas.TWMaquinasPlugin;
import br.com.tw.maquinas.models.MachineType;
import br.com.tw.maquinas.models.PlacedMachine;
import org.bukkit.Location;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class HologramManager {

    private final TWMaquinasPlugin plugin;
    private final Map<String, List<ArmorStand>> holograms = new ConcurrentHashMap<>();

    public HologramManager(TWMaquinasPlugin plugin) {
        this.plugin = plugin;
    }

    public void updateOrCreateHologram(PlacedMachine machine, MachineType type) {
        Location loc = machine.getBukkitLocation();
        if (loc == null || loc.getWorld() == null) return;
        if (!loc.getWorld().isChunkLoaded(loc.getBlockX() >> 4, loc.getBlockZ() >> 4)) return;

        List<String> lines = new ArrayList<>();
        lines.add(type.getDisplayName());

        lines.add("§7Stack: §e" + machine.getStackAmount() + "x §8| §7Vel: §aLv." + machine.getSpeedLevel());

        long nowSec = System.currentTimeMillis() / 1000L;
        double realCycleSeconds = Math.max(1.0, type.getCycleSeconds() - ((machine.getSpeedLevel() - 1) * 0.5));
        double percent = Math.min(1.0, (nowSec - machine.getLastTickEpochSeconds()) / realCycleSeconds);

        int bars = (int) (percent * 10);
        StringBuilder progress = new StringBuilder("§a");
        for (int i = 0; i < 10; i++) {
            if (i == bars) progress.append("§7");
            progress.append("■");
        }
        lines.add("§7Progresso: " + progress.toString());

        double maxStorage = (type.getValuePerCycle() * machine.getStackAmount()) * (10 * machine.getStorageLevel());
        lines.add("§7Armazenado: §2$" + TWFormat.formatarDinheiro(machine.getStoredEarnings()) + " §8/ §a$" + TWFormat.formatarDinheiro(maxStorage));

        if (machine.getTotalGenerated() > 0) {
            lines.add("§7Total Histórico: §6$" + TWFormat.formatarDinheiro(machine.getTotalGenerated()));
        }

        if (machine.getFuel() <= 0) {
            lines.add("§c§lSEM COMBUSTÍVEL!");
        } else if (machine.getStoredEarnings() >= maxStorage) {
            lines.add("§c§lARMAZÉM CHEIO! RECOLHE OS LUCROS!");
        } else {
            lines.add("§7Combustível: §e" + machine.getFuel() + " ciclos");
        }

        List<ArmorStand> stands = holograms.get(machine.getLocationKey());

        if (stands == null || stands.size() != lines.size() || stands.stream().anyMatch(Entity::isDead)) {
            removeHologram(machine.getLocationKey());
            stands = new ArrayList<>();

            double yOffset = 1.0 + (lines.size() * 0.28);
            for (String text : lines) {
                Location lineLoc = loc.clone().add(0.5, yOffset, 0.5);
                ArmorStand stand = (ArmorStand) loc.getWorld().spawnEntity(lineLoc, EntityType.ARMOR_STAND);
                setupArmorStand(stand, text);
                stands.add(stand);
                yOffset -= 0.28;
            }
            holograms.put(machine.getLocationKey(), stands);
        } else {
            for (int i = 0; i < lines.size(); i++) {
                stands.get(i).setCustomName(lines.get(i));
            }
        }
    }

    private void setupArmorStand(ArmorStand stand, String text) {
        stand.setVisible(false);
        stand.setGravity(false);
        stand.setCanPickupItems(false);
        stand.setCustomName(text);
        stand.setCustomNameVisible(true);
        stand.setMarker(true);
    }

    public void removeHologram(String locKey) {
        List<ArmorStand> stands = holograms.remove(locKey);
        if (stands != null) {
            for (ArmorStand stand : stands) {
                if (stand.isValid()) {
                    stand.remove();
                }
            }
        }
    }

    public void clearAll() {
        for (List<ArmorStand> stands : holograms.values()) {
            for (ArmorStand stand : stands) {
                if (stand.isValid()) {
                    stand.remove();
                }
            }
        }
        holograms.clear();
    }
}