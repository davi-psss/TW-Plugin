package br.com.tw.terrenos.listeners;

import br.com.tw.core.TWCorePlugin;
import br.com.tw.core.data.TWUser;
import org.bukkit.ChatColor;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;

public class TerrenoListener implements Listener {
    private final double PRECO_TERRENO = 50000.0;

    // Deteta quando o jogador usa o comando /p auto (forçado pelo nosso Menu)
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlotCommand(PlayerCommandPreprocessEvent event) {
        String msg = event.getMessage().toLowerCase();

        if (msg.startsWith("/p auto") || msg.startsWith("/plot auto") || msg.startsWith("/plotsquared:plot auto")) {
            Player player = event.getPlayer();
            TWUser user = TWCorePlugin.getInstance().getUserManager().getUser(player.getUniqueId());

            if (user != null) {
                // Removemos o dinheiro no momento exato em que ele executa o comando de sucesso
                if (user.removeBalance(PRECO_TERRENO)) {
                    TWCorePlugin.getInstance().getUserManager().saveUserAsync(user);
                    player.playSound(player.getLocation(), Sound.LEVEL_UP, 1f, 0.5f);
                    player.sendMessage(ChatColor.GREEN + "Pagaste $" + br.com.tw.core.utils.TWFormat.formatarDinheiro(PRECO_TERRENO) + " pela taxa de aquisição do terreno!");
                }
            }
        }
    }
}