package br.com.tw.core.events;

import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public final class TWSpawnerKillEvent extends Event {
    private static final HandlerList HANDLERS = new HandlerList();
    private final Player killer;
    private final LivingEntity entity;
    private final long killedAmount;
    private long bonusSouls;

    public TWSpawnerKillEvent(Player killer, LivingEntity entity, long killedAmount) {
        if (killedAmount < 0) throw new IllegalArgumentException("Quantidade de abates inválida.");
        this.killer = killer;
        this.entity = entity;
        this.killedAmount = killedAmount;
    }

    public Player getKiller() { return killer; }
    public LivingEntity getEntity() { return entity; }
    public long getKilledAmount() { return killedAmount; }
    public long getBonusSouls() { return bonusSouls; }

    public void addBonusSouls(long amount) {
        if (amount < 0) throw new IllegalArgumentException("Almas adicionais não podem ser negativas.");
        bonusSouls = Math.addExact(bonusSouls, amount);
    }

    @Override public HandlerList getHandlers() { return HANDLERS; }
    public static HandlerList getHandlerList() { return HANDLERS; }
}
