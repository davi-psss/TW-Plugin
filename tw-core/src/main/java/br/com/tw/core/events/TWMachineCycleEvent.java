package br.com.tw.core.events;

import org.bukkit.Location;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

import java.util.UUID;

public final class TWMachineCycleEvent extends Event {
    private static final HandlerList HANDLERS = new HandlerList();
    private final UUID ownerId;
    private final Location machineLocation;
    private double generated;
    private boolean bypassStorageLimit;

    public TWMachineCycleEvent(UUID ownerId, Location machineLocation, double generated) {
        this.ownerId = ownerId;
        this.machineLocation = machineLocation.clone();
        setGenerated(generated);
    }

    public UUID getOwnerId() { return ownerId; }
    public Location getMachineLocation() { return machineLocation.clone(); }
    public double getGenerated() { return generated; }
    public boolean isBypassStorageLimit() { return bypassStorageLimit; }

    public void setBypassStorageLimit(boolean bypassStorageLimit) {
        this.bypassStorageLimit = bypassStorageLimit;
    }

    public void setGenerated(double generated) {
        if (!Double.isFinite(generated) || generated < 0.0) {
            throw new IllegalArgumentException("Produção de máquina inválida.");
        }
        this.generated = generated;
    }

    @Override public HandlerList getHandlers() { return HANDLERS; }
    public static HandlerList getHandlerList() { return HANDLERS; }
}
