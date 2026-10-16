package br.com.tw.minas.menus;

import br.com.tw.core.TWCorePlugin;
import br.com.tw.core.menus.TWMenu;
import br.com.tw.minas.TWMinasPlugin;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.Arrays;
import java.util.Map;

public class RankingMenu implements TWMenu {
    private final Inventory inventory;
    private final Player player;

    public RankingMenu(Player player) {
        this.player = player;
        this.inventory = Bukkit.createInventory(this, 36, ChatColor.GOLD + "§lRanking dos Melhores");

        // Coloca os itens de "A carregar..."
        setupLoadingItems();

        // Puxa os dados reais de forma segura (sem travar o servidor)
        loadRankingAsync();
    }

    private void setupLoadingItems() {
        inventory.setItem(11, createLoadingSkull("§e§l1º Lugar"));
        inventory.setItem(13, createLoadingSkull("§f§l2º Lugar"));
        inventory.setItem(15, createLoadingSkull("§6§l3º Lugar"));
        inventory.setItem(31, createItem(Material.WOOD_DOOR, "§cVoltar", "§7Retornar à Central."));
    }

    private void loadRankingAsync() {
        Bukkit.getScheduler().runTaskAsynchronously(TWMinasPlugin.getInstance(), () -> {
            Map<String, Long> topMiners = TWCorePlugin.getInstance().getUserManager().getTopMiners(3);

            // Volta para a thread principal para atualizar o inventário
            Bukkit.getScheduler().runTask(TWMinasPlugin.getInstance(), () -> {
                int[] slots = {11, 13, 15};
                String[] titulos = {"§e§l1º O Rei da Mina", "§f§l2º Prata", "§6§l3º Bronze"};

                int i = 0;
                for (Map.Entry<String, Long> entry : topMiners.entrySet()) {
                    if (i >= 3) break;
                    String nome = entry.getKey();
                    long blocos = entry.getValue();

                    inventory.setItem(slots[i], getPlayerSkull(nome, titulos[i], "§7Blocos Quebrados: §b" + String.format("%,d", blocos)));
                    i++;
                }
            });
        });
    }

    @Override
    public void onClick(InventoryClickEvent event) {
        int slot = event.getSlot();
        if (slot == 31) {
            player.playSound(player.getLocation(), Sound.CLICK, 1f, 1f);
            new MinaMainMenu(player).open();
        }
    }

    @Override
    public Inventory getInventory() { return inventory; }

    public void open() {
        player.openInventory(inventory);
    }

    private ItemStack createItem(Material mat, String name, String... lore) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(name);
        meta.setLore(Arrays.asList(lore));
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createLoadingSkull(String titulo) {
        ItemStack skull = new ItemStack(Material.SKULL_ITEM, 1, (short) 3);
        SkullMeta meta = (SkullMeta) skull.getItemMeta();
        meta.setDisplayName(titulo);
        meta.setLore(Arrays.asList("§7A consultar o Banco de Dados..."));
        skull.setItemMeta(meta);
        return skull;
    }

    private ItemStack getPlayerSkull(String playerName, String titulo, String... lore) {
        ItemStack skull = new ItemStack(Material.SKULL_ITEM, 1, (short) 3);
        SkullMeta meta = (SkullMeta) skull.getItemMeta();
        meta.setOwner(playerName);
        meta.setDisplayName(titulo + " §7- §a" + playerName);
        meta.setLore(Arrays.asList(lore));
        skull.setItemMeta(meta);
        return skull;
    }
}