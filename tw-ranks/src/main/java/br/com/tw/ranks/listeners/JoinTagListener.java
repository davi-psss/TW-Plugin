package br.com.tw.ranks.listeners;

import br.com.tw.core.TWCorePlugin;
import br.com.tw.core.data.TWUser;
import br.com.tw.ranks.TWRanksPlugin;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

public class JoinTagListener implements Listener {

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();

        TWUser user = TWCorePlugin.getInstance().getUserManager().getUser(player.getUniqueId());

        if (user != null) {
            String rankId = user.getRankId();
            String prestigeId = user.getPrestigeId();

            // 1. Sincronização com o LuckPerms (Define o grupo principal)
            // O grupo no LuckPerms precisa ter o mesmo nome do rank (ex: carvao, ferro, ouro)
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "lp user " + player.getName() + " parent set rank_" + rankId.toLowerCase());

            // 2. Configuração Visual da Tag
            String rankTag = TWRanksPlugin.getInstance().getConfig().getString("ranks." + rankId + ".tag", "&8[" + rankId + "]");

            String prefix = rankTag;
            if (!prestigeId.equals("NENHUM")) {
                String prestigeTag = TWRanksPlugin.getInstance().getConfig().getString("prestigios." + prestigeId + ".tag", "&c[" + prestigeId + "]");
                prefix = prestigeTag + " " + rankTag;
            }

            prefix = ChatColor.translateAlternateColorCodes('&', prefix);

            // Aplica no TAB
            String tabName = prefix + " " + ChatColor.GRAY + player.getName();
            if (tabName.length() > 32) {
                tabName = tabName.substring(0, 32);
            }
            player.setPlayerListName(tabName);

            // Aplica no Chat (DisplayName)
            player.setDisplayName(prefix + " " + ChatColor.GRAY + player.getName() + ChatColor.RESET);
        }
    }
}