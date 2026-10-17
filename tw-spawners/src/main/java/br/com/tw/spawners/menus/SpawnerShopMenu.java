package br.com.tw.spawners.menus;

import br.com.tw.core.TWCorePlugin;
import br.com.tw.core.data.TWUser;
import br.com.tw.core.menus.TWConfirmationMenu;
import br.com.tw.core.menus.TWMenu;
import br.com.tw.core.utils.TWFormat;
import br.com.tw.core.utils.TWMenuFrame;
import br.com.tw.spawners.TWSpawnersPlugin;
import br.com.tw.spawners.listeners.SpawnerListener;
import br.com.tw.spawners.managers.SpawnerManager;
import br.com.tw.spawners.models.SpawnerType;
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

public class SpawnerShopMenu implements TWMenu {

    private final Inventory inventory;
    private final Player player;
    private final TWUser user;
    private final SpawnerManager manager;
    private final Map<Integer, SpawnerType> slotMap = new HashMap<>();

    public SpawnerShopMenu(Player player) {
        this.player = player;
        this.user = TWCorePlugin.getInstance().getUserManager().getUser(player.getUniqueId());
        this.manager = TWSpawnersPlugin.getInstance().getSpawnerManager();
        this.inventory = Bukkit.createInventory(this, 54, ChatColor.DARK_GRAY + "Loja de Geradores");
        setupItems();
    }

    private void setupItems() {
        for (int i = 0; i < 54; i++) {
            inventory.setItem(i, createGlass(15));
        }

        int[] slots = {10, 11, 12, 13, 14, 15, 16};
        int idx = 0;

        for (SpawnerType type : manager.getSpawnerTypes().values()) {
            if (idx >= slots.length) break;
            int slot = slots[idx++];
            slotMap.put(slot, type);

            boolean unlocked = user != null && manager.hasRequiredRank(user.getRankId(), user.getPrestigeId(), type.getRequiredRank());
            ItemStack item = new ItemStack(Material.MOB_SPAWNER);
            ItemMeta meta = item.getItemMeta();
            meta.setDisplayName(type.getDisplayName());
            meta.setLore(Arrays.asList(
                    "§7Rank Necessário: §f" + type.getRequiredRank(),
                    "§7Preço Base: §a$" + TWFormat.formatarDinheiro(type.getBuyCost()),
                    "",
                    unlocked ? "§aBotão Esquerdo: §7Comprar 1x" : "§c§lRANK INSUFICIENTE",
                    unlocked ? "§eBotão Direito: §7Digitar Quantidade" : ""
            ));
            item.setItemMeta(meta);
            inventory.setItem(slot, item);
        }

        // Boosters na Loja
        inventory.setItem(38, createBoosterButton("§bBooster Menor §7(1.5x)", 1.5, 30, 250000.0));
        inventory.setItem(40, createBoosterButton("§aBooster Médio §7(2.0x)", 2.0, 60, 1000000.0));
        inventory.setItem(42, createBoosterButton("§dBooster Divino §7(3.0x)", 3.0, 60, 5000000.0));
        TWMenuFrame.apply(inventory);
    }

