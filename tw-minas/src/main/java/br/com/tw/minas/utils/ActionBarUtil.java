package br.com.tw.minas.utils;

import org.bukkit.entity.Player;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

public class ActionBarUtil {

    private static Method getHandleMethod;
    private static Field playerConnectionField;
    private static Method sendPacketMethod;
    private static Method chatSerializerMethod;
    private static Constructor<?> packetConstructor;

    static {
        try {
            Class<?> craftPlayerClass = Class.forName("org.bukkit.craftbukkit.v1_8_R3.entity.CraftPlayer");
            Class<?> packetPlayOutChatClass = Class.forName("net.minecraft.server.v1_8_R3.PacketPlayOutChat");
            Class<?> packetClass = Class.forName("net.minecraft.server.v1_8_R3.Packet");
            Class<?> chatComponentClass = Class.forName("net.minecraft.server.v1_8_R3.IChatBaseComponent");
            Class<?> chatSerializerClass = Class.forName("net.minecraft.server.v1_8_R3.IChatBaseComponent$ChatSerializer");
            Class<?> entityPlayerClass = Class.forName("net.minecraft.server.v1_8_R3.EntityPlayer");
            Class<?> playerConnectionClass = Class.forName("net.minecraft.server.v1_8_R3.PlayerConnection");

            getHandleMethod = craftPlayerClass.getMethod("getHandle");
            playerConnectionField = entityPlayerClass.getField("playerConnection");
            sendPacketMethod = playerConnectionClass.getMethod("sendPacket", packetClass);
            chatSerializerMethod = chatSerializerClass.getMethod("a", String.class);
            packetConstructor = packetPlayOutChatClass.getConstructor(chatComponentClass, byte.class);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void send(Player player, String message) {
        if (packetConstructor == null) return;
        try {
            Object chatComponent = chatSerializerMethod.invoke(null, "{\"text\": \"" + message.replace("\"", "\\\"") + "\"}");
            Object packet = packetConstructor.newInstance(chatComponent, (byte) 2);
            Object handle = getHandleMethod.invoke(player);
            Object connection = playerConnectionField.get(handle);
            sendPacketMethod.invoke(connection, packet);
        } catch (Exception ignored) {}
    }
}