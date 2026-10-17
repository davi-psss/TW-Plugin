package br.com.tw.caixas.model;

public enum CrateEffect {
    MINING_CONTRACT("mining_contract", "Contrato de Mineração"),
    SEISMIC_CHARGE("seismic_charge", "Carga Sísmica"),
    AUTOSELL_DRILL("autosell_drill", "Broca de Auto-Sell"),
    SOUL_MAGNET("soul_magnet", "Íman de Almas");

    private final String id;
    private final String displayName;

    CrateEffect(String id, String displayName) {
        this.id = id;
        this.displayName = displayName;
    }

    public String getId() { return id; }
    public String getDisplayName() { return displayName; }

    public static CrateEffect fromId(String id) {
        if (id == null) return null;
        for (CrateEffect effect : values()) {
            if (effect.id.equalsIgnoreCase(id)) return effect;
        }
        return null;
    }
}