    private ItemStack createBoosterButton(String name, double mult, int mins, double price) {
        ItemStack item = new ItemStack(Material.NETHER_STAR, 1);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(name);
        meta.setLore(Arrays.asList(
                "§7Acelera um Spawner específico",
                "§7e aumenta os ganhos em §f" + mult + "x§7.",
                "§7Duração: §f" + mins + " Minutos",
                "",
                "§7Preço: §a$" + TWFormat.formatarDinheiro(price),
                "§eClica para comprar!"
        ));
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createGlass(int color) {
        ItemStack glass = new ItemStack(Material.STAINED_GLASS_PANE, 1, (short) color);
        ItemMeta meta = glass.getItemMeta();
        meta.setDisplayName(" ");
        glass.setItemMeta(meta);
        return glass;
    }

    @Override
    public void onClick(InventoryClickEvent event) {
        if (user == null) return;
        int slot = event.getSlot();

        if (slot == 38) confirmPurchase(250000.0, "Comprar booster 1.5x", () -> buyBooster(1.5, 30, 250000.0));
        if (slot == 40) confirmPurchase(1000000.0, "Comprar booster 2x", () -> buyBooster(2.0, 60, 1000000.0));
        if (slot == 42) confirmPurchase(5000000.0, "Comprar booster 3x", () -> buyBooster(3.0, 60, 5000000.0));

        SpawnerType type = slotMap.get(slot);
        if (type != null) {
            if (!manager.hasRequiredRank(user.getRankId(), user.getPrestigeId(), type.getRequiredRank())) {
                player.playSound(player.getLocation(), Sound.VILLAGER_NO, 1f, 1f);
                player.sendMessage(ChatColor.RED + "Precisas do rank " + type.getRequiredRank() + " para comprar isto!");
                return;
            }

            if (event.isRightClick()) {
                player.closeInventory();
                SpawnerListener.pendingPurchases.put(player.getUniqueId(), type);
                player.sendMessage("§eEscreve no chat a quantidade de §f" + type.getDisplayName() + " §eque desejas comprar (ou 'cancelar' para sair).");
                return;
            }

            if (player.getInventory().firstEmpty() == -1) {
                player.playSound(player.getLocation(), Sound.VILLAGER_NO, 1f, 1f);
                player.sendMessage(ChatColor.RED + "O teu inventário está cheio!");
                return;
            }

            double totalCost = type.getBuyCost();
            confirmPurchase(totalCost, "Comprar 1x " + type.getDisplayName(),
                    () -> purchaseSpawner(type, 1, totalCost));
        }
    }

    private void purchaseSpawner(SpawnerType type, long amount, double price) {
        if (user.removeBalance(price)) {
            java.util.Map<Integer, ItemStack> leftovers =
                    player.getInventory().addItem(manager.createSpawnerItem(type, amount));
            if (!leftovers.isEmpty()) {
                user.addBalance(price);
                TWCorePlugin.getInstance().getUserManager().saveUserAsync(user);
                player.sendMessage(ChatColor.RED + "O inventário encheu durante a confirmação; a compra foi cancelada e o saldo devolvido.");
                return;
            }
            TWCorePlugin.getInstance().getUserManager().saveUserAsync(user);
            player.playSound(player.getLocation(), Sound.LEVEL_UP, 1f, 1.5f);
            player.sendMessage(ChatColor.GREEN + "Compraste " + amount + "x " + type.getDisplayName()
                    + ChatColor.GREEN + " por $" + TWFormat.formatarDinheiro(price) + "!");
        } else {
            player.playSound(player.getLocation(), Sound.VILLAGER_NO, 1f, 1f);
            player.sendMessage(ChatColor.RED + "Faltam $" + TWFormat.formatarDinheiro(price - user.getBalance()) + ".");
        }
    }

    private void confirmPurchase(double price, String description, Runnable purchase) {
        double threshold = TWSpawnersPlugin.getInstance().getConfig()
                .getDouble("confirmation.price-threshold", 250000.0);
        if (!Double.isFinite(threshold) || threshold < 0.0) {
            throw new IllegalStateException("Valor inválido em confirmation.price-threshold.");
        }
        if (price >= threshold) {
            new TWConfirmationMenu(player, description + " por $" + TWFormat.formatarDinheiro(price) + "?",
                    purchase).open();
        } else {
            purchase.run();
        }
    }

    private void buyBooster(double mult, int mins, double price) {
        if (player.getInventory().firstEmpty() == -1) {
            player.playSound(player.getLocation(), Sound.VILLAGER_NO, 1f, 1f);
            player.sendMessage(ChatColor.RED + "Inventário cheio!");
            return;
        }
        if (user.removeBalance(price)) {
            TWCorePlugin.getInstance().getUserManager().saveUserAsync(user);
            player.getInventory().addItem(manager.createBoosterItem(mult, mins));
            player.playSound(player.getLocation(), Sound.ORB_PICKUP, 1f, 1.2f);
            player.sendMessage(ChatColor.GREEN + "Booster comprado com sucesso!");
        } else {
            player.playSound(player.getLocation(), Sound.VILLAGER_NO, 1f, 1f);
            player.sendMessage(ChatColor.RED + "Saldo insuficiente!");
        }
    }

    @Override
    public Inventory getInventory() { return inventory; }

    public void open() {
        player.openInventory(inventory);
        player.playSound(player.getLocation(), Sound.CHEST_OPEN, 0.5f, 1f);
    }
}