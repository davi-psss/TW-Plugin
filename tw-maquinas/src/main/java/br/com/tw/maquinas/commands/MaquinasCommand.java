package br.com.tw.maquinas.commands;

import br.com.tw.maquinas.menus.MachineShopMenu;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class MaquinasCommand implements CommandExecutor {
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (sender instanceof Player) {
            new MachineShopMenu((Player) sender).open();
        }
        return true;
    }
}