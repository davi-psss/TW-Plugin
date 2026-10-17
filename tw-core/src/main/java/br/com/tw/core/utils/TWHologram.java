package br.com.tw.core.utils;

import br.com.tw.core.TWCorePlugin;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.metadata.MetadataValue;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import java.util.logging.Level;

public class TWHologram {

    public static final String METADATA_KEY = "tw_hologram";

    private final JavaPlugin plugin;
    private BukkitTask orphanCleanupTask;

    public TWHologram(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public ArmorStand spawn(Location location, String text, String locationKey) {
        ArmorStand stand = (ArmorStand) location.getWorld().spawnEntity(location, EntityType.ARMOR_STAND);
        stand.setVisible(false);
        stand.setGravity(false);
        stand.setCanPickupItems(false);
        stand.setCustomName(text);
        stand.setCustomNameVisible(true);
        stand.setMarker(true);
        stand.setMetadata(METADATA_KEY, new FixedMetadataValue(plugin, locationKey));
        return stand;
    }

    public void purgeAt(String locationKey) {
        Location base = getBlockLocation(locationKey);

        if (base != null && base.getWorld().isChunkLoaded(
                base.getBlockX() >> 4, base.getBlockZ() >> 4)) {
            List<Entity> removals = new ArrayList<>();
            for (Entity entity : base.getChunk().getEntities()) {
                if (!(entity instanceof ArmorStand)) continue;

                ArmorStand stand = (ArmorStand) entity;
                if (locationKey.equals(getLocationKey(stand))
                        || (!stand.hasMetadata(METADATA_KEY) && isLegacyHologramAt(stand, base))) {
                    removals.add(stand);
                }
            }
            removeEntities(removals);
            return;
        }

        List<Entity> removals = new ArrayList<>();
        for (World world : Bukkit.getWorlds()) {
            for (Entity entity : world.getEntities()) {
                if (!(entity instanceof ArmorStand)) continue;
                if (locationKey.equals(getLocationKey(entity))) removals.add(entity);
            }
        }
        removeEntities(removals);
    }

    public void removeChunkHolograms(Chunk chunk) {
        List<Entity> removals = new ArrayList<>();
        for (Entity entity : chunk.getEntities()) {
            if (entity instanceof ArmorStand && getLocationKey(entity) != null) {
                removals.add(entity);
            }
        }
        removeEntities(removals);
    }

    public void removeAll() {
        List<Entity> removals = new ArrayList<>();
        for (World world : Bukkit.getWorlds()) {
            for (Entity entity : world.getEntities()) {
                if (entity instanceof ArmorStand && getLocationKey(entity) != null) {
                    removals.add(entity);
                }
            }
        }
        removeEntities(removals);
    }

    public void removeOrphans(Predicate<String> locationExists) {
        List<Entity> removals = new ArrayList<>();
        for (World world : Bukkit.getWorlds()) {
            for (Entity entity : world.getEntities()) {
                if (!(entity instanceof ArmorStand)) continue;

                String locationKey = getLocationKey(entity);
                if (locationKey != null && !locationExists.test(locationKey)) {
                    removals.add(entity);
                }
            }
        }
        removeEntities(removals);
    }

    public boolean belongsToChunk(String locationKey, String worldName, int chunkX, int chunkZ) {
        Location location = getBlockLocation(locationKey);
        return location != null
                && location.getWorld().getName().equals(worldName)
                && (location.getBlockX() >> 4) == chunkX
                && (location.getBlockZ() >> 4) == chunkZ;
    }

    public void startOrphanCleanup(Predicate<String> locationExists) {
        stopOrphanCleanup();
        orphanCleanupTask = Bukkit.getScheduler().runTaskTimer(plugin,
                () -> removeOrphans(locationExists), 100L, 100L);
    }

    public void stopOrphanCleanup() {
        if (orphanCleanupTask != null) {
            orphanCleanupTask.cancel();
            orphanCleanupTask = null;
        }
    }

    public void destroyForPlayer(Player player, World sourceWorld) {
        List<Integer> entityIds = new ArrayList<>();
        for (Entity entity : sourceWorld.getEntities()) {
            if (entity instanceof ArmorStand && getLocationKey(entity) != null) {
                entityIds.add(entity.getEntityId());
            }
        }
        if (entityIds.isEmpty()) return;

        int[] ids = new int[entityIds.size()];
        for (int i = 0; i < entityIds.size(); i++) {
            ids[i] = entityIds.get(i);
        }

        try {
            String nmsPackage = TWProtocol.getNmsPackage();
            Class<?> packetClass = Class.forName(nmsPackage + "PacketPlayOutEntityDestroy");
            Constructor<?> constructor = packetClass.getConstructor(int[].class);
            Object packet = constructor.newInstance((Object) ids);

            TWProtocol.sendPacket(TWCorePlugin.getInstance(), player, packet);
        } catch (ReflectiveOperationException | SecurityException e) {
            plugin.getLogger().log(Level.SEVERE,
                    "Não foi possível enviar o pacote de destruição dos hologramas para " + player.getName(), e);
        }
    }

    private String getLocationKey(Entity entity) {
        if (!entity.hasMetadata(METADATA_KEY)) return null;
        for (MetadataValue value : entity.getMetadata(METADATA_KEY)) {
            if (value.getOwningPlugin() == plugin) {
                return value.asString();
            }
        }
        return null;
    }

    private Location getBlockLocation(String locationKey) {
        if (locationKey == null) return null;
        String[] parts = locationKey.split(";", 4);
        if (parts.length != 4) return null;

        World world = Bukkit.getWorld(parts[0]);
        if (world == null) return null;
        try {
            return new Location(world, Integer.parseInt(parts[1]), Integer.parseInt(parts[2]),
                    Integer.parseInt(parts[3]));
        } catch (NumberFormatException e) {
            plugin.getLogger().warning("Chave de localização de holograma inválida: " + locationKey);
            return null;
        }
    }

    private boolean isLegacyHologramAt(ArmorStand stand, Location base) {
        Location standLocation = stand.getLocation();
        return stand.isMarker()
                && stand.isCustomNameVisible()
                && Math.abs(standLocation.getX() - (base.getBlockX() + 0.5)) <= 0.75
                && Math.abs(standLocation.getZ() - (base.getBlockZ() + 0.5)) <= 0.75
                && standLocation.getY() >= base.getBlockY() + 1.0
                && standLocation.getY() <= base.getBlockY() + 4.0;
    }

    private void removeEntities(List<Entity> entities) {
        for (Entity entity : entities) {
            entity.remove();
        }
    }
}
