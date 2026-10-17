package br.com.tw.core.utils;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.logging.Level;

public final class TWItemTags {

    private static volatile NmsAccess cachedAccess;

    private TWItemTags() {
    }

    public static ItemStack set(ItemStack item, Map<String, Object> tags) {
        if (isEmpty(item) || tags == null || tags.isEmpty()) return item;

        try {
            NmsAccess access = getAccess();
            Object nmsItem = access.asNmsCopy.invoke(null, item);
            Object compound = access.getTag.invoke(nmsItem);
            if (compound == null) compound = access.compoundConstructor.newInstance();

            for (Map.Entry<String, Object> entry : tags.entrySet()) {
                setValue(access, compound, entry.getKey(), entry.getValue());
            }
            access.setTag.invoke(nmsItem, compound);
            return (ItemStack) access.asBukkitCopy.invoke(null, nmsItem);
        } catch (ReflectiveOperationException | SecurityException e) {
            throw failure("Não foi possível gravar as tags NBT do item.", e);
        }
    }

    public static boolean has(ItemStack item, String key) {
        if (isEmpty(item)) return false;
        try {
            NmsAccess access = getAccess();
            Object nmsItem = access.asNmsCopy.invoke(null, item);
            Object compound = access.getTag.invoke(nmsItem);
            return compound != null && (Boolean) access.hasKey.invoke(compound, key);
        } catch (ReflectiveOperationException | SecurityException e) {
            throw failure("Não foi possível ler as tags NBT do item.", e);
        }
    }

    public static String getString(ItemStack item, String key) {
        if (isEmpty(item)) return null;
        try {
            NmsAccess access = getAccess();
            Object compound = access.getTag.invoke(access.asNmsCopy.invoke(null, item));
            if (compound == null || !(Boolean) access.hasKey.invoke(compound, key)) return null;
            return (String) access.getString.invoke(compound, key);
        } catch (ReflectiveOperationException | SecurityException e) {
            throw failure("Não foi possível ler a tag NBT " + key + ".", e);
        }
    }

    public static long getLong(ItemStack item, String key, long fallback) {
        if (isEmpty(item)) return fallback;
        try {
            NmsAccess access = getAccess();
            Object compound = access.getTag.invoke(access.asNmsCopy.invoke(null, item));
            if (compound == null || !(Boolean) access.hasKey.invoke(compound, key)) return fallback;
            return ((Number) access.getLong.invoke(compound, key)).longValue();
        } catch (ReflectiveOperationException | SecurityException e) {
            throw failure("Não foi possível ler a tag NBT " + key + ".", e);
        }
    }

    public static int getInt(ItemStack item, String key, int fallback) {
        if (isEmpty(item)) return fallback;
        try {
            NmsAccess access = getAccess();
            Object compound = access.getTag.invoke(access.asNmsCopy.invoke(null, item));
            if (compound == null || !(Boolean) access.hasKey.invoke(compound, key)) return fallback;
            return ((Number) access.getInt.invoke(compound, key)).intValue();
        } catch (ReflectiveOperationException | SecurityException e) {
            throw failure("Não foi possível ler a tag NBT " + key + ".", e);
        }
    }

    public static double getDouble(ItemStack item, String key, double fallback) {
        if (isEmpty(item)) return fallback;
        try {
            NmsAccess access = getAccess();
            Object compound = access.getTag.invoke(access.asNmsCopy.invoke(null, item));
            if (compound == null || !(Boolean) access.hasKey.invoke(compound, key)) return fallback;
            return ((Number) access.getDouble.invoke(compound, key)).doubleValue();
        } catch (ReflectiveOperationException | SecurityException e) {
            throw failure("Não foi possível ler a tag NBT " + key + ".", e);
        }
    }

    public static boolean getBoolean(ItemStack item, String key, boolean fallback) {
        if (isEmpty(item)) return fallback;
        try {
            NmsAccess access = getAccess();
            Object compound = access.getTag.invoke(access.asNmsCopy.invoke(null, item));
            if (compound == null || !(Boolean) access.hasKey.invoke(compound, key)) return fallback;
            return (Boolean) access.getBoolean.invoke(compound, key);
        } catch (ReflectiveOperationException | SecurityException e) {
            throw failure("Não foi possível ler a tag NBT " + key + ".", e);
        }
    }

