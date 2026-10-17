package br.com.tw.caixas.menus;

import br.com.tw.caixas.TWCaixasPlugin;
import br.com.tw.caixas.data.CrateAccount;
import br.com.tw.caixas.managers.CrateService;
import br.com.tw.caixas.model.CrateType;
import br.com.tw.core.menus.TWMenu;
import br.com.tw.core.utils.TWMenuFrame;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Arrays;
import java.util.List;

public final class KeyForgeMenu implements TWMenu {
    private static final int[] SLOTS = {10, 12, 14, 16};
    private final TWCaixasPlugin plugin;
    private final CrateService service;
    private final CrateAccount account;
    private final Inventory inventory;

    public KeyForgeMenu(TWCaixasPlugin plugin, CrateService service, Player player, CrateAccount account) {
        this.plugin = plugin;
        this.service = service;
        this.account = account;
        inventory = Bukkit.createInventory(this, 27, "§8Forja de Chaves");
        CrateType[] types = CrateType.values();
        for (int i = 0; i < types.length - 1; i++) {
            CrateType source = types[i];
            CrateType target = types[i + 1];
            long cost = i == types.length - 2
                    ? service.getCatalog().getDivineForgeCost() : service.getCatalog().getForgeCost();
            inventory.setItem(SLOTS[i], forgeItem(source, target, cost));
        }
        TWMenuFrame.apply(inventory);
    }

    @Override
    public Inventory getInventory() { return inventory; }

    @Override
    public void onClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        int index = -1;
        for (int i = 0; i < SLOTS.length; i++) {
            if (SLOTS[i] == event.getRawSlot()) index = i;
        }
        if (index < 0) return;
        CrateType[] types = CrateType.values();
        CrateType source = types[index];
        CrateType target = types[index + 1];
        long cost = index == types.length - 2
                ? service.getCatalog().getDivineForgeCost() : service.getCatalog().getForgeCost();
        Player player = (Player) event.getWhoClicked();
        if (account.getKeys(source) < cost) {
            player.sendMessage("§cPrecisas de " + cost + " chaves " + source.getId() + ".");
            return;
        }
        service.forgeKeys(player.getUniqueId(), source, target, cost, success -> {
            if (success) {
                player.sendMessage("§aFundiste §e" + cost + " §achaves em 1 chave " + target.getId() + ".");
                player.closeInventory();
            } else {
                player.sendMessage("§cNão tens chaves suficientes ou a forja falhou.");
            }
        });
    }

    private ItemStack forgeItem(CrateType source, CrateType target, long cost) {
        ItemStack item = new ItemStack(Material.ANVIL);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName("§e" + source.getId() + " §7→ §d" + target.getId());
        meta.setLore(Arrays.asList("§7Tens: §e" + account.getKeys(source) + "x",
                "§7Custo: §c" + cost + "x " + source.getId(),
                "§aRecebes: §e1x " + target.getId(), "§eClica para fundir."));
        item.setItemMeta(meta);
        return item;
    }
}
