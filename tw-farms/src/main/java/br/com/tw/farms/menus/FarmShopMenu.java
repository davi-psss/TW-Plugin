package br.com.tw.farms.menus;

import br.com.tw.core.TWCorePlugin;
import br.com.tw.core.data.TWUser;
import br.com.tw.core.menus.TWConfirmationMenu;
import br.com.tw.core.menus.TWMenu;
import br.com.tw.core.utils.TWFormat;
import br.com.tw.core.utils.TWMenuFrame;
import br.com.tw.farms.TWFarmsPlugin;
import br.com.tw.farms.managers.FarmManager;
import br.com.tw.farms.models.FarmType;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public final class FarmShopMenu implements TWMenu {
    private final Inventory inventory;
    private final Player player;
    private final FarmManager manager;
    private final Map<Integer, FarmType> farmSlots = new HashMap<>();
    private final Map<Integer, String> boosterSlots = new HashMap<>();
    private final int[] cropSlots = {10, 11, 12, 13, 14, 15, 16, 19, 20, 21, 22, 23, 24, 25, 28, 29};

    public FarmShopMenu(Player player, FarmManager manager) {
        this.player = player;
        this.manager = manager;
        this.inventory = Bukkit.createInventory(this, 54, ChatColor.DARK_GREEN + "Loja de Farms");
        setup();
    }

    private void setup() {
        for (int slot = 0; slot < inventory.getSize(); slot++) inventory.setItem(slot, glass(slot));
        int index = 0;
        for (FarmType type : manager.getFarmTypes().values()) {
            if (index >= cropSlots.length) break;
            int slot = cropSlots[index++];
            farmSlots.put(slot, type);
            ItemStack item = new ItemStack(type.getPlantingMaterial());
            ItemMeta meta = item.getItemMeta();
            meta.setDisplayName(plantingTitle(type));
            meta.setLore(Arrays.asList("§7Rank necessário: §f" + type.getRequiredRank(),
                    "§7Preço por unidade: §a$" + TWFormat.formatarDinheiro(type.getBuyCost()),
                    "§7Planta no terreno e deixa crescer no tempo do Minecraft.",
                    "§7Valor base: §e$" + TWFormat.formatarDinheiro(type.getSellValue()) + " / unidade",
                    "", "§eClique esquerdo: §7Comprar 1", "§6Clique direito: §7Comprar 10",
                    "§bShift + clique: §7Comprar até 64"));
            item.setItemMeta(meta);
            inventory.setItem(slot, item);
        }

        ConfigurationSection boosters = TWFarmsPlugin.getInstance().getConfig()
                .getConfigurationSection("shop.growth-boosters");
        int[] boosterPositions = {37, 38, 39, 41};
        int idx = 0;
        if (boosters != null) {
            for (String key : boosters.getKeys(false)) {
                if (idx >= boosterPositions.length) break;
                int slot = boosterPositions[idx++];
                boosterSlots.put(slot, key);
                inventory.setItem(slot, configItem(boosters.getConfigurationSection(key),
                        Material.SUGAR, "§bBooster de Crescimento " + key,
                        "§7Acelera todas as tuas plantações deste tipo."));
            }
        }
        inventory.setItem(42, item(Material.DIAMOND_HOE, "§a§lEnxada do Fazendeiro",
                "§7Compra uma enxada especial para", "§7colher plantações físicas próximas.",
                "§7Preço: §a$" + TWFormat.formatarDinheiro(TWFarmsPlugin.getInstance()
                        .getConfig().getDouble("shop.harvester-hoe.price", 250000.0))));
        inventory.setItem(44, item(Material.CHEST, "§a§lArmazém da Farm",
                "§7Guarda os produtos colhidos",
                "§7automaticamente no terreno.",
                "§eUsa /farm para abrir."));
        inventory.setItem(49, item(Material.BOOK, "§e§lComo funciona",
                "§7Constrói a tua plantação com blocos",
                "§7e mecanismos normais do Minecraft.",
                "§7Os produtos vão para o armazém /farm."));
    }

    private ItemStack configItem(ConfigurationSection section, Material material, String title, String hint) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(title);
        if (section == null) {
            meta.setLore(Arrays.asList(hint, "§cConfiguração inválida"));
        } else {
            meta.setLore(Arrays.asList(hint, "§7Velocidade: §ex" + section.getDouble("multiplier", 1.0),
                    "§7Duração: §f" + section.getLong("duration-seconds", 0) / 60 + " minutos",
                    "§7Preço: §a$" + TWFormat.formatarDinheiro(section.getDouble("price", 0.0)),
                    "§eClique para comprar."));
        }
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack glass(int slot) {
        int row = slot / 9;
        int column = slot % 9;
        boolean edge = row == 0 || row == 5 || column == 0 || column == 8;
        ItemStack pane = TWMenuFrame.stainedGlassPane(edge ? (row == 0 || row == 5 ? 13 : 5) : 15);
        return pane;
    }

    private ItemStack item(Material material, String title, String... lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(title);
        meta.setLore(Arrays.asList(lore));
        item.setItemMeta(meta);
        return item;
    }

    private String plantingTitle(FarmType type) {
        String title = ChatColor.translateAlternateColorCodes('&', type.getDisplayName());
        String plainTitle = ChatColor.stripColor(title);
        if (plainTitle.regionMatches(true, 0, "Farm de ", 0, "Farm de ".length())) {
            return "§aPlantação de " + plainTitle.substring("Farm de ".length());
        }
        return title;
    }

    @Override
    public void onClick(InventoryClickEvent event) {
        int slot = event.getRawSlot();
        if (slot < 0 || slot >= inventory.getSize()) return;
        TWUser user = TWCorePlugin.getInstance().getUserManager().getUser(player.getUniqueId());
        if (user == null) {
            player.sendMessage(ChatColor.RED + "Os teus dados ainda estão a ser carregados.");
            return;
        }
        FarmType type = farmSlots.get(slot);
        if (type != null) {
            if (!manager.hasRequiredRank(user.getRankId(), user.getPrestigeId(), type.getRequiredRank())) {
                player.sendMessage(ChatColor.RED + "Precisas do rank " + type.getRequiredRank() + ".");
                return;
            }
            long amount = event.isShiftClick() ? 64 : event.isRightClick() ? 10 : 1;
            if (event.isShiftClick()) {
                amount = (long) Math.min(64, Math.floor(user.getBalance() / type.getBuyCost()));
                if (amount < 1) {
                    player.sendMessage(ChatColor.RED + "Não tens saldo suficiente.");
                    return;
                }
            }
            double cost = type.getBuyCost() * amount;
            final long purchaseAmount = amount;
            confirmIfNeeded(cost, "Comprar " + amount + "x " + ChatColor.stripColor(plantingTitle(type)),
                    () -> purchase(user, cost, manager.createPlantingItems(type, purchaseAmount)));
            return;
        }

        String boosterKey = boosterSlots.get(slot);
        if (boosterKey != null) {
            ConfigurationSection section = TWFarmsPlugin.getInstance().getConfig()
                    .getConfigurationSection("shop.growth-boosters." + boosterKey);
            if (section != null) {
                double price = section.getDouble("price", Double.NaN);
                confirmIfNeeded(price, "Comprar booster de crescimento", () -> purchase(user, price,
                        manager.createBoosterItem(section.getDouble("multiplier", 0.0),
                                section.getLong("duration-seconds", 0))));
            }
            return;
        }

        if (slot == 42) {
            double price = TWFarmsPlugin.getInstance().getConfig().getDouble("shop.harvester-hoe.price", Double.NaN);
            confirmIfNeeded(price, "Comprar Enxada do Fazendeiro",
                    () -> purchase(user, price, manager.createHarvesterHoe()));
        } else if (slot == 44) {
            new FarmStorageMenu(player, manager).open();
        }
    }

    private void confirmIfNeeded(double cost, String description, Runnable purchase) {
        if (!Double.isFinite(cost) || cost < 0.0) throw new IllegalStateException("Preço inválido na loja de farms.");
        double threshold = TWFarmsPlugin.getInstance().getConfig()
                .getDouble("shop.confirmation-price-threshold", 250000.0);
        if (!Double.isFinite(threshold) || threshold < 0.0) throw new IllegalStateException("Limite de confirmação inválido.");
        if (cost >= threshold) new TWConfirmationMenu(player, description + " por $"
                + TWFormat.formatarDinheiro(cost) + "?", purchase).open();
        else purchase.run();
    }

    private void purchase(TWUser user, double cost, ItemStack item) {
        if (player.getInventory().firstEmpty() == -1) {
            player.sendMessage(ChatColor.RED + "O teu inventário está cheio.");
            return;
        }
        if (!user.removeBalance(cost)) {
            player.sendMessage(ChatColor.RED + "Saldo insuficiente: faltam $"
                    + TWFormat.formatarDinheiro(Math.max(0.0, cost - user.getBalance())) + ".");
            return;
        }
        TWCorePlugin.getInstance().getUserManager().saveUserAsync(user);
        player.getInventory().addItem(item);
        player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 1.4f);
        player.sendMessage(ChatColor.GREEN + "Compra concluída por $" + TWFormat.formatarDinheiro(cost) + ".");
    }

    public void open() {
        player.openInventory(inventory);
        player.playSound(player.getLocation(), Sound.BLOCK_CHEST_OPEN, 0.5f, 1f);
    }

    @Override
    public Inventory getInventory() { return inventory; }
}
