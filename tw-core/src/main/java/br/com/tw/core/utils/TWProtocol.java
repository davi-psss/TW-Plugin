package br.com.tw.core.utils;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

public final class TWProtocol {

    private static final Set<String> loggedFailures = ConcurrentHashMap.newKeySet();

    private TWProtocol() {
    }

    public static void sendActionBar(JavaPlugin plugin, Player player, String message) {
        try {
            String nms = getNmsPackage();
            Class<?> componentClass = Class.forName(nms + "IChatBaseComponent");
            Class<?> serializerClass = Class.forName(nms + "IChatBaseComponent$ChatSerializer");
            Class<?> packetClass = Class.forName(nms + "PacketPlayOutChat");
            Object component = serializerClass.getMethod("a", String.class)
                    .invoke(null, toJson(message));
            Object packet = packetClass.getConstructor(componentClass, byte.class).newInstance(component, (byte) 2);
            sendPacket(plugin, player, packet);
        } catch (ReflectiveOperationException | IllegalArgumentException e) {
            throw failure(plugin, "Não foi possível preparar a ActionBar para " + player.getName() + ".", e);
        }
    }

    public static void sendPacket(JavaPlugin plugin, Player player, Object packet) {
        try {
            String craft = getCraftPackage();
            Object handle = Class.forName(craft + "entity.CraftPlayer")
                    .getMethod("getHandle").invoke(player);
            Field connectionField = handle.getClass().getField("playerConnection");
            Object connection = connectionField.get(handle);
            Method sendPacket = null;
            for (Method method : connection.getClass().getMethods()) {
                if (method.getName().equals("sendPacket")
                        && method.getParameterTypes().length == 1
                        && method.getParameterTypes()[0].isAssignableFrom(packet.getClass())) {
                    sendPacket = method;
                    break;
                }
            }
            if (sendPacket == null) {
                throw new NoSuchMethodException("Método sendPacket não encontrado em playerConnection.");
            }
            sendPacket.invoke(connection, packet);
        } catch (ReflectiveOperationException | SecurityException e) {
            throw failure(plugin, "Não foi possível enviar um pacote para " + player.getName() + ".", e);
        }
    }

    public static String getNmsPackage() {
        return "net.minecraft.server." + getServerVersion() + ".";
    }

    public static String getCraftPackage() {
        return "org.bukkit.craftbukkit." + getServerVersion() + ".";
    }

    private static String getServerVersion() {
        String[] parts = Bukkit.getServer().getClass().getPackage().getName().split("\\.");
        if (parts.length < 4) {
            throw new IllegalStateException("Não foi possível identificar a versão interna do servidor.");
        }
        return parts[3];
    }

    private static String toJson(String text) {
        return "{\"text\":\"" + text.replace("\\", "\\\\").replace("\"", "\\\"")
                .replace("\n", "\\n").replace("\r", "\\r") + "\"}";
    }

    private static IllegalStateException failure(JavaPlugin plugin, String message, Exception cause) {
        String key = plugin.getName() + ":" + cause.getClass().getName();
        if (loggedFailures.add(key)) {
            plugin.getLogger().log(Level.SEVERE, message, cause);
        }
        return new IllegalStateException(message, cause);
    }
}
