package br.com.tw.minas.menus;

import br.com.tw.core.TWCorePlugin;
import br.com.tw.core.data.TWUser;
import br.com.tw.core.menus.TWMenu;
import br.com.tw.core.utils.TWFormat;
import br.com.tw.core.utils.TWMenuFrame;
import br.com.tw.minas.TWMinasPlugin;
import br.com.tw.minas.managers.MineEventManager;
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

public class MinaMainMenu implements TWMenu {

    private final Inventory inventory;
    private final Player player;

    public MinaMainMenu(Player player) {
        this.player = player;
        this.inventory = Bukkit.createInventory(this, 27, ChatColor.AQUA + "§lCentral de Mineração");
        setupItems(player);
    }

    private void setupItems(Player player) {
        MineEventManager events = TWMinasPlugin.getInstance().getMineEventManager();
        String statusHappyHour = events.isHappyHourActive()
                ? "§a§lATIVO (2.0x Ganhos!)"
                : "§e" + events.getCommunityProgress() + " §7/ §e" + events.getCommunityGoal() + " blocos";

        inventory.setItem(4, createItem(Material.BEACON, "§b§lMeta Comunitária (Happy Hour)",
                "§7Progresso Global: " + statusHappyHour));

        inventory.setItem(11, createItem(Material.MAP, "§d§lCartinhas de Missões",
                "§7Cumpre missões aleatórias na mina por grandes prémios!"));
        inventory.setItem(12, createItem(Material.CHEST, "§6§lCatálogo de Minas",
                "§7Explore todas as minas do servidor."));
        inventory.setItem(13, createItem(Material.DIAMOND_PICKAXE, "§b§lIr Minerar",
                "§7Teleporte instantâneo para a sua mina."));
        inventory.setItem(14, createItem(Material.ANVIL, "§c§lOficina dos Duendes",
                "§7Evolua a sua picareta."));
        inventory.setItem(15, createItem(Material.GOLD_INGOT, "§e§lRanking",
                "§7Veja os melhores jogadores do servidor."));
        inventory.setItem(26, getPlayerStatsItem(player));
        TWMenuFrame.apply(inventory);
    }

    @Override
    public void onClick(InventoryClickEvent event) {
        int slot = event.getSlot();

        if (slot == 13) {
            player.closeInventory();
            player.playSound(player.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 1f, 1f);
            player.sendMessage(ChatColor.AQUA + "A teleportar para a tua melhor mina...");

            TWUser user = TWCorePlugin.getInstance().getUserManager().getUser(player.getUniqueId());
            String warpName = (user != null && !user.getRankId().equalsIgnoreCase("MAX"))
                    ? user.getRankId().toLowerCase() : "carvao";

            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "warp " + warpName + " " + player.getName());
        } else if (slot == 11) {
            player.playSound(player.getLocation(), Sound.ENTITY_BAT_TAKEOFF, 0.5f, 1.5f);
            new CartinhasMenu(player).open();
        } else if (slot == 12) {
            player.playSound(player.getLocation(), Sound.BLOCK_CHEST_OPEN, 1f, 2f);
            new CatalogoMenu(player).open();
        } else if (slot == 14) {
            player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_USE, 1f, 1f);
            new OficinaMenu(player).open();
        } else if (slot == 15) {
            player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1f, 2f);
            new RankingMenu(player).open();
        }
    }

    @Override
    public Inventory getInventory() { return inventory; }

    public void open() {
        player.openInventory(inventory);
        player.playSound(player.getLocation(), Sound.BLOCK_CHEST_OPEN, 0.5f, 1f);
    }

    private ItemStack createItem(Material mat, String name, String lore) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(name);
        meta.setLore(Arrays.asList(lore));
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack getPlayerStatsItem(Player player) {
        ItemStack skull = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) skull.getItemMeta();
        meta.setOwningPlayer(player);
        meta.setDisplayName(ChatColor.YELLOW + "§lEstatísticas Pessoais");

        TWUser user = TWCorePlugin.getInstance().getUserManager().getUser(player.getUniqueId());
        if (user != null) {
            String formatadoMina = TWFormat.formatarDinheiro(user.getDinheiroMina());
            meta.setLore(Arrays.asList(
                    "§7Nível da Picareta: §a" + user.getPickaxeLevel(),
                    "§7Blocos Quebrados: §b" + String.format("%,d", user.getBlocosQuebrados()),
                    "§7Dinheiro Gerado: §2$" + formatadoMina
            ));
        }

        skull.setItemMeta(meta);
        return skull;
    }
}