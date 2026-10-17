package br.com.tw.core.events;

import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

import java.util.UUID;

public final class TWFarmHarvestEvent extends Event {
    private static final HandlerList HANDLERS = new HandlerList();
    private final UUID ownerId;
    private final String cropType;
    private final long amount;
    private final String locationKey;

    public TWFarmHarvestEvent(UUID ownerId, String cropType, long amount, String locationKey) {
        if (ownerId == null || cropType == null || cropType.trim().isEmpty() || amount < 1
                || locationKey == null || locationKey.trim().isEmpty()) {
            throw new IllegalArgumentException("Colheita inválida.");
        }
        this.ownerId = ownerId;
        this.cropType = cropType;
        this.amount = amount;
        this.locationKey = locationKey;
    }

    public UUID getOwnerId() { return ownerId; }
    public String getCropType() { return cropType; }
    public long getAmount() { return amount; }
    public String getLocationKey() { return locationKey; }

    @Override public HandlerList getHandlers() { return HANDLERS; }
    public static HandlerList getHandlerList() { return HANDLERS; }
}
