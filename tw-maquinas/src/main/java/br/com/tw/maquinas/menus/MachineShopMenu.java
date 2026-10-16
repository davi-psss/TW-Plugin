package br.com.tw.maquinas.menus;

import br.com.tw.core.TWCorePlugin;
import br.com.tw.core.data.TWUser;
import br.com.tw.core.menus.TWMenu;
import br.com.tw.core.utils.TWFormat;
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
        this.inventory = Bukkit.createInventory(this, 45, ChatColor.DARK_GRAY + "Loja de Máquinas");
        setupItems();
    }

    private void setupItems() {
        int[] slots = {10, 11, 12, 13, 14, 15, 16};
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
                    ChatColor.GRAY + "Rank Necessário: " + ChatColor.WHITE + type.getRequiredRank(),
                    ChatColor.GRAY + "Preço (1x): " + ChatColor.GREEN + "$" + TWFormat.formatarDinheiro(type.getBuyCost()),
                    ChatColor.GRAY + "Geração: " + ChatColor.AQUA + "$" + TWFormat.formatarDinheiro(type.getValuePerCycle()) + " a cada " + type.getCycleSeconds() + "s",
                    "",
                    unlocked ? ChatColor.YELLOW + "Botão Esquerdo: Comprar 1x" : ChatColor.RED + "Rank insuficiente!",
                    unlocked ? ChatColor.GOLD + "Botão Direito: Comprar 10x" : ""
            ));
            item.setItemMeta(meta);
            inventory.setItem(slot, item);
        }

        // Botão de Comprar Combustível
        double fuelCost = TWMaquinasPlugin.getInstance().getConfig().getDouble("combustivel.preco-money", 5000.0);
        int fuelUnits = TWMaquinasPlugin.getInstance().getConfig().getInt("combustivel.unidades-por-compra", 100);
        ItemStack fuelBtn = new ItemStack(Material.COAL, 1, (short) 1);
        ItemMeta fMeta = fuelBtn.getItemMeta();
        fMeta.setDisplayName(ChatColor.GOLD + "" + ChatColor.BOLD + "Comprar Combustível");
        fMeta.setLore(Arrays.asList(
                ChatColor.GRAY + "Unidades: " + ChatColor.AQUA + fuelUnits + " cargas",
                ChatColor.GRAY + "Preço: " + ChatColor.GREEN + "$" + TWFormat.formatarDinheiro(fuelCost),
                "",
                ChatColor.YELLOW + "Clica para adquirir!"
        ));
        fuelBtn.setItemMeta(fMeta);
        inventory.setItem(31, fuelBtn);
    }

    @Override
    public void onClick(InventoryClickEvent event) {
        if (user == null) return;
        int slot = event.getSlot();

        if (slot == 31) {
            double fuelCost = TWMaquinasPlugin.getInstance().getConfig().getDouble("combustivel.preco-money", 5000.0);
            int fuelUnits = TWMaquinasPlugin.getInstance().getConfig().getInt("combustivel.unidades-por-compra", 100);
            if (user.removeBalance(fuelCost)) {
                TWCorePlugin.getInstance().getUserManager().saveUserAsync(user);
                player.getInventory().addItem(manager.createFuelItem(fuelUnits));
                player.playSound(player.getLocation(), Sound.ORB_PICKUP, 1f, 1.2f);
                player.sendMessage(ChatColor.GREEN + "Compraste " + fuelUnits + "x unidades de Combustível!");
            } else {
                player.playSound(player.getLocation(), Sound.VILLAGER_NO, 1f, 1f);
                player.sendMessage(ChatColor.RED + "Não tens saldo suficiente!");
            }
            return;
        }

        MachineType type = slotMap.get(slot);
        if (type != null) {
            if (!manager.hasRequiredRank(user.getRankId(), user.getPrestigeId(), type.getRequiredRank())) {
                player.playSound(player.getLocation(), Sound.VILLAGER_NO, 1f, 1f);
                player.sendMessage(ChatColor.RED + "Precisas do rank " + type.getRequiredRank() + " para comprar esta máquina!");
                return;
            }

            long amount = event.isRightClick() ? 10 : 1;
            double totalCost = type.getBuyCost() * amount;

            if (user.removeBalance(totalCost)) {
                TWCorePlugin.getInstance().getUserManager().saveUserAsync(user);
                player.getInventory().addItem(manager.createMachineItem(type, amount));
                player.playSound(player.getLocation(), Sound.LEVEL_UP, 1f, 1.5f);
                player.sendMessage(ChatColor.GREEN + "Compraste " + amount + "x " + type.getDisplayName() + ChatColor.GREEN + " por $" + TWFormat.formatarDinheiro(totalCost) + "!");
            } else {
                player.playSound(player.getLocation(), Sound.VILLAGER_NO, 1f, 1f);
                player.sendMessage(ChatColor.RED + "Faltam $" + TWFormat.formatarDinheiro(totalCost - user.getBalance()) + " para esta compra.");
            }
        }
    }

    @Override
    public Inventory getInventory() { return inventory; }

    public void open() {
        player.openInventory(inventory);
        player.playSound(player.getLocation(), Sound.CHEST_OPEN, 0.5f, 1f);
    }
}