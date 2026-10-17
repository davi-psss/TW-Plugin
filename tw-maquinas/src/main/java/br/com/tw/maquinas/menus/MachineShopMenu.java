package br.com.tw.maquinas.menus;

import br.com.tw.core.TWCorePlugin;
import br.com.tw.core.data.TWUser;
import br.com.tw.core.menus.TWConfirmationMenu;
import br.com.tw.core.menus.TWMenu;
import br.com.tw.core.utils.TWFormat;
import br.com.tw.core.utils.TWMenuFrame;
import br.com.tw.maquinas.TWMaquinasPlugin;
import br.com.tw.maquinas.managers.MachineManager;
import br.com.tw.maquinas.models.MachineType;
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

public class MachineShopMenu implements TWMenu {

    private final Inventory inventory;
    private final Player player;
    private final TWUser user;
    private final MachineManager manager;
    private final Map<Integer, MachineType> slotMap = new HashMap<>();

    public MachineShopMenu(Player player) {
        this.player = player;
        this.user = TWCorePlugin.getInstance().getUserManager().getUser(player.getUniqueId());
        this.manager = TWMaquinasPlugin.getInstance().getMachineManager();
        this.inventory = Bukkit.createInventory(this, 54, ChatColor.DARK_GRAY + "Loja de Máquinas");
        setupItems();
    }

    private void setupItems() {
        for (int i = 0; i < 54; i++) {
            inventory.setItem(i, createGlass(15));
        }

        int[] slots = {10, 11, 12, 13, 14, 15, 16, 19, 20, 21};
        int idx = 0;

        for (MachineType type : manager.getMachineTypes().values()) {
            if (idx >= slots.length) break;
            int slot = slots[idx++];
            slotMap.put(slot, type);

            boolean unlocked = user != null && manager.hasRequiredRank(user.getRankId(), user.getPrestigeId(), type.getRequiredRank());
            ItemStack item = new ItemStack(type.getMaterial());
            ItemMeta meta = item.getItemMeta();
            meta.setDisplayName(type.getDisplayName());
            meta.setLore(Arrays.asList(
                    "§7Rank Necessário: §f" + type.getRequiredRank(),
                    "§7Preço Base (1x): §a$" + TWFormat.formatarDinheiro(type.getBuyCost()),
                    "§7Geração: §b$" + TWFormat.formatarDinheiro(type.getValuePerCycle()) + " a cada " + type.getCycleSeconds() + "s",
                    "",
                    unlocked ? "§eBotão Esquerdo: §7Comprar 1x" : "§c§lRANK INSUFICIENTE",
                    unlocked ? "§6Botão Direito: §7Comprar 10x" : "",
                    unlocked ? "§bShift + Clique: §7Comprar MAX" : ""
            ));
            item.setItemMeta(meta);
            inventory.setItem(slot, item);
        }

        double baseFuelPrice = TWMaquinasPlugin.getInstance().getConfig().getDouble("combustivel.preco-money", 5000.0);
        inventory.setItem(38, createFuelButton("§eCombustível Pequeno", 100, baseFuelPrice));
        inventory.setItem(40, createFuelButton("§6Combustível Médio", 1000, baseFuelPrice * 10));
        inventory.setItem(42, createFuelButton("§cCombustível Grande", 10000, baseFuelPrice * 100));

        ItemStack info = new ItemStack(Material.BOOK);
        ItemMeta iMeta = info.getItemMeta();
        iMeta.setDisplayName("§a§lDica de Abastecimento");
        iMeta.setLore(Arrays.asList("§7Segura §eSHIFT §7e clica com o combustível", "§7na máquina para colocar tudo de uma vez!"));
        info.setItemMeta(iMeta);
        inventory.setItem(49, info);
        TWMenuFrame.apply(inventory);
    }

