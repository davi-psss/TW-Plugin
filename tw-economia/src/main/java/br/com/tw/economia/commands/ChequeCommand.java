package br.com.tw.economia.commands;

import br.com.tw.core.TWCorePlugin;
import br.com.tw.core.data.TWUser;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Arrays;

public class ChequeCommand implements CommandExecutor {

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) return true;
        Player player = (Player) sender;

        if (args.length < 1) {
            player.sendMessage(ChatColor.RED + "Uso correto: /cheque <quantia>");
            return true;
        }

        double amount;
        try {
            amount = Double.parseDouble(args[0]);
            if (amount <= 0) throw new NumberFormatException();
        } catch (NumberFormatException e) {
            player.sendMessage(ChatColor.RED + "Quantia inválida. Usa apenas números positivos.");
            return true;
        }

        TWUser user = TWCorePlugin.getInstance().getUserManager().getUser(player.getUniqueId());
        if (user != null) {
            if (user.removeBalance(amount)) {
                ItemStack cheque = new ItemStack(Material.PAPER);
                ItemMeta meta = cheque.getItemMeta();
                meta.setDisplayName(ChatColor.GREEN + "§lCheque Bancário");

                // O valor cru fica escondido na Lore para facilitar a leitura no listener
                meta.setLore(Arrays.asList(
                        ChatColor.GRAY + "Assinado por: " + ChatColor.AQUA + player.getName(),
                        ChatColor.GRAY + "Valor: " + ChatColor.GREEN + "$" + String.format("%,.2f", amount),
                        ChatColor.GRAY + "Clica com o botão direito para resgatar.",
                        ChatColor.BLACK + String.valueOf(amount)
                ));
                cheque.setItemMeta(meta);

                player.getInventory().addItem(cheque);
                player.sendMessage(ChatColor.GREEN + "Criaste um cheque de $" + String.format("%,.2f", amount) + ".");
                player.playSound(player.getLocation(), Sound.ORB_PICKUP, 1.0f, 1.0f);
            } else {
                player.sendMessage(ChatColor.RED + "Não tens saldo suficiente.");
            }
        }
        return true;
    }
}
