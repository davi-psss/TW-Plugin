package br.com.tw.farms.commands;

import br.com.tw.farms.managers.FarmManager;
import br.com.tw.farms.menus.FarmShopMenu;
import br.com.tw.farms.menus.FarmStorageMenu;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public final class FarmCommand implements CommandExecutor {
    private final FarmManager manager;

    public FarmCommand(FarmManager manager) {
        this.manager = manager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "Este comando só pode ser usado por jogadores.");
            return true;
        }
        Player player = (Player) sender;
        if (args.length > 0 && (args[0].equalsIgnoreCase("loja") || args[0].equalsIgnoreCase("shop"))) {
            new FarmShopMenu(player, manager).open();
        } else if (args.length == 0 || args[0].equalsIgnoreCase("silo")
                || args[0].equalsIgnoreCase("armazem") || args[0].equalsIgnoreCase("armazém")) {
            new FarmStorageMenu(player, manager).open();
        } else {
            player.sendMessage(ChatColor.YELLOW + "Usa /farm, /farm silo ou /farm loja.");
        }
        return true;
    }
}
