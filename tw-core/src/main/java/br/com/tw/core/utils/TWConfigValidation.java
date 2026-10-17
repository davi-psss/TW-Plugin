package br.com.tw.core.utils;

import org.bukkit.plugin.java.JavaPlugin;

public final class TWConfigValidation {

    private TWConfigValidation() {
    }

    public static double requireFiniteAtLeast(JavaPlugin plugin, String path, double value, double minimum) {
        if (!Double.isFinite(value) || value < minimum) {
            throw invalid(plugin, path, "deve ser finito e maior ou igual a " + minimum + ".");
        }
        return value;
    }

    public static double requireProbability(JavaPlugin plugin, String path, double value) {
        if (!Double.isFinite(value) || value < 0.0 || value > 1.0) {
            throw invalid(plugin, path, "deve estar entre 0 e 1.");
        }
        return value;
    }

    public static int requireIntRange(JavaPlugin plugin, String path, int value, int minimum, int maximum) {
        if (value < minimum || value > maximum) {
            throw invalid(plugin, path, "deve estar entre " + minimum + " e " + maximum + ".");
        }
        return value;
    }

    public static String requireText(JavaPlugin plugin, String path, String value) {
        if (value == null || value.trim().isEmpty()) {
            throw invalid(plugin, path, "não pode estar vazio.");
        }
        return value;
    }

    private static IllegalArgumentException invalid(JavaPlugin plugin, String path, String reason) {
        String message = "Configuração inválida em " + path + ": " + reason;
        plugin.getLogger().severe(message);
        return new IllegalArgumentException(message);
    }
}
