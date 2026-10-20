package br.com.tw.core.utils;

import org.bukkit.Material;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public final class TWMenuFrame {

    private static final Material[] STAINED_GLASS_PANES = {
            Material.WHITE_STAINED_GLASS_PANE,
            Material.ORANGE_STAINED_GLASS_PANE,
            Material.MAGENTA_STAINED_GLASS_PANE,
            Material.LIGHT_BLUE_STAINED_GLASS_PANE,
            Material.YELLOW_STAINED_GLASS_PANE,
            Material.LIME_STAINED_GLASS_PANE,
            Material.PINK_STAINED_GLASS_PANE,
            Material.GRAY_STAINED_GLASS_PANE,
            Material.LIGHT_GRAY_STAINED_GLASS_PANE,
            Material.CYAN_STAINED_GLASS_PANE,
            Material.PURPLE_STAINED_GLASS_PANE,
            Material.BLUE_STAINED_GLASS_PANE,
            Material.BROWN_STAINED_GLASS_PANE,
            Material.GREEN_STAINED_GLASS_PANE,
            Material.RED_STAINED_GLASS_PANE,
            Material.BLACK_STAINED_GLASS_PANE
    };

    private TWMenuFrame() {
    }

    public static ItemStack stainedGlassPane(int legacyColor) {
        if (legacyColor < 0 || legacyColor >= STAINED_GLASS_PANES.length) {
            throw new IllegalArgumentException("Cor inválida para vidro: " + legacyColor);
        }
        ItemStack pane = new ItemStack(STAINED_GLASS_PANES[legacyColor]);
        ItemMeta meta = pane.getItemMeta();
        meta.setDisplayName(" ");
        pane.setItemMeta(meta);
        return pane;
    }

    public static void apply(Inventory inventory) {
        int size = inventory.getSize();
        if (size % 9 != 0) {
            throw new IllegalArgumentException("Menus com moldura devem ter linhas completas de 9 slots.");
        }

        int rows = size / 9;
        for (int slot = 0; slot < size; slot++) {
            int row = slot / 9;
            int column = slot % 9;
            boolean edge = row == 0 || row == rows - 1 || column == 0 || column == 8;
            if (!edge) continue;

            ItemStack current = inventory.getItem(slot);
            if (current != null && !current.getType().name().endsWith("_STAINED_GLASS_PANE")) continue;

            boolean corner = (row == 0 || row == rows - 1) && (column == 0 || column == 8);
            inventory.setItem(slot, stainedGlassPane(corner ? 3 : 11));
        }
    }
}
