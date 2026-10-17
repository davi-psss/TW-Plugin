package br.com.tw.economia.listeners;

import br.com.tw.core.TWCorePlugin;
import br.com.tw.core.data.TWUser;
import br.com.tw.core.utils.TWItemTags;
import br.com.tw.economia.TWEconomiaPlugin;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

import java.util.List;

public class ChequeListener implements Listener {

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        if (event.getAction() == Action.RIGHT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_BLOCK) {
            ItemStack item = player.getItemInHand();

            if (item != null && item.getType() == Material.PAPER && item.hasItemMeta() && item.getItemMeta().hasDisplayName()) {
                if (item.getItemMeta().getDisplayName().equals(ChatColor.GREEN + "§lCheque Bancário")) {
                    event.setCancelled(true);
                    if (!TWItemTags.getBoolean(item, "tw_bank_cheque", false)
                            || !TWItemTags.has(item, "tw_cheque_value")
                            || !TWItemTags.has(item, "tw_cheque_id")) {
                        player.sendMessage(ChatColor.RED + "Este cheque é inválido ou está corrompido.");
                        return;
                    }
                    List<String> lore = item.getItemMeta().getLore();
                    if (lore == null || lore.size() < 3) {
                        player.sendMessage(ChatColor.RED + "Este cheque é inválido ou está corrompido.");
                        return;
                    }
                    double amount = TWItemTags.getDouble(item, "tw_cheque_value", 0.0);
                    String chequeId = TWItemTags.getString(item, "tw_cheque_id");
                    if (!Double.isFinite(amount) || amount <= 0) {
                        player.sendMessage(ChatColor.RED + "Este cheque é inválido ou está corrompido.");
                        return;
                    }

                    TWUser user = TWCorePlugin.getInstance().getUserManager().getUser(player.getUniqueId());
                    if (user == null) {
                        player.sendMessage(ChatColor.RED + "Os teus dados ainda estão a ser carregados.");
                        return;
                    }
                    if (!TWCorePlugin.getInstance().getUserManager().flushUsersSync()) {
                        player.sendMessage(ChatColor.RED + "Não foi possível preparar o resgate agora.");
                        return;
                    }
                    java.util.OptionalDouble redeemedBalance;
                    try {
                        redeemedBalance = TWEconomiaPlugin.getInstance().getChequeRepository()
                                .redeem(chequeId, user.getUuid(), amount);
                    } catch (IllegalStateException e) {
                        player.sendMessage(ChatColor.RED + "Não foi possível resgatar o cheque agora.");
                        return;
                    }
                    if (!redeemedBalance.isPresent()) {
                        player.sendMessage(ChatColor.RED + "Este cheque já foi resgatado ou é inválido.");
                        return;
                    }
                    user.setBalance(redeemedBalance.getAsDouble());

                    if (item.getAmount() > 1) {
                        item.setAmount(item.getAmount() - 1);
                    } else {
                        player.setItemInHand(null);
                    }
                    TWCorePlugin.getInstance().getUserManager().saveUserAsync(user);
                    player.updateInventory();
                    player.sendMessage(ChatColor.GREEN + "Resgataste um cheque de $" + String.format("%,.2f", amount) + "!");
                    player.playSound(player.getLocation(), Sound.LEVEL_UP, 1.0f, 1.5f);
                }
            }
        }
    }
}