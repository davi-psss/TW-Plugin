package br.com.tw.spawners.menus;

import br.com.tw.core.menus.TWMenu;
import br.com.tw.core.utils.TWFormat;
import br.com.tw.spawners.TWSpawnersPlugin;
import br.com.tw.spawners.managers.SpawnerManager;
import br.com.tw.spawners.models.PlacedSpawner;
import br.com.tw.spawners.models.SpawnerType;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Arrays;

public class SpawnerPanelMenu implements TWMenu {

    private final Inventory inventory;
    private final Player player;
    private final PlacedSpawner spawner;
    private final SpawnerType type;
    private final SpawnerManager manager;

    public SpawnerPanelMenu(Player player, PlacedSpawner spawner) {
        this.player = player;
        this.spawner = spawner;
        this.manager = TWSpawnersPlugin.getInstance().getSpawnerManager();
        this.type = manager.getSpawnerTypes().get(spawner.getTypeId());
        this.inventory = Bukkit.createInventory(this, 27, ChatColor.DARK_GRAY + "Painel do Gerador");
        setupItems();
    }

    private void setupItems() {
        inventory.clear();
        if (type == null) return;

        // Slot 11: Retirar 1x Unidade do Stack
        ItemStack removeOne = new ItemStack(Material.SHEARS);
        ItemMeta rMeta = removeOne.getItemMeta();
        rMeta.setDisplayName(ChatColor.YELLOW + "" + ChatColor.BOLD + "Retirar 1x Unidade");
        rMeta.setLore(Arrays.asList(
                ChatColor.GRAY + "Remove apenas 1 gerador deste",
                ChatColor.GRAY + "agrupamento para o teu inventário.",
                "",
                ChatColor.YELLOW + "Clica para retirar 1x!"
        ));
        removeOne.setItemMeta(rMeta);
        inventory.setItem(11, removeOne);

        // Slot 13: Informações do Gerador
        ItemStack info = new ItemStack(Material.MOB_SPAWNER);
        ItemMeta iMeta = info.getItemMeta();
        iMeta.setDisplayName(type.getDisplayName());
        iMeta.setLore(Arrays.asList(
                ChatColor.GRAY + "Dono: " + ChatColor.WHITE + spawner.getOwnerName(),
                ChatColor.GRAY + "Entidade: " + ChatColor.AQUA + type.getEntityType().name(),
                ChatColor.GRAY + "Stack Acumulado: " + ChatColor.YELLOW + spawner.getStackAmount() + "x",
                ChatColor.GRAY + "Valor Base por Abate: " + ChatColor.GREEN + "$" + TWFormat.formatarDinheiro(type.getValuePerKill()),
                ChatColor.GRAY + "Chance de Token: " + ChatColor.AQUA + (int) (type.getTokenChance() * 100) + "%"
        ));
        info.setItemMeta(iMeta);
        inventory.setItem(13, info);

        // Slot 15: Recolher Todo o Stack
        ItemStack pickupAll = new ItemStack(Material.HOPPER);
        ItemMeta pMeta = pickupAll.getItemMeta();
        pMeta.setDisplayName(ChatColor.RED + "" + ChatColor.BOLD + "Recolher Tudo (" + spawner.getStackAmount() + "x)");
        pMeta.setLore(Arrays.asList(
                ChatColor.GRAY + "Remove o bloco do terreno e guarda",
                ChatColor.GRAY + "todo o stack no teu inventário.",
                "",
                ChatColor.RED + "Clica para recolher tudo!"
        ));
        pickupAll.setItemMeta(pMeta);
        inventory.setItem(15, pickupAll);
    }

    @Override
    public void onClick(InventoryClickEvent event) {
        if (!spawner.getOwnerUuid().equals(player.getUniqueId()) && !player.hasPermission("tw.admin")) {
            player.playSound(player.getLocation(), Sound.VILLAGER_NO, 1f, 1f);
            player.sendMessage(ChatColor.RED + "Apenas o proprietário pode gerir este gerador!");
            return;
        }

        if (type == null) return;
        Location loc = spawner.getBukkitLocation();
        if (loc == null || manager.getSpawnerAt(loc) == null) {
            player.closeInventory();
            return;
        }

        int slot = event.getSlot();

        if (slot == 11) { // Retirar 1x
            if (player.getInventory().firstEmpty() == -1) {
                player.playSound(player.getLocation(), Sound.VILLAGER_NO, 1f, 1f);
                player.sendMessage(ChatColor.RED + "O teu inventário está cheio!");
                return;
            }

            if (spawner.getStackAmount() > 1) {
                spawner.addStack(-1);
                manager.saveSpawnerAsync(spawner);
                TWSpawnersPlugin.getInstance().getHologramManager().updateOrCreateHologram(spawner, type);

                player.getInventory().addItem(manager.createSpawnerItem(type, 1));
                player.playSound(player.getLocation(), Sound.ITEM_PICKUP, 1f, 1.2f);
                player.sendMessage(ChatColor.GREEN + "Retiraste 1x " + type.getDisplayName() + ChatColor.GREEN + " do stack!");
                setupItems();
            } else {
                // Se só restar 1x, recolhe o bloco inteiro
                recolherBlocoTotal(loc);
            }
        } else if (slot == 15) { // Recolher Tudo
            if (player.getInventory().firstEmpty() == -1) {
                player.playSound(player.getLocation(), Sound.VILLAGER_NO, 1f, 1f);
                player.sendMessage(ChatColor.RED + "O teu inventário está cheio!");
                return;
            }
            recolherBlocoTotal(loc);
        }
    }

    private void recolherBlocoTotal(Location loc) {
        player.closeInventory();
        player.getInventory().addItem(manager.createSpawnerItem(type, spawner.getStackAmount()));

        TWSpawnersPlugin.getInstance().getHologramManager().removeHologram(spawner.getLocationKey());
        loc.getBlock().setType(Material.AIR);
        manager.removeSpawner(loc);

        player.playSound(player.getLocation(), Sound.ITEM_PICKUP, 1f, 1f);
        player.sendMessage(ChatColor.YELLOW + "Recolheste " + spawner.getStackAmount() + "x " + type.getDisplayName() + ChatColor.YELLOW + " com sucesso!");
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }

    public void open() {
        player.openInventory(inventory);
        player.playSound(player.getLocation(), Sound.CLICK, 1f, 1f);
    }
}