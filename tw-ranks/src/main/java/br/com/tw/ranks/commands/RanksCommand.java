package br.com.tw.ranks.commands;

import br.com.tw.ranks.menus.RanksMenu;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class RanksCommand implements CommandExecutor {

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "Apenas jogadores podem abrir o menu de ranks.");
            return true;
        }

        Player player = (Player) sender;
        // Abre o menu GUI para o jogador
        new RanksMenu(player).open();

        return true;
    }
}