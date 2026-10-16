package br.com.tw.economia.commands;

import br.com.tw.core.TWCorePlugin;
import br.com.tw.core.data.TWUser;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class PayCommand implements CommandExecutor {
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) return true;
        Player player = (Player) sender;

        if (args.length < 2) {
            player.sendMessage(ChatColor.RED + "Uso correto: /pay <jogador> <quantia>");
            return true;
        }

        Player target = Bukkit.getPlayer(args[0]);
        if (target == null || !target.isOnline()) {
            player.sendMessage(ChatColor.RED + "Jogador não encontrado ou offline.");
            return true;
        }

        if (target.getName().equals(player.getName())) {
            player.sendMessage(ChatColor.RED + "Não podes enviar dinheiro para ti mesmo.");
            return true;
        }

        double amount;
        try {
            amount = Double.parseDouble(args[1]);
            if (amount <= 0) throw new NumberFormatException();
        } catch (NumberFormatException e) {
            player.sendMessage(ChatColor.RED + "Quantia inválida. Usa apenas números positivos.");
            return true;
        }

        TWUser user = TWCorePlugin.getInstance().getUserManager().getUser(player.getUniqueId());
        TWUser targetUser = TWCorePlugin.getInstance().getUserManager().getUser(target.getUniqueId());

        if (user != null && targetUser != null) {
            if (user.removeBalance(amount)) {
                targetUser.addBalance(amount);

                String formatted = br.com.tw.core.utils.TWFormat.formatarDinheiro(amount);
                player.sendMessage(ChatColor.GREEN + "Enviaste $" + formatted + " para " + target.getName() + ".");
                target.sendMessage(ChatColor.GREEN + "Recebeste $" + formatted + " de " + player.getName() + ".");
            } else {
                player.sendMessage(ChatColor.RED + "Não tens saldo suficiente para esta transação.");
            }
        }
        return true;
    }
}