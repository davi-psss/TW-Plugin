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
        lines.add("§7Stack: §e" + machine.getStackAmount() + "x §8| §7Nível: §a" + machine.getLevel());
        lines.add("§7Armazenado: §2$" + TWFormat.formatarDinheiro(machine.getStoredEarnings()));

        if (machine.getFuel() <= 0) {
            lines.add("§c§lSEM COMBUSTÍVEL!");
        }

        List<ArmorStand> stands = holograms.get(machine.getLocationKey());

        if (stands == null || stands.size() != lines.size() || stands.stream().anyMatch(Entity::isDead)) {
            removeHologram(machine.getLocationKey());
            stands = new ArrayList<>();

            // Na 1.8.8, com marker(true), o texto renderiza na base da entidade.
            // Somamos 1.0 (altura do bloco) + 0.3 por linha para ficar claramente visível no topo:
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
        stand.setMarker(true); // Impede colisões e cliques
        // Removemos stand.setSmall(true) para manter a escala padronizada de nametag no 1.8.8
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