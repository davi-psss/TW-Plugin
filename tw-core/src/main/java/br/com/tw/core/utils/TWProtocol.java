package br.com.tw.core.utils;

import org.bukkit.entity.Player;

public final class TWProtocol {

    private TWProtocol() {
    }

    public static void sendActionBar(Player player, String message) {
        player.sendActionBar(message);
    }
}
