package br.com.tw.core.data;

import java.util.UUID;

public final class TWUserSnapshot {

    private final UUID uuid;
    private final String name;
    private final long tokens;
    private final String rankId;
    private final String prestigeId;
    private final long blocksBroken;
    private final double mineMoney;
    private final double balance;
    private final int pickaxeLevel;
    private final double pickaxeXp;
    private final String pickaxeSkills;
    private final String missionsData;
    private final long version;

    TWUserSnapshot(UUID uuid, String name, long tokens, String rankId, String prestigeId,
                   long blocksBroken, double mineMoney, double balance, int pickaxeLevel,
                   double pickaxeXp, String pickaxeSkills, String missionsData, long version) {
        this.uuid = uuid;
        this.name = name;
        this.tokens = tokens;
        this.rankId = rankId;
        this.prestigeId = prestigeId;
        this.blocksBroken = blocksBroken;
        this.mineMoney = mineMoney;
        this.balance = balance;
        this.pickaxeLevel = pickaxeLevel;
        this.pickaxeXp = pickaxeXp;
        this.pickaxeSkills = pickaxeSkills;
        this.missionsData = missionsData;
        this.version = version;
    }

    public UUID getUuid() { return uuid; }
    public String getName() { return name; }
    public long getTokens() { return tokens; }
    public String getRankId() { return rankId; }
    public String getPrestigeId() { return prestigeId; }
    public long getBlocksBroken() { return blocksBroken; }
    public double getMineMoney() { return mineMoney; }
    public double getBalance() { return balance; }
    public int getPickaxeLevel() { return pickaxeLevel; }
    public double getPickaxeXp() { return pickaxeXp; }
    public String getPickaxeSkills() { return pickaxeSkills; }
    public String getMissionsData() { return missionsData; }
    public long getVersion() { return version; }
}
