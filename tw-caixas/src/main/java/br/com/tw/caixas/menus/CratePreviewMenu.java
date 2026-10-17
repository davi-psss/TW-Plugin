package br.com.tw.caixas.menus;

import br.com.tw.caixas.config.CrateCatalog;
import br.com.tw.caixas.model.CrateDefinition;
import br.com.tw.caixas.model.CrateReward;
import br.com.tw.caixas.model.CrateType;
import br.com.tw.core.menus.TWMenu;
import br.com.tw.core.utils.TWMenuFrame;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

public final class CratePreviewMenu implements TWMenu {
    private final Inventory inventory;

    public CratePreviewMenu(CrateCatalog catalog, CrateType type) {
        CrateDefinition definition = catalog.get(type);
        inventory = Bukkit.createInventory(this, 27, "§8Prémios: " + definition.getDisplayName());
        double total = definition.getRewards().stream().mapToDouble(CrateReward::getWeight).sum();
        int slot = 10;
        for (CrateReward reward : definition.getRewards()) {
            double percentage = (reward.getWeight() / total) * 100.0;
            ItemStack item = new ItemStack(reward.getKind() == br.com.tw.caixas.model.RewardKind.ITEM
                    ? reward.getMaterial() : Material.PAPER);
            ItemMeta meta = item.getItemMeta();
            meta.setDisplayName("§f" + reward.getRarity().name());
            List<String> lore = new ArrayList<>();
            lore.add("§7Chance: §e" + String.format(java.util.Locale.US, "%.2f%%", percentage));
            if (reward.getDisplayName() != null) lore.add("§7Item: " + reward.getDisplayName());
            if (reward.getEffect() != null) lore.add("§7Efeito: §f" + reward.getEffect());
            lore.add("§7Quantidade: §e" + reward.getAmountRangeText());
            meta.setLore(lore);
            item.setItemMeta(meta);
            inventory.setItem(slot++, item);
        }
        TWMenuFrame.apply(inventory);
    }

    @Override
    public Inventory getInventory() { return inventory; }

    @Override
    public void onClick(InventoryClickEvent event) {
    }
}
