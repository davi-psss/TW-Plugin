package br.com.tw.ranks.hooks;

import br.com.tw.core.TWCorePlugin;
import br.com.tw.core.data.TWUser;
import br.com.tw.ranks.TWRanksPlugin;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

public class TWRankExpansion extends PlaceholderExpansion {

    @Override
    public String getIdentifier() {
        return "tw";
    }

    @Override
    public String getAuthor() {
        return "Servidor";
    }

    @Override
    public String getVersion() {
        return "1.0";
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public String onPlaceholderRequest(Player player, String identifier) {
        if (player == null) return "";

        TWUser user = TWCorePlugin.getInstance().getUserManager().getUser(player.getUniqueId());
        if (user == null) return "Carregando...";

        if (identifier.equalsIgnoreCase("rank")) {
            String rankId = user.getRankId();
            if (rankId == null) return "CARVAO"; // Fallback de segurança
            String prefix = TWRanksPlugin.getInstance().getRanksConfig().getString("ranks." + rankId + ".prefix");
            return prefix != null ? ChatColor.translateAlternateColorCodes('&', prefix) : rankId;
        }

        if (identifier.equalsIgnoreCase("prestigio")) {
            String presId = user.getPrestigeId();
            if (presId == null || presId.equalsIgnoreCase("NENHUM")) return "";
            String prefix = TWRanksPlugin.getInstance().getRanksConfig().getString("prestiges." + presId + ".prefix");
            return prefix != null ? ChatColor.translateAlternateColorCodes('&', prefix) : presId;
        }

        if (identifier.equalsIgnoreCase("tokens")) {
            return String.valueOf(user.getTokens());
        }

        return null;
    }
}