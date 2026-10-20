package br.com.tw.spawners.utils;

import br.com.tw.core.utils.TWProtocol;
import br.com.tw.spawners.TWSpawnersPlugin;
import org.bukkit.entity.Player;

public final class SpawnerActionBarUtil {

    private SpawnerActionBarUtil() {
    }

    public static void send(Player player, String message) {
        TWProtocol.sendActionBar(player, message);
    }
}
