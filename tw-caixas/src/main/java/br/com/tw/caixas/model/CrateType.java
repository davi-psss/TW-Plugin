package br.com.tw.caixas.model;

import org.bukkit.Material;

import java.util.Locale;

public enum CrateType {
    MINEIRA("mineira", 10),
    SANGUINARIA("sanguinaria", 11),
    TECNOLOGICA("tecnologica", 12),
    NATUREZA("natureza", 13),
    DIVINA("divina", 14);

    private final String id;
    private final int menuSlot;

    CrateType(String id, int menuSlot) {
        this.id = id;
        this.menuSlot = menuSlot;
    }

    public String getId() {
        return id;
    }

    public int getMenuSlot() {
        return menuSlot;
    }

    public static CrateType fromId(String value) {
        if (value == null) return null;
        String normalized = value.toLowerCase(Locale.ROOT);
        for (CrateType type : values()) {
            if (type.id.equals(normalized)) return type;
        }
        return null;
    }

    public Material getDefaultIcon() {
        switch (this) {
            case MINEIRA: return Material.CHEST;
            case SANGUINARIA: return Material.REDSTONE;
            case TECNOLOGICA: return Material.REDSTONE_BLOCK;
            case NATUREZA: return Material.MELON;
            case DIVINA: return Material.ENCHANTED_BOOK;
            default: throw new IllegalStateException("Tipo de caixa sem ícone.");
        }
    }
}