    private ItemStack createFuelButton(String name, int units, double price) {
        ItemStack item = new ItemStack(Material.COAL, 1, (short) 1);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(name);
        meta.setLore(Arrays.asList(
                "§7Unidades: §b" + units + " ciclos",
                "§7Preço: §a$" + TWFormat.formatarDinheiro(price),
                "",
                "§eClica para adquirir!"
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

        double baseFuelPrice = TWMaquinasPlugin.getInstance().getConfig().getDouble("combustivel.preco-money", 5000.0);
        if (slot == 38) buyFuel(100, baseFuelPrice);
        if (slot == 40) buyFuel(1000, baseFuelPrice * 10);
        if (slot == 42) buyFuel(10000, baseFuelPrice * 100);

        MachineType type = slotMap.get(slot);
        if (type != null) {
            if (!manager.hasRequiredRank(user.getRankId(), user.getPrestigeId(), type.getRequiredRank())) {
                player.playSound(player.getLocation(), Sound.VILLAGER_NO, 1f, 1f);
                player.sendMessage(ChatColor.RED + "Precisas do rank " + type.getRequiredRank() + " para comprar esta máquina!");
                return;
            }

            long amount = 1;
            if (event.isShiftClick()) {
                amount = (long) Math.floor(user.getBalance() / type.getBuyCost());
                if (amount > 64) amount = 64;
                if (amount <= 0) amount = 1;
            } else if (event.isRightClick()) {
                amount = 10;
            }

            if (player.getInventory().firstEmpty() == -1) {
                player.playSound(player.getLocation(), Sound.VILLAGER_NO, 1f, 1f);
                player.sendMessage(ChatColor.RED + "O teu inventário está cheio!");
                return;
            }

            final long purchaseAmount = amount;
            double totalCost = type.getBuyCost() * purchaseAmount;
            confirmIfExpensive(totalCost, "Comprar " + purchaseAmount + "x " + type.getDisplayName(),
                    () -> purchaseMachine(type, purchaseAmount, totalCost));
        }
    }

    private void buyFuel(int units, double price) {
        if (player.getInventory().firstEmpty() == -1) {
            player.playSound(player.getLocation(), Sound.VILLAGER_NO, 1f, 1f);
            player.sendMessage(ChatColor.RED + "O teu inventário está cheio!");
            return;
        }
        confirmIfExpensive(price, "Comprar " + units + " unidades de combustível",
                () -> purchaseFuel(units, price));
    }

    private void purchaseMachine(MachineType type, long amount, double totalCost) {
        if (user.removeBalance(totalCost)) {
            TWCorePlugin.getInstance().getUserManager().saveUserAsync(user);
            giveItem(manager.createMachineItem(type, amount));
            player.playSound(player.getLocation(), Sound.LEVEL_UP, 1f, 1.5f);
            player.sendMessage(ChatColor.GREEN + "Compraste " + amount + "x " + type.getDisplayName()
                    + ChatColor.GREEN + " por $" + TWFormat.formatarDinheiro(totalCost) + "!");
        } else {
            player.playSound(player.getLocation(), Sound.VILLAGER_NO, 1f, 1f);
            player.sendMessage(ChatColor.RED + "Faltam $" + TWFormat.formatarDinheiro(totalCost - user.getBalance())
                    + " para concluir a compra.");
        }
    }

    private void purchaseFuel(int units, double price) {
        if (user.removeBalance(price)) {
            TWCorePlugin.getInstance().getUserManager().saveUserAsync(user);
            giveItem(manager.createFuelItem(units));
            player.playSound(player.getLocation(), Sound.ORB_PICKUP, 1f, 1.2f);
            player.sendMessage(ChatColor.GREEN + "Compraste " + units + "x ciclos de Combustível!");
        } else {
            player.playSound(player.getLocation(), Sound.VILLAGER_NO, 1f, 1f);
            player.sendMessage(ChatColor.RED + "Não tens saldo suficiente!");
        }
    }

    private void confirmIfExpensive(double price, String description, Runnable purchase) {
        double threshold = TWMaquinasPlugin.getInstance().getConfig().getDouble("confirmation.price-threshold", 250000.0);
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

    private void giveItem(ItemStack item) {
        java.util.Map<Integer, ItemStack> leftovers = player.getInventory().addItem(item);
        for (ItemStack leftover : leftovers.values()) {
            player.getWorld().dropItemNaturally(player.getLocation(), leftover);
        }
    }

    @Override
    public Inventory getInventory() { return inventory; }

    public void open() {
        player.openInventory(inventory);
        player.playSound(player.getLocation(), Sound.CHEST_OPEN, 0.5f, 1f);
    }
}