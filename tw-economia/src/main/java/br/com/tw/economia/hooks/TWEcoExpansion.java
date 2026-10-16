package br.com.tw.economia.hooks;

import br.com.tw.core.TWCorePlugin;
import br.com.tw.core.data.TWUser;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.entity.Player;

public class TWEcoExpansion extends PlaceholderExpansion {

    @Override
    public String getIdentifier() {
        return "tweco";
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
        if (user == null) return "0,00";

        if (identifier.equalsIgnoreCase("saldo")) {
            return br.com.tw.core.utils.TWFormat.formatarDinheiro(user.getBalance());
        }

        return null;
    }
}