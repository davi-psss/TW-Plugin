package br.com.tw.core.menus;

import br.com.tw.core.utils.TWMenuFrame;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Arrays;

public final class TWConfirmationMenu implements TWMenu {

    private final Player player;
    private final Inventory inventory;
    private final Runnable confirmation;
    private boolean completed;

    public TWConfirmationMenu(Player player, String description, Runnable confirmation) {
        this.player = player;
        this.confirmation = confirmation;
        this.inventory = Bukkit.createInventory(this, 27, ChatColor.DARK_RED + "Confirmar ação");
        inventory.setItem(11, createItem(Material.EMERALD_BLOCK, ChatColor.GREEN + "Confirmar", description));
        inventory.setItem(15, createItem(Material.REDSTONE_BLOCK, ChatColor.RED + "Cancelar"));
        TWMenuFrame.apply(inventory);
    }

    @Override
    public void onClick(InventoryClickEvent event) {
        if (completed || event.getClickedInventory() != inventory) return;
        if (event.getSlot() == 11) {
            completed = true;
            player.closeInventory();
            confirmation.run();
        } else if (event.getSlot() == 15) {
            completed = true;
            player.closeInventory();
            player.sendMessage(ChatColor.YELLOW + "Ação cancelada.");
        }
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }

    public void open() {
        player.openInventory(inventory);
    }

    private ItemStack createItem(Material material, String name, String... lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(name);
        meta.setLore(Arrays.asList(lore));
        item.setItemMeta(meta);
        return item;
    }
}
