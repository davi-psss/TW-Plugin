package br.com.tw.ranks.menus;

import br.com.tw.core.TWCorePlugin;
import br.com.tw.core.data.TWUser;
import br.com.tw.core.menus.TWMenu;
import br.com.tw.ranks.TWRanksPlugin;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Arrays;
import java.util.List;

public class RanksMenu implements TWMenu {
    private final Inventory inventory;
    private final Player player;
    private final TWUser user;

    // Lista ordenada dos Ranks do servidor
    private final List<String> rankOrder = Arrays.asList("CARVAO", "NEVE", "SINO", "BISCOITO", "DUENDE", "RENA", "TRENO", "QUEBRANOZES", "ESTRELA", "NOEL");

    public RanksMenu(Player player) {
        this.player = player;
        this.user = TWCorePlugin.getInstance().getUserManager().getUser(player.getUniqueId());
        this.inventory = Bukkit.createInventory(this, 54, ChatColor.DARK_GRAY + "Árvore de Evolução");
        setupItems();
    }

    private void setupItems() {
        if (user == null) return;
        String currentRank = user.getRankId();
        int currentIndex = currentRank.equalsIgnoreCase("MAX") ? 999 : rankOrder.indexOf(currentRank.toUpperCase());

        // Desenha a linha do tempo (Slots 10 a 16, 19 a 25, etc)
        int[] slots = {10, 11, 12, 13, 14, 15, 16, 28, 29, 30};

        for (int i = 0; i < rankOrder.size(); i++) {
            String rankName = rankOrder.get(i);
            int slot = slots[i];

            if (i < currentIndex) {
                // Ranks já passados (Vidro Verde)
                inventory.setItem(slot, createItem(Material.STAINED_GLASS_PANE, 5, "§a§l" + rankName, "§7Rank já desbloqueado!"));
            } else if (i == currentIndex) {
                // Rank Atual (Destaque)
                inventory.setItem(slot, createItem(Material.EMERALD_BLOCK, 0, "§e§l" + rankName, "§7Este é o teu rank atual."));
            } else {
                // Próximos Ranks (Vidro Vermelho)
                double cost = TWRanksPlugin.getInstance().getRanksConfig().getDouble("ranks." + rankOrder.get(i - 1) + ".cost");
                inventory.setItem(slot, createItem(Material.STAINED_GLASS_PANE, 14, "§c§l" + rankName, "§7Custo: §a$" + String.format("%,.2f", cost), "§cBloqueado."));
            }
        }

        // Botão de Ação Inferior Central
        inventory.setItem(49, createItem(Material.EXP_BOTTLE, 0, "§a§lEVOLUIR (RANKUP)", "§7Clica aqui para subir de rank."));
        inventory.setItem(50, createItem(Material.GOLD_BLOCK, 0, "§e§lEVOLUÇÃO MAX", "§7Clica aqui para dar rankup", "§7o máximo que conseguires."));
    }

    @Override
    public void onClick(InventoryClickEvent event) {
        int slot = event.getSlot();
        if (slot == 49) {
            player.closeInventory();
            player.performCommand(user.getRankId().equalsIgnoreCase("MAX") ? "prestigio" : "rankup");
        } else if (slot == 50) {
            player.closeInventory();
            player.performCommand("rankupmax");
        }
    }

    @Override
    public Inventory getInventory() { return inventory; }
    public void open() { player.openInventory(inventory); }

    private ItemStack createItem(Material mat, int data, String name, String... lore) {
        ItemStack item = new ItemStack(mat, 1, (short) data);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(name);
        meta.setLore(Arrays.asList(lore));
        item.setItemMeta(meta);
        return item;
    }
}