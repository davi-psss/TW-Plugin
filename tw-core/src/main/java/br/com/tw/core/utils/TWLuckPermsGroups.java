package br.com.tw.core.utils;

import java.util.Locale;

public final class TWLuckPermsGroups {
    private TWLuckPermsGroups() {
    }

    public static String rank(String rankId) {
        String normalized = normalize(rankId);
        if (normalized.startsWith("RANK_")) normalized = normalized.substring("RANK_".length());
        if (normalized.equals("CARVAO")) return "default";
        if (normalized.equals("MAX")) return "noel";
        return normalized.toLowerCase(Locale.ROOT);
    }

    public static String prestige(String prestigeId) {
        String normalized = normalize(prestigeId);
        if (normalized.startsWith("PRESTIGIO_")) normalized = normalized.substring("PRESTIGIO_".length());
        return normalized.toLowerCase(Locale.ROOT);
    }

    private static String normalize(String id) {
        if (id == null || id.trim().isEmpty()) {
            throw new IllegalArgumentException("O identificador do grupo LuckPerms é obrigatório.");
        }
        return id.trim().toUpperCase(Locale.ROOT);
    }
}
