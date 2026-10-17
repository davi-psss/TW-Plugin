package br.com.tw.minas.utils;

import br.com.tw.core.utils.TWProtocol;
import br.com.tw.minas.TWMinasPlugin;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

public final class TWBossBar {

    private static final int BAR_SEGMENTS = 20;

    private TWBossBar() {
    }

    public static void sendBossBar(Player player, String message, float progress) {
        if (!player.isOnline()) return;

        float clampedProgress = Math.max(0.0f, Math.min(1.0f, progress));
        int filledSegments = Math.round(clampedProgress * BAR_SEGMENTS);
        StringBuilder bar = new StringBuilder(BAR_SEGMENTS * 2);
        for (int i = 0; i < BAR_SEGMENTS; i++) {
            bar.append(i < filledSegments ? ChatColor.GREEN : ChatColor.DARK_GRAY).append('|');
        }

        String actionBar = message + " " + bar + ChatColor.WHITE + " "
                + Math.round(clampedProgress * 100.0f) + "%";
        TWProtocol.sendActionBar(TWMinasPlugin.getInstance(), player, actionBar);
    }

    public static void removeBossBar(Player player) {
        if (player.isOnline()) {
            TWProtocol.sendActionBar(TWMinasPlugin.getInstance(), player, "");
        }
    }

    public static void removeAll() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            removeBossBar(player);
        }
    }
}
