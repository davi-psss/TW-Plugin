package br.com.tw.core.utils;

import org.bukkit.Material;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public final class TWMenuFrame {

    private static final short FRAME_COLOR = 11;
    private static final short CORNER_COLOR = 3;

    private TWMenuFrame() {
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
            if (current != null && current.getType() != Material.STAINED_GLASS_PANE) continue;

            boolean corner = (row == 0 || row == rows - 1) && (column == 0 || column == 8);
            ItemStack pane = new ItemStack(Material.STAINED_GLASS_PANE, 1,
                    corner ? CORNER_COLOR : FRAME_COLOR);
            ItemMeta meta = pane.getItemMeta();
            meta.setDisplayName(" ");
            pane.setItemMeta(meta);
            inventory.setItem(slot, pane);
        }
    }
}
