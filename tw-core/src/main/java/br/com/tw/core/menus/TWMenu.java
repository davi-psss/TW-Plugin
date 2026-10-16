package br.com.tw.core.menus;

import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.InventoryHolder;

public interface TWMenu extends InventoryHolder {
    // Este método será chamado automaticamente sempre que alguém clicar no menu
    void onClick(InventoryClickEvent event);
}