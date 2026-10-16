package br.com.tw.minas.utils;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class TWBossBar {

    private static final Map<UUID, Object> activeWithers = new ConcurrentHashMap<>();

    private static Class<?> craftPlayerClass;
    private static Class<?> craftWorldClass;
    private static Class<?> worldServerClass;
    private static Class<?> entityWitherClass;
    private static Class<?> entityLivingClass;
    private static Class<?> entityClass;
    private static Class<?> packetSpawnClass;
    private static Class<?> packetDestroyClass;
    private static Class<?> packetMetadataClass;
    private static Class<?> packetTeleportClass;
    private static Class<?> dataWatcherClass;

    private static Method getHandleMethod;
    private static Method getWorldHandleMethod;
    private static Field playerConnectionField;
    private static Method sendPacketMethod;

    private static Method setLocationMethod;
    private static Method setCustomNameMethod;
    private static Method setInvisibleMethod;
    private static Method setHealthMethod;
    private static Method getDataWatcherMethod;
    private static Method getIdMethod;

    private static Constructor<?> spawnConstructor;
    private static Constructor<?> destroyConstructor;
    private static Constructor<?> metadataConstructor;
    private static Constructor<?> teleportConstructor;

    static {
        try {
            String version = Bukkit.getServer().getClass().getPackage().getName().split("\\.")[3];
            craftPlayerClass = Class.forName("org.bukkit.craftbukkit." + version + ".entity.CraftPlayer");
            craftWorldClass = Class.forName("org.bukkit.craftbukkit." + version + ".CraftWorld");
            worldServerClass = Class.forName("net.minecraft.server." + version + ".World");
            entityWitherClass = Class.forName("net.minecraft.server." + version + ".EntityWither");
            entityLivingClass = Class.forName("net.minecraft.server." + version + ".EntityLiving");
            entityClass = Class.forName("net.minecraft.server." + version + ".Entity");
            packetSpawnClass = Class.forName("net.minecraft.server." + version + ".PacketPlayOutSpawnEntityLiving");
            packetDestroyClass = Class.forName("net.minecraft.server." + version + ".PacketPlayOutEntityDestroy");
            packetMetadataClass = Class.forName("net.minecraft.server." + version + ".PacketPlayOutEntityMetadata");
            packetTeleportClass = Class.forName("net.minecraft.server." + version + ".PacketPlayOutEntityTeleport");
            dataWatcherClass = Class.forName("net.minecraft.server." + version + ".DataWatcher");

            getHandleMethod = craftPlayerClass.getMethod("getHandle");
            getWorldHandleMethod = craftWorldClass.getMethod("getHandle");

            Class<?> entityPlayerClass = Class.forName("net.minecraft.server." + version + ".EntityPlayer");
            playerConnectionField = entityPlayerClass.getField("playerConnection");
            Class<?> playerConnectionClass = Class.forName("net.minecraft.server." + version + ".PlayerConnection");
            sendPacketMethod = playerConnectionClass.getMethod("sendPacket", Class.forName("net.minecraft.server." + version + ".Packet"));

            setLocationMethod = entityClass.getMethod("setLocation", double.class, double.class, double.class, float.class, float.class);
            setCustomNameMethod = entityClass.getMethod("setCustomName", String.class);
            setInvisibleMethod = entityClass.getMethod("setInvisible", boolean.class);
            setHealthMethod = entityLivingClass.getMethod("setHealth", float.class);
            getDataWatcherMethod = entityClass.getMethod("getDataWatcher");
            getIdMethod = entityClass.getMethod("getId");

            spawnConstructor = packetSpawnClass.getConstructor(entityLivingClass);
            destroyConstructor = packetDestroyClass.getConstructor(int[].class);
            metadataConstructor = packetMetadataClass.getConstructor(int.class, dataWatcherClass, boolean.class);
            teleportConstructor = packetTeleportClass.getConstructor(entityClass);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void sendBossBar(Player player, String message, float healthPercent) {
        try {
            Object wither = activeWithers.get(player.getUniqueId());
            boolean isNew = false;

            if (wither == null) {
                Object worldServer = getWorldHandleMethod.invoke(craftWorldClass.cast(player.getWorld()));
                wither = entityWitherClass.getConstructor(worldServerClass).newInstance(worldServer);
                activeWithers.put(player.getUniqueId(), wither);
                isNew = true;
            }

            // Move o Wither invisível acompanhando a câmera do jogador para a barra não sumir
            Location loc = player.getLocation();
            Location target = loc.clone().add(loc.getDirection().multiply(30));
            setLocationMethod.invoke(wither, target.getX(), target.getY(), target.getZ(), target.getYaw(), target.getPitch());
            setCustomNameMethod.invoke(wither, message);
            setInvisibleMethod.invoke(wither, true);
            setHealthMethod.invoke(wither, Math.max(0.1f, healthPercent * 300f));

            Object handle = getHandleMethod.invoke(player);
            Object connection = playerConnectionField.get(handle);

            if (isNew) {
                Object spawnPacket = spawnConstructor.newInstance(wither);
                sendPacketMethod.invoke(connection, spawnPacket);
            } else {
                Object teleportPacket = teleportConstructor.newInstance(wither);
                sendPacketMethod.invoke(connection, teleportPacket);

                Object dataWatcher = getDataWatcherMethod.invoke(wither);
                Object metaPacket = metadataConstructor.newInstance(getIdMethod.invoke(wither), dataWatcher, true);
                sendPacketMethod.invoke(connection, metaPacket);
            }
        } catch (Exception ignored) { }
    }

    public static void removeBossBar(Player player) {
        try {
            Object wither = activeWithers.remove(player.getUniqueId());
            if (wither != null) {
                int id = (int) getIdMethod.invoke(wither);
                Object destroyPacket = destroyConstructor.newInstance(new Object[]{new int[]{id}});

                Object handle = getHandleMethod.invoke(player);
                Object connection = playerConnectionField.get(handle);
                sendPacketMethod.invoke(connection, destroyPacket);
            }
        } catch (Exception ignored) { }
    }
}