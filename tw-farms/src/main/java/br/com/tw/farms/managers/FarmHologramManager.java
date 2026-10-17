package br.com.tw.farms.managers;

import br.com.tw.farms.TWFarmsPlugin;
import br.com.tw.farms.models.FarmType;
import br.com.tw.farms.models.PlacedFarm;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;

public final class FarmHologramManager {
    private static final String PREFIX = "§0[TW-FARM]";
    private final TWFarmsPlugin plugin;
    private final Map<String, ArmorStand> holograms = new ConcurrentHashMap<>();
    private final Set<java.util.UUID> activeHologramIds = ConcurrentHashMap.newKeySet();

    public FarmHologramManager(TWFarmsPlugin plugin) {
        this.plugin = plugin;
    }

    public void updateOrCreate(PlacedFarm farm, FarmType type) {
        Location location = farm.getBukkitLocation();
        if (location == null || !location.getWorld().isChunkLoaded(location.getBlockX() >> 4,
                location.getBlockZ() >> 4)) return;

        ArmorStand stand = holograms.get(farm.getLocationKey());
        if (stand == null || stand.isDead()) {
            if (stand != null) activeHologramIds.remove(stand.getUniqueId());
            stand = (ArmorStand) location.getWorld().spawnEntity(location.clone().add(0.5, 1.4, 0.5),
                    EntityType.ARMOR_STAND);
            stand.setVisible(false);
            stand.setGravity(false);
            stand.setSmall(true);
            stand.setBasePlate(false);
            stand.setCustomNameVisible(true);
            stand.setRemoveWhenFarAway(false);
            holograms.put(farm.getLocationKey(), stand);
            activeHologramIds.add(stand.getUniqueId());
        }
        String name = org.bukkit.ChatColor.translateAlternateColorCodes('&', type.getDisplayName());
        stand.setCustomName(PREFIX + " " + name + " §7x" + farm.getStackAmount()
                + " §a" + farm.getStoredTotal() + " §7| §eAdubo " + farm.getFertilizer());
    }

    public void startOrphanCleanup(Predicate<String> isRegisteredFarm) {
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            for (org.bukkit.World world : Bukkit.getWorlds()) {
                for (Entity entity : world.getEntities()) {
                    if (!(entity instanceof ArmorStand)) continue;
                    ArmorStand stand = (ArmorStand) entity;
                    String name = stand.getCustomName();
                    if (name != null && name.startsWith(PREFIX)
                            && !activeHologramIds.contains(stand.getUniqueId())) {
                        stand.remove();
                    }
                }
            }
            for (String key : holograms.keySet()) {
                if (!isRegisteredFarm.test(key)) remove(key);
            }
        }, 40L);
    }

    public void clearAll() {
        for (ArmorStand stand : holograms.values()) {
            if (stand != null && !stand.isDead()) stand.remove();
        }
        holograms.clear();
        activeHologramIds.clear();
    }

    public void remove(String key) {
        ArmorStand stand = holograms.remove(key);
        if (stand != null) {
            activeHologramIds.remove(stand.getUniqueId());
            if (!stand.isDead()) stand.remove();
        }
    }
}
