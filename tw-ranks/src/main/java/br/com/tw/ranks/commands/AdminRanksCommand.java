package br.com.tw.ranks.commands;

import br.com.tw.core.TWCorePlugin;
import br.com.tw.core.data.TWUser;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class AdminRanksCommand implements CommandExecutor {
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("tw.admin")) {
            sender.sendMessage(ChatColor.RED + "Sem permissão.");
            return true;
        }

        if (args.length < 3) {
            sender.sendMessage(ChatColor.RED + "Uso: /twadmin <setrank|setprestigio> <jogador> <valor>");
            return true;
        }

        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) {
            sender.sendMessage(ChatColor.RED + "Jogador não encontrado.");
            return true;
        }

        TWUser user = TWCorePlugin.getInstance().getUserManager().getUser(target.getUniqueId());
        if (user == null) {
            sender.sendMessage(ChatColor.RED + "Dados do jogador não carregados.");
            return true;
        }

        String valor = args[2].toUpperCase();

        if (args[0].equalsIgnoreCase("setrank")) {
            user.setRankId(valor);
            sender.sendMessage(ChatColor.GREEN + "Rank de " + target.getName() + " alterado para " + valor);

        } else if (args[0].equalsIgnoreCase("setprestigio")) {
            user.setPrestigeId(valor);
            sender.sendMessage(ChatColor.GREEN + "Prestígio de " + target.getName() + " alterado para " + valor);
        }

        TWCorePlugin.getInstance().syncLuckPermsGroups(target.getName(), user);
        TWCorePlugin.getInstance().getUserManager().saveUserAsync(user);
        return true;
    }
}