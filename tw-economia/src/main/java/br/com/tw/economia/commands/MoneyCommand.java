package br.com.tw.economia.commands;

import br.com.tw.core.TWCorePlugin;
import br.com.tw.core.data.TWUser;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class MoneyCommand implements CommandExecutor {
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) return true;
        Player player = (Player) sender;

        TWUser user = TWCorePlugin.getInstance().getUserManager().getUser(player.getUniqueId());
        if (user != null) {
            String saldoFormatado = br.com.tw.core.utils.TWFormat.formatarDinheiro(user.getBalance());
            player.sendMessage(ChatColor.GREEN + "Saldo atual: " + ChatColor.YELLOW + "$" + saldoFormatado);
        } else {
            player.sendMessage(ChatColor.RED + "Não foi possível carregar o teu saldo.");
        }
        return true;
    }
}