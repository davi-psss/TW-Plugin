package br.com.tw.caixas.menus;

import br.com.tw.caixas.model.CrateReward;
import br.com.tw.caixas.model.RewardKind;
import br.com.tw.core.utils.TWFormat;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

final class CrateMenuItems {
    private CrateMenuItems() {
    }

    static ItemStack rewardIcon(CrateReward reward, long amount, boolean critical) {
        Material material = reward.getKind() == RewardKind.ITEM ? reward.getMaterial() : Material.PAPER;
        ItemStack item = new ItemStack(material == null ? Material.PAPER : material);
        ItemMeta meta = item.getItemMeta();
        String name;
        switch (reward.getKind()) {
            case MONEY:
                name = "§a$" + TWFormat.formatarDinheiro(amount);
                break;
            case TOKENS:
                name = "§b" + amount + " Tokens";
                break;
            case XP:
                name = "§d" + amount + " níveis de XP";
                break;
            case SOULS:
                name = "§c" + amount + " Almas";
                break;
            case ITEM:
                name = reward.getDisplayName() == null ? "§fItem de recompensa" : reward.getDisplayName();
                break;
            default:
                name = "§fRecompensa";
        }
        meta.setDisplayName((critical ? "§6✦ CRÍTICO ✦ " : "") + name);
        List<String> lore = new ArrayList<>();
        lore.add("§7Raridade: " + rarityColor(reward.getRarity()) + reward.getRarity().name());
        if (reward.getKind() == RewardKind.ITEM && reward.getEffect() != null) {
            lore.add("§7Efeito: §f" + reward.getEffect());
        }
        if (critical) lore.add("§6Prêmio duplicado pelo crítico!");
        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    static String describe(CrateReward reward, long amount, boolean critical) {
        ItemStack icon = rewardIcon(reward, amount, critical);
        return icon.hasItemMeta() && icon.getItemMeta().hasDisplayName()
                ? icon.getItemMeta().getDisplayName() : reward.getRarity().name();
    }

    private static String rarityColor(CrateReward.Rarity rarity) {
        switch (rarity) {
            case UNCOMMON: return "§a";
            case RARE: return "§b";
            case EPIC: return "§d";
            case LEGENDARY: return "§6";
            default: return "§f";
        }
    }
}
