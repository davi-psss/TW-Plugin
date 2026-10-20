package br.com.tw.economia.commands;

import br.com.tw.core.TWCorePlugin;
import br.com.tw.core.data.TWUser;
import br.com.tw.core.utils.TWItemTags;
import br.com.tw.economia.TWEconomiaPlugin;
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
            if (!Double.isFinite(amount) || amount <= 0) throw new NumberFormatException();
        } catch (NumberFormatException e) {
            player.sendMessage(ChatColor.RED + "Quantia inválida. Usa apenas números positivos.");
            return true;
        }

        TWUser user = TWCorePlugin.getInstance().getUserManager().getUser(player.getUniqueId());
        if (user != null) {
            if (TWCorePlugin.getInstance().getUserManager().flushUsersSync()) {
                String chequeId = java.util.UUID.randomUUID().toString();
                java.util.OptionalDouble newBalance;
                try {
                    newBalance = TWEconomiaPlugin.getInstance().getChequeRepository()
                            .issue(chequeId, user.getUuid(), amount);
                } catch (IllegalStateException e) {
                    player.sendMessage(ChatColor.RED + "Não foi possível emitir o cheque agora.");
                    return true;
                }
                if (newBalance.isPresent()) {
                    user.setBalance(newBalance.getAsDouble());
                    ItemStack cheque = new ItemStack(Material.PAPER, 1);
                    ItemMeta meta = cheque.getItemMeta();
                    meta.setDisplayName(ChatColor.GREEN + "§lCheque Bancário");
                    meta.setLore(Arrays.asList(
                            ChatColor.GRAY + "Assinado por: " + ChatColor.AQUA + player.getName(),
                            ChatColor.GRAY + "Valor: " + ChatColor.GREEN + "$" + String.format("%,.2f", amount),
                            ChatColor.GRAY + "Clica com o botão direito para resgatar."
                    ));
                    cheque.setItemMeta(meta);
                    java.util.Map<String, Object> tags = new java.util.HashMap<>();
                    tags.put("tw_bank_cheque", true);
                    tags.put("tw_cheque_id", chequeId);
                    tags.put("tw_cheque_value", amount);
                    cheque = TWItemTags.set(cheque, tags);

                    java.util.Map<Integer, ItemStack> leftovers = player.getInventory().addItem(cheque);
                    for (ItemStack leftover : leftovers.values()) {
                        player.getWorld().dropItemNaturally(player.getLocation(), leftover);
                    }
                    TWCorePlugin.getInstance().getUserManager().saveUserAsync(user);
                    player.sendMessage(ChatColor.GREEN + "Criaste um cheque de $" + String.format("%,.2f", amount) + ".");
                    player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.0f);
                } else {
                    player.sendMessage(ChatColor.RED + "Não tens saldo suficiente.");
                }
            } else {
                player.sendMessage(ChatColor.RED + "Não foi possível salvar os teus dados para criar o cheque.");
            }
        } else {
            player.sendMessage(ChatColor.RED + "Os teus dados ainda estão a ser carregados.");
        }
        return true;
    }
}
