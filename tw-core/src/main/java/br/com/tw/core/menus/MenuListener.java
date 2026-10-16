package br.com.tw.core.menus;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;

public class MenuListener implements Listener {

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        Inventory inv = event.getInventory();

        // Verifica se o inventário implementa a nossa interface TWMenu
        if (inv != null && inv.getHolder() instanceof TWMenu) {
            event.setCancelled(true); // Bloqueia pegar o item

            if (event.getCurrentItem() == null) return;

            // Passa a responsabilidade do clique para a própria classe do menu
            TWMenu menu = (TWMenu) inv.getHolder();
            menu.onClick(event);
        }
    }
}