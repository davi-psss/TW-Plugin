package br.com.tw.minas.menus;

import br.com.tw.core.menus.TWMenu;
import br.com.tw.core.utils.TWMenuFrame;
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
import java.util.HashMap;
import java.util.Map;

public class CatalogoMenu implements TWMenu {

    private final Inventory inventory;
    private final Player player;
    private final Map<Integer, WarpInfo> warpsMap = new HashMap<>();

    public CatalogoMenu(Player player) {
        this.player = player;
        this.inventory = Bukkit.createInventory(this, 54, "§8⚒ Catálogo de Minas");

        initWarps();
        setupItems();
    }

    private void initWarps() {
        // Linha 2 e 3: Jornada Principal
        warpsMap.put(10, new WarpInfo("carvao", "group.carvao", Material.COAL_BLOCK, "§8[§0Carvão§8]"));
        warpsMap.put(11, new WarpInfo("neve", "group.neve", Material.SNOW_BLOCK, "§8[§fNeve§8]"));
        warpsMap.put(12, new WarpInfo("sino", "group.sino", Material.GOLD_BLOCK, "§8[§eSino§8]"));
        warpsMap.put(13, new WarpInfo("biscoito", "group.biscoito", Material.BROWN_TERRACOTTA, "§8[§6Biscoito§8]"));
        warpsMap.put(14, new WarpInfo("duende", "group.duende", Material.GREEN_WOOL, "§8[§aDuende§8]"));
        warpsMap.put(19, new WarpInfo("rena", "group.rena", Material.SPRUCE_LOG, "§8[§cRena§8]"));
        warpsMap.put(20, new WarpInfo("treno", "group.treno", Material.QUARTZ_BLOCK, "§8[§4Trenó§8]"));
        warpsMap.put(21, new WarpInfo("quebranozes", "group.quebranozes", Material.RED_TERRACOTTA, "§8[§5Quebra-Nozes§8]"));
        warpsMap.put(22, new WarpInfo("estrela", "group.estrela", Material.GLOWSTONE, "§8[§e§lEstrela§8]"));
        warpsMap.put(23, new WarpInfo("noel", "group.noel", Material.EMERALD_BLOCK, "§8[§c§lNoel§8]"));

        // Linha 4: VIPs
        warpsMap.put(30, new WarpInfo("vipfestivo", "group.vipfestivo", Material.LIME_WOOL, "§a[VIP Festivo]"));
        warpsMap.put(31, new WarpInfo("vipnevasca", "group.vipnevasca", Material.LIGHT_BLUE_WOOL, "§b[VIP Nevasca]"));
        warpsMap.put(32, new WarpInfo("vippolar", "group.vippolar", Material.RED_WOOL, "§c[VIP Polar]"));

        // Linha 5: Prestígios
        warpsMap.put(38, new WarpInfo("grinch", "group.grinch", Material.EMERALD_BLOCK, "§2Prestígio I [Grinch]"));
        warpsMap.put(39, new WarpInfo("yeti", "group.yeti", Material.DARK_PRISMARINE, "§bPrestígio II [Yeti]"));
        warpsMap.put(40, new WarpInfo("krampus", "group.krampus", Material.BEACON, "§4Prestígio III [Krampus]"));

        // PvP (Sempre desbloqueada)
        warpsMap.put(42, new WarpInfo("minapvp", "", Material.DIAMOND_SWORD, "§c⚒ Mina PvP [Congelada]"));
    }

    private void setupItems() {
        // Neve no topo para decoração
        for (int i = 0; i < 9; i++) {
            inventory.setItem(i, createItem(Material.WHITE_STAINED_GLASS_PANE, " "));
        }

        // Renderiza cada mina dinamicamente verificando as permissões
        for (Map.Entry<Integer, WarpInfo> entry : warpsMap.entrySet()) {
            int slot = entry.getKey();
            WarpInfo info = entry.getValue();

            // A Mina PvP não tem permissão exigida
            if (info.perm.isEmpty() || player.hasPermission(info.perm) || player.hasPermission("tw.admin")) {
                inventory.setItem(slot, createItem(info.mat, info.name, "", "§aClica para viajar!"));
            } else {
                inventory.setItem(slot, createItem(Material.GRAY_DYE, info.name, "", "§cMina Bloqueada!", "§7Evolui para aceder."));
            }
        }

        inventory.setItem(49, createItem(Material.OAK_DOOR, "§cVoltar", "§7Retornar à Central."));
        TWMenuFrame.apply(inventory);
    }

    @Override
    public void onClick(InventoryClickEvent event) {
        int slot = event.getSlot();

        if (slot == 49) {
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1f, 1f);
            new MinaMainMenu(player).open();
            return;
        }

        // Se clicou numa mina bloqueada (Corante Cinza)
        if (event.getCurrentItem() != null && event.getCurrentItem().getType() == Material.GRAY_DYE) {
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
            player.sendMessage(ChatColor.RED + "Não tens Rank/VIP suficiente para entrar nesta mina!");
            return;
        }

        WarpInfo info = warpsMap.get(slot);
        if (info != null) {
            player.closeInventory();
            player.playSound(player.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 1f, 1f);
            player.sendMessage(ChatColor.GREEN + "⚒ A viajar para a mina " + info.name + "...");
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "warp " + info.warpName + " " + player.getName());
        }
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }

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

    // Estrutura de dados para guardar a configuração de cada botão
    private static class WarpInfo {
        String warpName;
        String perm;
        Material mat;
        String name;

        WarpInfo(String warpName, String perm, Material mat, String name) {
            this.warpName = warpName;
            this.perm = perm;
            this.mat = mat;
            this.name = name;
        }
    }
}