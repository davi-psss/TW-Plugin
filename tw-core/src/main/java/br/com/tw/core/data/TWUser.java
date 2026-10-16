package br.com.tw.core.data;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class TWUser {
    private final UUID uuid;
    private String name;
    private long tokens;
    private String rankId;
    private String prestigeId;
    private long blocosQuebrados;
    private double dinheiroMina;
    private double balance;

    // CAMPOS DA PICARETA
    private int pickaxeLevel;
    private double pickaxeXp;
    private final Map<String, Integer> pickaxeSkills;

    // NOVO: SISTEMA DE CARTINHAS (MISSÕES ALEATÓRIAS) E DIRTY CHECKING
    private String missoesData;
    private transient boolean dirty = false;

    public TWUser(UUID uuid, String name, long tokens, String rankId, String prestigeId) {
        this.uuid = uuid;
        this.name = name;
        this.tokens = tokens;
        this.rankId = rankId;
        this.prestigeId = prestigeId;
        this.balance = 0.0;
        this.pickaxeLevel = 1;
        this.pickaxeXp = 0.0;
        this.pickaxeSkills = new HashMap<>();
        this.missoesData = "";
    }

    public boolean isDirty() { return dirty; }
    public void setDirty(boolean dirty) { this.dirty = dirty; }

    public UUID getUuid() { return uuid; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; this.dirty = true; }

    public long getTokens() { return tokens; }
    public void setTokens(long tokens) { this.tokens = tokens; this.dirty = true; }
    public void addTokens(long amount) { this.tokens += amount; this.dirty = true; }
    public boolean removeTokens(long amount) {
        if (this.tokens >= amount) {
            this.tokens -= amount;
            this.dirty = true;
            return true;
        }
        return false;
    }

    public String getRankId() { return rankId; }
    public void setRankId(String rankId) { this.rankId = rankId; this.dirty = true; }
    public String getPrestigeId() { return prestigeId; }
    public void setPrestigeId(String prestigeId) { this.prestigeId = prestigeId; this.dirty = true; }

    public long getBlocosQuebrados() { return blocosQuebrados; }
    public void setBlocosQuebrados(long blocosQuebrados) { this.blocosQuebrados = blocosQuebrados; }
    public void addBlocosQuebrados(long quantidade) { this.blocosQuebrados += quantidade; this.dirty = true; }

    public double getDinheiroMina() { return dinheiroMina; }
    public void setDinheiroMina(double dinheiroMina) { this.dinheiroMina = dinheiroMina; }
    public void addDinheiroMina(double quantidade) { this.dinheiroMina += quantidade; this.dirty = true; }

    public double getBalance() { return balance; }
    public void setBalance(double balance) { this.balance = balance; this.dirty = true; }
    public void addBalance(double amount) { this.balance += amount; this.dirty = true; }
    public boolean removeBalance(double amount) {
        if (this.balance >= amount) {
            this.balance -= amount;
            this.dirty = true;
            return true;
        }
        return false;
    }

    public int getPickaxeLevel() { return pickaxeLevel; }
    public void setPickaxeLevel(int pickaxeLevel) { this.pickaxeLevel = pickaxeLevel; }
    public double getPickaxeXp() { return pickaxeXp; }
    public void setPickaxeXp(double pickaxeXp) { this.pickaxeXp = pickaxeXp; }

    public boolean addPickaxeXp(double amount) {
        boolean leveledUp = false;
        double xpNeeded = this.pickaxeLevel * 1000.0;
        this.pickaxeXp += amount;
        while (this.pickaxeXp >= xpNeeded) {
            this.pickaxeXp -= xpNeeded;
            this.pickaxeLevel++;
            xpNeeded = this.pickaxeLevel * 1000.0;
            leveledUp = true;
        }
        this.dirty = true;
        return leveledUp;
    }

    public int getSkillLevel(String skill) {
        return pickaxeSkills.getOrDefault(skill.toLowerCase(), 0);
    }

    public void upgradeSkill(String skill) {
        String key = skill.toLowerCase();
        pickaxeSkills.put(key, getSkillLevel(key) + 1);
        this.dirty = true;
    }

    public void loadSkillsFromString(String data) {
        pickaxeSkills.clear();
        if (data == null || data.isEmpty()) return;
        for (String pair : data.split(",")) {
            String[] split = pair.split(":");
            if (split.length == 2) {
                pickaxeSkills.put(split[0], Integer.parseInt(split[1]));
            }
        }
    }

    public String getSkillsAsString() {
        if (pickaxeSkills.isEmpty()) return "";
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, Integer> entry : pickaxeSkills.entrySet()) {
            sb.append(entry.getKey()).append(":").append(entry.getValue()).append(",");
        }
        return sb.toString();
    }

    public String getMissoesData() { return missoesData == null ? "" : missoesData; }
    public void setMissoesData(String missoesData) { this.missoesData = missoesData; this.dirty = true; }
}