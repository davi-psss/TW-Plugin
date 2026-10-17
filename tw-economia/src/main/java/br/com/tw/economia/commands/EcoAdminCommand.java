package br.com.tw.economia.commands;

import br.com.tw.core.TWCorePlugin;
import br.com.tw.core.data.TWUser;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class EcoAdminCommand implements CommandExecutor {

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("tw.admin")) {
            sender.sendMessage(ChatColor.RED + "Sem permissão.");
            return true;
        }

        if (args.length < 3) {
            sender.sendMessage(ChatColor.RED + "Uso: /eco <give|take|set> <jogador> <quantia>");
            return true;
        }

        String acao = args[0].toLowerCase();
        if (!acao.equals("give") && !acao.equals("add") && !acao.equals("take")
                && !acao.equals("remove") && !acao.equals("set")) {
            sender.sendMessage(ChatColor.RED + "Ação desconhecida. Utiliza: give, take ou set.");
            return true;
        }
        String targetName = args[1];
        double amount;

        try {
            amount = Double.parseDouble(args[2]);
            if (!Double.isFinite(amount) || amount < 0) throw new NumberFormatException();
        } catch (NumberFormatException e) {
            sender.sendMessage(ChatColor.RED + "Quantia inválida. Use apenas números positivos.");
            return true;
        }

        Player target = Bukkit.getPlayer(targetName);

        if (target != null && target.isOnline()) {
            // Jogador está online
            TWUser user = TWCorePlugin.getInstance().getUserManager().getUser(target.getUniqueId());
            if (user != null) {
                aplicarAcaoOnline(sender, user, acao, amount, target.getName());
                TWCorePlugin.getInstance().getUserManager().saveUserAsync(user);
            } else {
                sender.sendMessage(ChatColor.RED + "Os dados do jogador ainda estão a ser carregados.");
            }
        } else {
            // Jogador está offline
            Bukkit.getScheduler().runTaskAsynchronously(TWCorePlugin.getInstance(), () -> {
                boolean sucesso;
                try {
                    sucesso = TWCorePlugin.getInstance().getUserManager().updateOfflineBalance(targetName, amount, acao);
                } catch (IllegalStateException e) {
                    TWCorePlugin.getInstance().getLogger().log(java.util.logging.Level.SEVERE,
                            "Falha ao atualizar o saldo offline de " + targetName + ".", e);
                    Bukkit.getScheduler().runTask(TWCorePlugin.getInstance(),
                            () -> sender.sendMessage(ChatColor.RED + "Não foi possível atualizar o saldo agora."));
                    return;
                }

                Bukkit.getScheduler().runTask(TWCorePlugin.getInstance(), () -> {
                    if (sucesso) {
                        String formatted = String.format("%,.2f", amount);
                        sender.sendMessage(ChatColor.GREEN + "Transação offline realizada! Ação: " + acao + " | Quantia: $" + formatted + " | Jogador: " + targetName);
                    } else {
                        sender.sendMessage(ChatColor.RED + "O jogador '" + targetName + "' não foi encontrado na base de dados.");
                    }
                });
            });
        }

        return true;
    }

    private void aplicarAcaoOnline(CommandSender sender, TWUser user, String acao, double amount, String playerName) {
        String formatted = String.format("%,.2f", amount);
        switch (acao) {
            case "give":
            case "add":
                user.addBalance(amount);
                sender.sendMessage(ChatColor.GREEN + "Adicionaste $" + formatted + " à conta de " + playerName);
                break;
            case "take":
            case "remove":
                if (user.removeBalance(amount)) {
                    sender.sendMessage(ChatColor.GREEN + "Removeste $" + formatted + " da conta de " + playerName);
                } else {
                    sender.sendMessage(ChatColor.RED + "O jogador não tem saldo suficiente. O saldo foi zerado.");
                    user.setBalance(0);
                }
                break;
            case "set":
                user.setBalance(amount);
                sender.sendMessage(ChatColor.GREEN + "Definiste o saldo de " + playerName + " para $" + formatted);
                break;
            default:
                sender.sendMessage(ChatColor.RED + "Ação desconhecida. Utiliza: give, take ou set.");
                break;
        }
    }
}