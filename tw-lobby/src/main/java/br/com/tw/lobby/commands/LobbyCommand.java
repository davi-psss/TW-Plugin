package br.com.tw.lobby.commands;

import br.com.tw.lobby.TWLobbyPlugin;
import org.bukkit.ChatColor;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class LobbyCommand implements CommandExecutor {
    private final TWLobbyPlugin plugin;

    public LobbyCommand(TWLobbyPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) return true;
        Player player = (Player) sender;

        player.teleport(plugin.getLobbyLocation());
        player.sendMessage(ChatColor.AQUA + "Foste teleportado para o Lobby!");
        player.playSound(player.getLocation(), Sound.ENDERMAN_TELEPORT, 1f, 1f);
        return true;
    }
}