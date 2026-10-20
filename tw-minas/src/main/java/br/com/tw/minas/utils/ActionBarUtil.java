package br.com.tw.minas.utils;

import br.com.tw.core.utils.TWProtocol;
import br.com.tw.minas.TWMinasPlugin;
import org.bukkit.entity.Player;

public final class ActionBarUtil {

    private ActionBarUtil() {
    }

    public static void send(Player player, String message) {
        TWProtocol.sendActionBar(player, message);
    }
}
