package br.com.tw.farms.models;

import java.util.UUID;

public final class PhysicalCrop {
    private final String locationKey;
    private final UUID ownerUuid;
    private final String ownerName;
    private final String typeId;

    public PhysicalCrop(String locationKey, UUID ownerUuid, String ownerName, String typeId) {
        if (locationKey == null || ownerUuid == null || typeId == null) {
            throw new IllegalArgumentException("Localização, dono e tipo da cultura são obrigatórios.");
        }
        this.locationKey = locationKey;
        this.ownerUuid = ownerUuid;
        this.ownerName = ownerName;
        this.typeId = typeId.toUpperCase(java.util.Locale.ROOT);
    }

    public String getLocationKey() { return locationKey; }
    public UUID getOwnerUuid() { return ownerUuid; }
    public String getOwnerName() { return ownerName; }
    public String getTypeId() { return typeId; }
}
