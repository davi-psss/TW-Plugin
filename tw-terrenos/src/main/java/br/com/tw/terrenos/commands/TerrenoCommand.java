package br.com.tw.terrenos.commands;

import br.com.tw.terrenos.menus.TerrenosMenu;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class TerrenoCommand implements CommandExecutor {
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (sender instanceof Player) {
            new TerrenosMenu((Player) sender).open();
        }
        return true;
    }
}