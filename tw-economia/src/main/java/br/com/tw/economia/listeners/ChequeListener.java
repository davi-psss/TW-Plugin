package br.com.tw.economia.listeners;

import br.com.tw.core.TWCorePlugin;
import br.com.tw.core.data.TWUser;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

public class ChequeListener implements Listener {

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        if (event.getAction() == Action.RIGHT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_BLOCK) {
            ItemStack item = player.getItemInHand();

            if (item != null && item.getType() == Material.PAPER && item.hasItemMeta() && item.getItemMeta().hasDisplayName()) {
                if (item.getItemMeta().getDisplayName().equals(ChatColor.GREEN + "§lCheque Bancário")) {

                    try {
                        // Lê a 4ª linha da Lore (índice 3) onde escondemos o valor cru
                        String valorRaw = ChatColor.stripColor(item.getItemMeta().getLore().get(3));
                        double amount = Double.parseDouble(valorRaw);

                        TWUser user = TWCorePlugin.getInstance().getUserManager().getUser(player.getUniqueId());
                        if (user != null) {
                            user.addBalance(amount);

                            // Remove 1 cheque da mão do jogador
                            if (item.getAmount() > 1) {
                                item.setAmount(item.getAmount() - 1);
                            } else {
                                player.setItemInHand(null);
                            }
                            player.updateInventory();

                            player.sendMessage(ChatColor.GREEN + "Resgataste um cheque de $" + String.format("%,.2f", amount) + "!");
                            player.playSound(player.getLocation(), Sound.LEVEL_UP, 1.0f, 1.5f);
                        }
                    } catch (Exception e) {
                        player.sendMessage(ChatColor.RED + "Este cheque é inválido ou está corrompido.");
                    }
                }
            }
        }
    }
}