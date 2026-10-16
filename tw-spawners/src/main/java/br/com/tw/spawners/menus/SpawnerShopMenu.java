package br.com.tw.spawners.menus;

import br.com.tw.core.TWCorePlugin;
import br.com.tw.core.data.TWUser;
import br.com.tw.core.menus.TWMenu;
import br.com.tw.core.utils.TWFormat;
import br.com.tw.spawners.TWSpawnersPlugin;
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
        this.inventory = Bukkit.createInventory(this, 36, ChatColor.DARK_GRAY + "Loja de Spawners");
        setupItems();
    }

    private void setupItems() {
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
                    ChatColor.GRAY + "Rank Necessário: " + ChatColor.WHITE + type.getRequiredRank(),
                    ChatColor.GRAY + "Preço (1x): " + ChatColor.GREEN + "$" + TWFormat.formatarDinheiro(type.getBuyCost()),
                    ChatColor.GRAY + "Recompensa/Abate: " + ChatColor.AQUA + "$" + TWFormat.formatarDinheiro(type.getValuePerKill()),
                    "",
                    unlocked ? ChatColor.YELLOW + "Botão Esquerdo: Comprar 1x" : ChatColor.RED + "Rank insuficiente!",
                    unlocked ? ChatColor.GOLD + "Botão Direito: Comprar 10x" : ""
            ));
            item.setItemMeta(meta);
            inventory.setItem(slot, item);
        }
    }

    @Override
    public void onClick(InventoryClickEvent event) {
        if (user == null) return;
        SpawnerType type = slotMap.get(event.getSlot());
        if (type == null) return;

        if (!manager.hasRequiredRank(user.getRankId(), user.getPrestigeId(), type.getRequiredRank())) {
            player.playSound(player.getLocation(), Sound.VILLAGER_NO, 1f, 1f);
            player.sendMessage(ChatColor.RED + "Precisas do rank " + type.getRequiredRank() + " para comprar este gerador!");
            return;
        }

        long amount = event.isRightClick() ? 10 : 1;
        double totalCost = type.getBuyCost() * amount;

        if (user.removeBalance(totalCost)) {
            TWCorePlugin.getInstance().getUserManager().saveUserAsync(user);
            player.getInventory().addItem(manager.createSpawnerItem(type, amount));
            player.playSound(player.getLocation(), Sound.LEVEL_UP, 1f, 1.5f);
            player.sendMessage(ChatColor.GREEN + "Compraste " + amount + "x " + type.getDisplayName() + ChatColor.GREEN + " por $" + TWFormat.formatarDinheiro(totalCost) + "!");
        } else {
            player.playSound(player.getLocation(), Sound.VILLAGER_NO, 1f, 1f);
            player.sendMessage(ChatColor.RED + "Faltam $" + TWFormat.formatarDinheiro(totalCost - user.getBalance()) + " para esta compra.");
        }
    }

    @Override
    public Inventory getInventory() { return inventory; }

    public void open() {
        player.openInventory(inventory);
        player.playSound(player.getLocation(), Sound.CHEST_OPEN, 0.5f, 1f);
    }
}