    private static void setValue(NmsAccess access, Object compound, String key, Object value)
            throws ReflectiveOperationException {
        if (value instanceof String) {
            access.setString.invoke(compound, key, value);
        } else if (value instanceof Boolean) {
            access.setBoolean.invoke(compound, key, value);
        } else if (value instanceof Integer) {
            access.setInt.invoke(compound, key, value);
        } else if (value instanceof Long) {
            access.setLong.invoke(compound, key, value);
        } else if (value instanceof Double) {
            access.setDouble.invoke(compound, key, value);
        } else {
            throw new IllegalArgumentException("Tipo de tag NBT não suportado: "
                    + (value == null ? "null" : value.getClass().getName()));
        }
    }

    private static boolean isEmpty(ItemStack item) {
        return item == null || item.getType() == Material.AIR || item.getAmount() <= 0;
    }

    private static NmsAccess getAccess() throws ReflectiveOperationException {
        NmsAccess access = cachedAccess;
        if (access == null) {
            synchronized (TWItemTags.class) {
                access = cachedAccess;
                if (access == null) {
                    access = NmsAccess.create();
                    cachedAccess = access;
                }
            }
        }
        return access;
    }

    private static IllegalStateException failure(String message, Exception cause) {
        Bukkit.getLogger().log(Level.SEVERE, message, cause);
        return new IllegalStateException(message, cause);
    }

    private static final class NmsAccess {
        private final Method asNmsCopy;
        private final Method asBukkitCopy;
        private final Method getTag;
        private final Method setTag;
        private final Constructor<?> compoundConstructor;
        private final Method hasKey;
        private final Method getString;
        private final Method getLong;
        private final Method getInt;
        private final Method getDouble;
        private final Method getBoolean;
        private final Method setString;
        private final Method setLong;
        private final Method setInt;
        private final Method setDouble;
        private final Method setBoolean;

        private NmsAccess(Class<?> craftItemStack, Class<?> nmsItemStack, Class<?> compound)
                throws ReflectiveOperationException {
            asNmsCopy = craftItemStack.getMethod("asNMSCopy", ItemStack.class);
            asBukkitCopy = craftItemStack.getMethod("asBukkitCopy", nmsItemStack);
            getTag = nmsItemStack.getMethod("getTag");
            setTag = nmsItemStack.getMethod("setTag", compound);
            compoundConstructor = compound.getConstructor();
            hasKey = compound.getMethod("hasKey", String.class);
            getString = compound.getMethod("getString", String.class);
            getLong = compound.getMethod("getLong", String.class);
            getInt = compound.getMethod("getInt", String.class);
            getDouble = compound.getMethod("getDouble", String.class);
            getBoolean = compound.getMethod("getBoolean", String.class);
            setString = compound.getMethod("setString", String.class, String.class);
            setLong = compound.getMethod("setLong", String.class, long.class);
            setInt = compound.getMethod("setInt", String.class, int.class);
            setDouble = compound.getMethod("setDouble", String.class, double.class);
            setBoolean = compound.getMethod("setBoolean", String.class, boolean.class);
        }

        private static NmsAccess create() throws ReflectiveOperationException {
            String version = Bukkit.getServer().getClass().getPackage().getName().split("\\.")[3];
            String craftPackage = "org.bukkit.craftbukkit." + version + ".";
            String nmsPackage = "net.minecraft.server." + version + ".";
            Class<?> craftItemStack = Class.forName(craftPackage + "inventory.CraftItemStack");
            Class<?> nmsItemStack = Class.forName(nmsPackage + "ItemStack");
            Class<?> compound = Class.forName(nmsPackage + "NBTTagCompound");
            return new NmsAccess(craftItemStack, nmsItemStack, compound);
        }
    }
}
