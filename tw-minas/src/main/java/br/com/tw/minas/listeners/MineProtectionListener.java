package br.com.tw.minas.listeners;

import br.com.tw.core.TWCorePlugin;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.inventory.ItemStack;

public class MineProtectionListener implements Listener {
    @EventHandler(ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent event) {
        Player player = event.getPlayer();
        if (!isMine(player) || (player.hasPermission("tw.admin") && player.getGameMode() == GameMode.CREATIVE)) return;
        event.setCancelled(true);
    }

    @EventHandler(ignoreCancelled = true)
    public void onPickaxeDrop(PlayerDropItemEvent event) {
        Player player = event.getPlayer();
        if (!isMine(player) || !isPickaxe(event.getItemDrop().getItemStack())) return;
        event.setCancelled(true);
        player.sendMessage("§cNão podes dropar a picareta na mina!");
    }

    @EventHandler(ignoreCancelled = true)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        Player player = (Player) event.getWhoClicked();
        if (!isMine(player)) return;

        ItemStack hotbarItem = event.getHotbarButton() < 0
                ? null
                : player.getInventory().getItem(event.getHotbarButton());
        boolean swapsOffhandPickaxe = event.getClick() == ClickType.SWAP_OFFHAND
                && (isPickaxe(player.getInventory().getItemInMainHand())
                || isPickaxe(player.getInventory().getItemInOffHand()));
        if (isPickaxe(event.getCurrentItem())
                || isPickaxe(event.getCursor())
                || isPickaxe(hotbarItem)
                || swapsOffhandPickaxe) {
            event.setCancelled(true);
            player.sendMessage("§cNão podes mover a picareta na mina!");
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onInventoryDrag(InventoryDragEvent event) {
        if (!(event.getWhoClicked() instanceof Player) || !isPickaxe(event.getOldCursor())) return;
        Player player = (Player) event.getWhoClicked();
        if (!isMine(player)) return;
        event.setCancelled(true);
        player.sendMessage("§cNão podes mover a picareta na mina!");
    }

    private boolean isMine(Player player) {
        return TWCorePlugin.getInstance().isMineWorld(player.getWorld().getName());
    }

    private boolean isPickaxe(ItemStack item) {
        return item != null && item.getType() != Material.AIR && item.getType().name().endsWith("_PICKAXE");
    }
}
