package br.com.tw.farms.menus;

import br.com.tw.core.TWCorePlugin;
import br.com.tw.core.menus.TWMenu;
import br.com.tw.core.utils.TWMenuFrame;
import br.com.tw.farms.TWFarmsPlugin;
import br.com.tw.farms.managers.FarmManager;
import br.com.tw.spawners.TWSpawnersPlugin;
import br.com.tw.spawners.models.PlacedSpawner;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public final class FarmAlchemyMenu implements TWMenu {
    private final Inventory inventory;
    private final Player player;
    private final FarmManager farmManager;
    private final Map<Integer, PlacedSpawner> spawnerSlots = new HashMap<>();
    private final int[] slots = {10, 11, 12, 13, 14, 15, 16, 19, 20, 21, 22, 23, 24, 25};

    public FarmAlchemyMenu(Player player, FarmManager farmManager) {
        this.player = player;
        this.farmManager = farmManager;
        inventory = Bukkit.createInventory(this, 45, ChatColor.DARK_PURPLE + "Alquimia de Fertilizantes");
        setup();
    }

    private void setup() {
        for (int slot = 0; slot < inventory.getSize(); slot++) {
            ItemStack pane = TWMenuFrame.stainedGlassPane(10);
            inventory.setItem(slot, pane);
        }
        spawnerSlots.clear();
        int index = 0;
        for (PlacedSpawner spawner : TWSpawnersPlugin.getInstance().getSpawnerManager()
                .getPlacedSpawners().values()) {
            if (!spawner.getOwnerUuid().equals(player.getUniqueId()) || index >= slots.length) continue;
            int slot = slots[index++];
            spawnerSlots.put(slot, spawner);
            ItemStack item = new ItemStack(Material.SPAWNER);
            ItemMeta meta = item.getItemMeta();
            meta.setDisplayName("§dAlquimia no Spawner " + spawner.getTypeId());
            meta.setLore(Arrays.asList("§7XP disponível: §b" + spawner.getStoredXp(),
                    "§7Almas disponíveis: §5" + spawner.getStoredSouls(),
                    "", "§eClique esquerdo: §7Adubo enriquecido (XP + esmeraldas)",
                    "§6Clique direito: §7Solo mágico (almas + diamantes)"));
            item.setItemMeta(meta);
            inventory.setItem(slot, item);
        }
        inventory.setItem(31, make(Material.BOOK, "§eReceitas",
                "§7XP de spawner + esmeraldas produzem",
                "§7Adubo Enriquecido.",
                "§7Almas de spawner + diamantes produzem",
                "§7Solo Mágico."));
    }

    private ItemStack make(Material material, String title, String... lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(title);
        meta.setLore(Arrays.asList(lore));
        item.setItemMeta(meta);
        return item;
    }

    @Override
    public void onClick(InventoryClickEvent event) {
        PlacedSpawner spawner = spawnerSlots.get(event.getSlot());
        if (spawner == null) return;
        if (!spawner.getOwnerUuid().equals(player.getUniqueId()) && !player.hasPermission("tw.admin")) {
            player.sendMessage(ChatColor.RED + "Só podes usar recursos dos teus spawners.");
            return;
        }
        craft(spawner, event.isRightClick() ? "souls" : "xp");
        setup();
    }

    private void craft(PlacedSpawner spawner, String recipeId) {
        if (TWSpawnersPlugin.getInstance().getSpawnerManager()
                .getPlacedSpawners().get(spawner.getLocationKey()) != spawner) {
            player.sendMessage(ChatColor.RED + "Este spawner já não está disponível. Reabre a alquimia.");
            return;
        }
        String path = "alchemy." + recipeId + ".";
        long resourceCost = TWFarmsPlugin.getInstance().getConfig().getLong(
                path + ("souls".equals(recipeId) ? "souls" : "xp"), 0L);
        Material ingredient = Material.matchMaterial(
                TWFarmsPlugin.getInstance().getConfig().getString(path + "material", ""));
        int ingredientAmount = TWFarmsPlugin.getInstance().getConfig().getInt(path + "material-amount", 0);
        long units = TWFarmsPlugin.getInstance().getConfig().getLong(path + "fertilizer-units", 0L);
        int soilLevel = TWFarmsPlugin.getInstance().getConfig().getInt(path + "soil-level", 0);
        if (resourceCost < 1 || ingredient == null || ingredientAmount < 1 || units < 1
                || soilLevel < 1 || soilLevel > 3) {
            throw new IllegalStateException("Receita de alquimia inválida em " + path + ".");
        }
        long available = "souls".equals(recipeId) ? spawner.getStoredSouls() : spawner.getStoredXp();
        if (available < resourceCost) {
            player.sendMessage(ChatColor.RED + "O spawner não tem " + resourceCost
                    + ("souls".equals(recipeId) ? " almas." : " XP."));
            return;
        }
        if (player.getInventory().firstEmpty() == -1
                || !player.getInventory().contains(ingredient, ingredientAmount)) {
            player.sendMessage(ChatColor.RED + (player.getInventory().firstEmpty() == -1
                    ? "Liberta um espaço no inventário." : "Faltam " + ingredientAmount + "x " + ingredient + "."));
            return;
        }

        ItemStack fertilizer = farmManager.createFertilizerItem(units, soilLevel);
        player.getInventory().removeItem(new ItemStack(ingredient, ingredientAmount));
        if ("souls".equals(recipeId)) spawner.setStoredSouls(spawner.getStoredSouls() - resourceCost);
        else spawner.setStoredXp(spawner.getStoredXp() - resourceCost);
        TWSpawnersPlugin.getInstance().getSpawnerManager().saveSpawnerAsync(spawner);
        player.getInventory().setItem(player.getInventory().firstEmpty(), fertilizer);
        player.sendMessage(ChatColor.GREEN + "Alquimia concluída: recebeste " + units
                + " unidades de fertilizante.");
    }

    public void open() {
        if (TWCorePlugin.getInstance().getUserManager().getUser(player.getUniqueId()) == null) {
            player.sendMessage(ChatColor.RED + "Os teus dados ainda estão a ser carregados.");
            return;
        }
        player.openInventory(inventory);
    }

    @Override
    public Inventory getInventory() { return inventory; }
}
