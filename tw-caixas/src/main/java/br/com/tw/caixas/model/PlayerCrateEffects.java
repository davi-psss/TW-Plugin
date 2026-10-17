package br.com.tw.caixas.model;

import java.util.EnumMap;
import java.util.Map;

public final class PlayerCrateEffects {
    private final EnumMap<CrateEffect, Long> charges = new EnumMap<>(CrateEffect.class);

    public PlayerCrateEffects() {
        for (CrateEffect effect : CrateEffect.values()) charges.put(effect, 0L);
    }

    public synchronized long get(CrateEffect effect) {
        return charges.get(effect);
    }

    public synchronized long add(CrateEffect effect, long amount) {
        if (effect == null || amount < 1L) throw new IllegalArgumentException("Efeito ou cargas inválidos.");
        long updated = Math.addExact(charges.get(effect), amount);
        charges.put(effect, updated);
        return updated;
    }

    public synchronized long consume(CrateEffect effect, long amount) {
        if (effect == null || amount < 1L) throw new IllegalArgumentException("Efeito ou consumo inválidos.");
        long current = charges.get(effect);
        long consumed = Math.min(current, amount);
        charges.put(effect, current - consumed);
        return consumed;
    }

    public synchronized void set(CrateEffect effect, long amount) {
        if (effect == null || amount < 0L) throw new IllegalArgumentException("Efeito ou cargas inválidos.");
        charges.put(effect, amount);
    }

    public synchronized Map<CrateEffect, Long> snapshot() {
        return new EnumMap<>(charges);
    }
}
