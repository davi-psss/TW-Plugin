package br.com.tw.caixas.data;

import br.com.tw.caixas.model.CrateType;

public final class PhysicalCrate {
    private final long id;
    private final String world;
    private final int x;
    private final int y;
    private final int z;
    private final CrateType type;

    public PhysicalCrate(long id, String world, int x, int y, int z, CrateType type) {
        this.id = id;
        this.world = world;
        this.x = x;
        this.y = y;
        this.z = z;
        this.type = type;
    }

    public long getId() { return id; }
    public String getWorld() { return world; }
    public int getX() { return x; }
    public int getY() { return y; }
    public int getZ() { return z; }
    public CrateType getType() { return type; }
}
