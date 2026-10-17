package br.com.tw.caixas.data;

import br.com.tw.caixas.model.CrateType;

import java.util.EnumMap;
import java.util.Map;
import java.util.UUID;

public final class CrateAccount {
    private final UUID playerId;
    private final Map<CrateType, Long> keys = new EnumMap<>(CrateType.class);
    private final Map<CrateType, Integer> pity = new EnumMap<>(CrateType.class);

    public CrateAccount(UUID playerId) {
        this.playerId = playerId;
        for (CrateType type : CrateType.values()) {
            keys.put(type, 0L);
            pity.put(type, 0);
        }
    }

    public UUID getPlayerId() { return playerId; }
    public synchronized long getKeys(CrateType type) { return keys.get(type); }
    public synchronized int getPity(CrateType type) { return pity.get(type); }

    public synchronized void setKeys(CrateType type, long value) {
        if (value < 0) throw new IllegalArgumentException("Chaves não podem ser negativas.");
        keys.put(type, value);
    }

    public synchronized void setPity(CrateType type, int value) {
        if (value < 0) throw new IllegalArgumentException("Pity não pode ser negativo.");
        pity.put(type, value);
    }
}
