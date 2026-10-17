package br.com.tw.caixas.menus;

import br.com.tw.caixas.data.CrateAccount;
import br.com.tw.caixas.managers.CrateOpenings;
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

public final class CrateBulkMenu implements TWMenu {
    private final CrateService service;
    private final CrateOpenings openings;
    private final CrateAccount account;
    private final CrateType type;
    private final Inventory inventory;

    public CrateBulkMenu(CrateService service, CrateOpenings openings, Player player,
                         CrateAccount account, CrateType type) {
        this.service = service;
        this.openings = openings;
        this.account = account;
        this.type = type;
        inventory = Bukkit.createInventory(this, 27, "§8Abertura em Massa");
        inventory.setItem(11, choice(10, Material.IRON_BLOCK, "§aAbrir 10x"));
        inventory.setItem(13, choice(50, Material.GOLD_BLOCK, "§6Abrir 50x"));
        long available = account.getKeys(type);
        int amount = (int) Math.min(service.getCatalog().getBulkLimit(), available);
        inventory.setItem(15, choice(amount, Material.DIAMOND_BLOCK, "§bAbrir Todas (" + amount + "x)"));
        inventory.setItem(22, item(Material.BARRIER, "§cVoltar",
                Arrays.asList("§7Cada caixa sorteia uma recompensa.", "§7O pity avança à metade da velocidade.")));
        TWMenuFrame.apply(inventory);
    }

    @Override
    public Inventory getInventory() { return inventory; }

    @Override
    public void onClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        Player player = (Player) event.getWhoClicked();
        int slot = event.getRawSlot();
        int amount;
        if (slot == 11) amount = 10;
        else if (slot == 13) amount = 50;
        else if (slot == 15) amount = (int) Math.min(service.getCatalog().getBulkLimit(), account.getKeys(type));
        else if (slot == 22) {
            player.closeInventory();
            return;
        } else return;
        if (amount < 1 || account.getKeys(type) < amount) {
            player.sendMessage("§cNão tens chaves suficientes para essa opção.");
            return;
        }
        player.closeInventory();
        openings.openBulk(player, type, amount);
    }

    private ItemStack choice(int amount, Material material, String title) {
        List<String> lore = Arrays.asList(
                "§7Disponíveis: §e" + account.getKeys(type),
                amount < 1 ? "§cSem chaves disponíveis." : "§eClica para abrir.",
                "§7Uma recompensa aleatória por caixa.",
                "§cPity à metade da velocidade.");
        return item(material, title, lore);
    }

    private ItemStack item(Material material, String title, List<String> lore) {
        ItemStack stack = new ItemStack(material);
        ItemMeta meta = stack.getItemMeta();
        meta.setDisplayName(title);
        meta.setLore(lore);
        stack.setItemMeta(meta);
        return stack;
    }
}
