package br.com.tw.core.events;

import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public final class TWBlockMinedEvent extends Event {
    private static final HandlerList HANDLERS = new HandlerList();
    private final Player player;
    private final Block block;
    private double payout;

    public TWBlockMinedEvent(Player player, Block block, double payout) {
        this.player = player;
        this.block = block;
        setPayout(payout);
    }

    public Player getPlayer() { return player; }
    public Block getBlock() { return block; }
    public double getPayout() { return payout; }

    public void setPayout(double payout) {
        if (!Double.isFinite(payout) || payout < 0.0) {
            throw new IllegalArgumentException("Payout de mineração inválido.");
        }
        this.payout = payout;
    }

    @Override public HandlerList getHandlers() { return HANDLERS; }
    public static HandlerList getHandlerList() { return HANDLERS; }
}
