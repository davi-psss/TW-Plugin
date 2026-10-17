package br.com.tw.core.events;

import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

import java.util.List;

public final class TWMiningPrepareEvent extends Event {
    private static final HandlerList HANDLERS = new HandlerList();
    private final Player player;
    private final Block originBlock;
    private final List<Block> blocksToBreak;

    public TWMiningPrepareEvent(Player player, List<Block> blocksToBreak) {
        this(player, blocksToBreak == null || blocksToBreak.isEmpty() ? null : blocksToBreak.get(0), blocksToBreak);
    }

    public TWMiningPrepareEvent(Player player, Block originBlock, List<Block> blocksToBreak) {
        this.player = player;
        this.originBlock = originBlock;
        this.blocksToBreak = blocksToBreak;
    }

    public Player getPlayer() { return player; }
    public Block getOriginBlock() { return originBlock; }
    public List<Block> getBlocksToBreak() { return blocksToBreak; }
    @Override public HandlerList getHandlers() { return HANDLERS; }
    public static HandlerList getHandlerList() { return HANDLERS; }
}
