package br.com.tw.farms.menus;

import br.com.tw.core.TWCorePlugin;
import br.com.tw.core.data.TWUser;
import br.com.tw.core.menus.TWMenu;
import br.com.tw.core.utils.TWMenuFrame;
import br.com.tw.farms.TWFarmsPlugin;
import br.com.tw.farms.managers.FarmManager;
import br.com.tw.farms.models.FarmType;
import br.com.tw.farms.models.PlacedFarm;
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

public final class FarmPanelMenu implements TWMenu {
    private final Inventory inventory;
    private final Player player;
    private final PlacedFarm farm;
    private final FarmType type;
    private final FarmManager manager;

    public FarmPanelMenu(Player player, PlacedFarm farm) {
        this.player = player;
        this.farm = farm;
        this.manager = TWFarmsPlugin.getInstance().getFarmManager();
        this.type = manager.getFarmTypes().get(farm.getTypeId());
        inventory = Bukkit.createInventory(this, 45, ChatColor.DARK_GREEN + "Painel da Farm");
        setup();
    }

    private void setup() {
        for (int slot = 0; slot < inventory.getSize(); slot++) {
            ItemStack pane = TWMenuFrame.stainedGlassPane(15);
            inventory.setItem(slot, pane);
        }
        TWMenuFrame.apply(inventory);
        if (type == null) return;
        long capacity = manager.getStorageCapacity(farm);
        inventory.setItem(13, make(type.getMaterial(), "§eFarm compacta (legado)",
                "§7Dono: §f" + farm.getOwnerName(), "§7Stack: §e" + farm.getStackAmount() + "x",
                "§cEsta farm deixou de produzir.", "§7Constrói plantações físicas no terreno."));

        inventory.setItem(11, make(Material.CHEST, "§a§lSilo / Armazém",
                "§7Colheitas: §e" + farm.getStoredTotal() + " §8/ §f" + capacity,
                "§7Normais: §f" + farm.getStored(), "§6Douradas: §e" + farm.getStoredGolden(),
                "", "§eClique esquerdo: §7Vender toda a safra",
                "§bClique direito: §7Recolher para inventário"));
        long autoSellCost = TWFarmsPlugin.getInstance().getConfig()
                .getLong("upgrades.auto-sell-token-cost", 5000L);
        inventory.setItem(15, make(farm.isAutoSell() ? Material.EMERALD_BLOCK : Material.REDSTONE_BLOCK,
                "§b§lAuto-Sell: " + (farm.isAutoSell() ? "§aLigado"
                        : farm.isAutoSellUnlocked() ? "§cDesligado" : "§cBloqueado"),
                "§7Descarrega o silo a cada 10 segundos", "§7enquanto estiveres online.",
                farm.isAutoSellUnlocked() ? "" : "§7Desbloqueio: §b" + autoSellCost + " Tokens",
                "§eClica para alternar."));

        inventory.setItem(31, make(Material.WHEAT, "§a§lConstrói a tua plantação",
                "§7Planta culturas e usa os mecanismos",
                "§7normais do Minecraft para produzir."));
        inventory.setItem(40 + 4, make(Material.HOPPER, "§c§lDesmontar Farm",
                "§7Preserva stack, adubo, safra", "§7e upgrades no item devolvido.",
                "", "§cClica para desmontar."));
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
        int slot = event.getRawSlot();
        if (slot < 0 || slot >= inventory.getSize()) return;
        if (!farm.getOwnerUuid().equals(player.getUniqueId()) && !player.hasPermission("tw.admin")) {
            player.sendMessage(ChatColor.RED + "Apenas o proprietário pode gerir esta farm.");
            return;
        }
        TWUser user = TWCorePlugin.getInstance().getUserManager().getUser(player.getUniqueId());
        if (user == null) {
            player.sendMessage(ChatColor.RED + "Os teus dados ainda estão a ser carregados.");
            return;
        }
        if (slot == 11) {
            if (event.isRightClick()) {
                manager.withdrawFarmStorage(player, farm);
            } else {
                manager.sellFarmStorage(player, farm);
            }
            setup();
        } else if (slot == 15) {
            if (farm.isAutoSell()) {
                farm.setAutoSell(false);
            } else {
                if (!farm.isAutoSellUnlocked()) {
                    long cost = TWFarmsPlugin.getInstance().getConfig()
                            .getLong("upgrades.auto-sell-token-cost", 5000L);
                    if (!user.removeTokens(cost)) {
                        player.sendMessage(ChatColor.RED + "Precisas de " + cost + " Tokens para desbloquear o Auto-Sell.");
                        return;
                    }
                    farm.setAutoSellUnlocked(true);
                    TWCorePlugin.getInstance().getUserManager().saveUserAsync(user);
                }
                farm.setAutoSell(true);
            }
            manager.saveFarmAsync(farm);
            setup();
        } else if (slot == 44) {
            dismantle();
        }
    }

    private void dismantle() {
        int empty = player.getInventory().firstEmpty();
        if (empty == -1) {
            player.sendMessage(ChatColor.RED + "Liberta um espaço no inventário antes de desmontar.");
            return;
        }
        ItemStack item = manager.createFarmItem(farm);
        player.getInventory().setItem(empty, item);
        org.bukkit.Location location = farm.getBukkitLocation();
        if (location != null) location.getBlock().setType(Material.AIR);
        manager.removeFarm(farm.getBukkitLocation());
        TWFarmsPlugin.getInstance().getHologramManager().remove(farm.getLocationKey());
        player.closeInventory();
        player.playSound(player.getLocation(), Sound.ENTITY_ITEM_PICKUP, 1f, 1f);
        player.sendMessage(ChatColor.GREEN + "Farm desmontada; itens, safra e upgrades foram preservados.");
    }

    public void open() {
        player.openInventory(inventory);
        player.playSound(player.getLocation(), Sound.BLOCK_CHEST_OPEN, 0.5f, 1f);
    }

    @Override
    public Inventory getInventory() { return inventory; }
}
