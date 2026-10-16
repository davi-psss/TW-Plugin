package br.com.tw.spawners.commands;

import br.com.tw.spawners.menus.SpawnerShopMenu;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class SpawnersCommand implements CommandExecutor {
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (sender instanceof Player) {
            new SpawnerShopMenu((Player) sender).open();
        }
        return true;
    }
}