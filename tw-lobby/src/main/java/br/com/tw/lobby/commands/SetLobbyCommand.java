package br.com.tw.lobby.commands;

import br.com.tw.lobby.TWLobbyPlugin;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class SetLobbyCommand implements CommandExecutor {
    private final TWLobbyPlugin plugin;

    public SetLobbyCommand(TWLobbyPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) return true;
        Player player = (Player) sender;

        if (!player.hasPermission("tw.admin")) {
            player.sendMessage(ChatColor.RED + "Sem permissão.");
            return true;
        }

        Location loc = player.getLocation();
        plugin.getConfig().set("lobby.world", loc.getWorld().getName());
        plugin.getConfig().set("lobby.x", loc.getX());
        plugin.getConfig().set("lobby.y", loc.getY());
        plugin.getConfig().set("lobby.z", loc.getZ());
        plugin.getConfig().set("lobby.yaw", loc.getYaw());
        plugin.getConfig().set("lobby.pitch", loc.getPitch());
        plugin.saveConfig();

        player.sendMessage(ChatColor.GREEN + "Localização do Lobby definida com sucesso!");
        player.playSound(loc, Sound.LEVEL_UP, 1f, 2f);
        return true;
    }
}