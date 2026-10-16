package br.com.tw.spawners.managers;

import br.com.tw.spawners.TWSpawnersPlugin;
import br.com.tw.spawners.models.PlacedSpawner;
import br.com.tw.spawners.models.SpawnerType;
import org.bukkit.Location;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class SpawnerHologramManager {

    private final TWSpawnersPlugin plugin;
    private final Map<String, List<ArmorStand>> holograms = new ConcurrentHashMap<>();

    public SpawnerHologramManager(TWSpawnersPlugin plugin) {
        this.plugin = plugin;
    }

    public void updateOrCreateHologram(PlacedSpawner spawner, SpawnerType type) {
        Location loc = spawner.getBukkitLocation();
        if (loc == null || loc.getWorld() == null) return;
        if (!loc.getWorld().isChunkLoaded(loc.getBlockX() >> 4, loc.getBlockZ() >> 4)) return;

        List<String> lines = new ArrayList<>();
        lines.add(type.getDisplayName());
        lines.add("§7Quantia: §e" + spawner.getStackAmount() + "x");
        lines.add("§7Dono: §f" + spawner.getOwnerName());

        List<ArmorStand> stands = holograms.get(spawner.getLocationKey());

        if (stands == null || stands.size() != lines.size() || stands.stream().anyMatch(Entity::isDead)) {
            removeHologram(spawner.getLocationKey());
            stands = new ArrayList<>();

            // Ajuste de altura para a 1.8.8: o texto do ArmorStand com marker fica acima do bloco de ferro
            double yOffset = 1.1 + (lines.size() * 0.28);
            for (String text : lines) {
                Location lineLoc = loc.clone().add(0.5, yOffset, 0.5);
                ArmorStand stand = (ArmorStand) loc.getWorld().spawnEntity(lineLoc, EntityType.ARMOR_STAND);
                setupArmorStand(stand, text);
                stands.add(stand);
                yOffset -= 0.28;
            }
            holograms.put(spawner.getLocationKey(), stands);
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