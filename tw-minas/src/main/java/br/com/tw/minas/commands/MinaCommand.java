package br.com.tw.minas.commands;

import br.com.tw.minas.menus.MinaMainMenu;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class MinaCommand implements CommandExecutor {
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (sender instanceof Player) {
            new MinaMainMenu((Player) sender).open();
        }
        return true;
    }
}