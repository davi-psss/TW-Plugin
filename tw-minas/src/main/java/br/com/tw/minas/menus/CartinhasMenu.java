package br.com.tw.minas.menus;

import br.com.tw.core.TWCorePlugin;
import br.com.tw.core.data.TWUser;
import br.com.tw.core.menus.TWMenu;
import br.com.tw.core.utils.TWFormat;
import br.com.tw.core.utils.TWMenuFrame;
import br.com.tw.minas.TWMinasPlugin;
import br.com.tw.minas.managers.CartinhasManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Arrays;
import java.util.List;

public class CartinhasMenu implements TWMenu {

    private final Inventory inventory;
    private final Player player;
    private final TWUser user;

    public CartinhasMenu(Player player) {
        this.player = player;
        this.user = TWCorePlugin.getInstance().getUserManager().getUser(player.getUniqueId());
        this.inventory = Bukkit.createInventory(this, 36, ChatColor.LIGHT_PURPLE + "Cartinhas de Missões");
        setupItems();
    }

    private void setupItems() {
        inventory.clear();
        for (int i = 0; i < 9; i++) {
            inventory.setItem(i, createGlass(2));
        }

        if (user != null) {
            List<CartinhasManager.MissionSlot> missions = TWMinasPlugin.getInstance().getCartinhasManager().getMissions(user);
            int[] slots = {11, 13, 15};
            String[] cardNames = {
                    "§aCartinha do Duende §7(Comum)",
                    "§bCartinha do Boneco de Neve §9(Rara)",
                    "§eCartinha do Papai Noel §6(Lendária)"
            };

            for (int i = 0; i < missions.size() && i < 3; i++) {
                CartinhasManager.MissionSlot m = missions.get(i);
                boolean done = m.isCompleted();

                ItemStack paper = new ItemStack(done ? Material.MAP : Material.PAPER);
                ItemMeta meta = paper.getItemMeta();
                meta.setDisplayName(cardNames[i]);

                meta.setLore(Arrays.asList(
                        "§7Missão: §f" + m.type.getTitle(),
                        "§7Objetivo: §e" + m.type.getDescription(m.target),
                        "",
                        "§7Progresso: " + (done ? "§a§lCONCLUÍDA!" : "§f" + m.progress + " §7/ §f" + m.target + " " + buildBar(m.progress, m.target)),
                        "",
                        "§7Recompensas:",
                        " §b• +" + TWFormat.formatarDinheiro(m.rewardTokens) + " Tokens",
                        " §2• +$" + TWFormat.formatarDinheiro(m.rewardMoney),
                        "",
                        done ? "§a§lCLICA PARA RESGATAR RECOMPENSA!" : "§eBotão Direito: §7Sortear outra (50 Tokens)"
                ));

                if (done) {
                    meta.addEnchant(Enchantment.UNBREAKING, 1, true);
                    meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
                }

                paper.setItemMeta(meta);
                inventory.setItem(slots[i], paper);
            }
        }

        ItemStack back = new ItemStack(Material.OAK_DOOR);
        ItemMeta bMeta = back.getItemMeta();
        bMeta.setDisplayName("§cVoltar");
        bMeta.setLore(Arrays.asList("§7Retornar à Central."));
        back.setItemMeta(bMeta);
        inventory.setItem(31, back);
        TWMenuFrame.apply(inventory);
    }

    private String buildBar(long current, long max) {
        int bars = (int) ((double) current / Math.max(1, max) * 10);
        StringBuilder sb = new StringBuilder("§8[§a");
        for (int i = 0; i < 10; i++) {
            if (i == bars) sb.append("§7");
            sb.append("■");
        }
        return sb.append("§8]").toString();
    }

    @Override
    public void onClick(InventoryClickEvent event) {
        if (user == null) return;
        int slot = event.getSlot();

        if (slot == 31) {
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1f, 1f);
            new MinaMainMenu(player).open();
            return;
        }

        int missionIdx = (slot == 11) ? 0 : (slot == 13) ? 1 : (slot == 15) ? 2 : -1;
        if (missionIdx != -1) {
            boolean updated = TWMinasPlugin.getInstance().getCartinhasManager()
                    .claimOrReroll(player, user, missionIdx, event.isRightClick());
            if (updated) {
                setupItems();
            }
        }
    }

    @Override
    public Inventory getInventory() { return inventory; }

    public void open() { player.openInventory(inventory); }

    private ItemStack createGlass(int data) {
        return TWMenuFrame.stainedGlassPane(data);
    }
}