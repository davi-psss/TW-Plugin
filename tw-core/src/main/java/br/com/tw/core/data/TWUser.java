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
    private int pickaxeLevel;
    private double pickaxeXp;
    private final Map<String, Integer> pickaxeSkills;
    private String missoesData;
    private transient boolean dirty = false;
    private transient long version = 0L;

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

    public synchronized boolean isDirty() { return dirty; }
    public synchronized void setDirty(boolean dirty) {
        if (dirty) markChanged();
        else this.dirty = false;
    }

    public synchronized UUID getUuid() { return uuid; }
    public synchronized String getName() { return name; }
    public synchronized void setName(String name) { this.name = name; markChanged(); }

    public synchronized long getTokens() { return tokens; }
    public synchronized void setTokens(long tokens) {
        if (tokens < 0) throw new IllegalArgumentException("Tokens não podem ser negativos.");
        this.tokens = tokens;
        markChanged();
    }
    public synchronized void addTokens(long amount) {
        requireNonNegative(amount, "tokens");
        this.tokens = Math.addExact(this.tokens, amount);
        markChanged();
    }
    public synchronized boolean removeTokens(long amount) {
        if (amount < 0) throw new IllegalArgumentException("A quantidade de tokens não pode ser negativa.");
        if (this.tokens < amount) return false;
        this.tokens -= amount;
        markChanged();
        return true;
    }

    public synchronized String getRankId() { return rankId; }
    public synchronized void setRankId(String rankId) { this.rankId = rankId; markChanged(); }
    public synchronized String getPrestigeId() { return prestigeId; }
    public synchronized void setPrestigeId(String prestigeId) { this.prestigeId = prestigeId; markChanged(); }

    public synchronized long getBlocosQuebrados() { return blocosQuebrados; }
    public synchronized void setBlocosQuebrados(long blocosQuebrados) {
        requireNonNegative(blocosQuebrados, "blocos quebrados");
        this.blocosQuebrados = blocosQuebrados;
        markChanged();
    }
    public synchronized void addBlocosQuebrados(long quantidade) {
        requireNonNegative(quantidade, "blocos quebrados");
        this.blocosQuebrados = Math.addExact(this.blocosQuebrados, quantidade);
        markChanged();
    }

    public synchronized double getDinheiroMina() { return dinheiroMina; }
    public synchronized void setDinheiroMina(double dinheiroMina) {
        requireNonNegative(dinheiroMina, "dinheiro da mina");
        this.dinheiroMina = dinheiroMina;
        markChanged();
    }
    public synchronized void addDinheiroMina(double quantidade) {
        requireNonNegative(quantidade, "dinheiro da mina");
        this.dinheiroMina = requireFinite(this.dinheiroMina + quantidade, "dinheiro da mina");
        markChanged();
    }

    public synchronized double getBalance() { return balance; }
    public synchronized void setBalance(double balance) {
        this.balance = requireNonNegative(balance, "saldo");
        markChanged();
    }
    public synchronized void addBalance(double amount) {
        requireNonNegative(amount, "saldo");
        this.balance = requireFinite(this.balance + amount, "saldo");
        markChanged();
    }
    public synchronized boolean removeBalance(double amount) {
        requireNonNegative(amount, "saldo");
        if (this.balance < amount) return false;
        this.balance -= amount;
        markChanged();
        return true;
    }

    public boolean transferBalanceTo(TWUser target, double amount) {
        requireNonNegative(amount, "transferência");
        if (target == null || target == this || amount == 0.0) return false;
        synchronized (TWUser.class) {
            synchronized (this) {
                synchronized (target) {
                    if (balance < amount || !Double.isFinite(target.balance + amount)) return false;
                    balance -= amount;
                    target.balance += amount;
                    markChanged();
                    target.markChanged();
                    return true;
                }
            }
        }
    }

    public synchronized int getPickaxeLevel() { return pickaxeLevel; }
    public synchronized void setPickaxeLevel(int pickaxeLevel) {
        if (pickaxeLevel < 1) throw new IllegalArgumentException("O nível da picareta deve ser positivo.");
        this.pickaxeLevel = pickaxeLevel;
        markChanged();
    }
    public synchronized double getPickaxeXp() { return pickaxeXp; }
    public synchronized void setPickaxeXp(double pickaxeXp) {
        this.pickaxeXp = requireNonNegative(pickaxeXp, "experiência da picareta");
        markChanged();
    }

    public synchronized boolean addPickaxeXp(double amount) {
        requireNonNegative(amount, "experiência da picareta");
        boolean leveledUp = false;
        double xpNeeded = this.pickaxeLevel * 1000.0;
        this.pickaxeXp = requireFinite(this.pickaxeXp + amount, "experiência da picareta");
        while (this.pickaxeXp >= xpNeeded) {
            this.pickaxeXp -= xpNeeded;
            this.pickaxeLevel++;
            xpNeeded = this.pickaxeLevel * 1000.0;
            leveledUp = true;
        }
        markChanged();
        return leveledUp;
    }

    public synchronized int getSkillLevel(String skill) {
        return pickaxeSkills.getOrDefault(skill.toLowerCase(), 0);
    }

    public synchronized void upgradeSkill(String skill) {
        String key = skill.toLowerCase();
        pickaxeSkills.put(key, getSkillLevel(key) + 1);
        markChanged();
    }

    public synchronized void loadSkillsFromString(String data) {
        pickaxeSkills.clear();
        if (data == null || data.isEmpty()) return;
        for (String pair : data.split(",")) {
            String[] split = pair.split(":");
            if (split.length == 2) {
                pickaxeSkills.put(split[0], Integer.parseInt(split[1]));
            }
        }
    }

    public synchronized String getSkillsAsString() {
        if (pickaxeSkills.isEmpty()) return "";
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, Integer> entry : pickaxeSkills.entrySet()) {
            sb.append(entry.getKey()).append(":").append(entry.getValue()).append(",");
        }
        return sb.toString();
    }

    public synchronized String getMissoesData() { return missoesData == null ? "" : missoesData; }
    public synchronized void setMissoesData(String missoesData) {
        this.missoesData = missoesData;
        markChanged();
    }

    public synchronized TWUserSnapshot snapshot() {
        return new TWUserSnapshot(uuid, name, tokens, rankId, prestigeId, blocosQuebrados,
                dinheiroMina, balance, pickaxeLevel, pickaxeXp, getSkillsAsString(),
                getMissoesData(), version);
    }

    public synchronized void markSaved(long savedVersion) {
        if (version == savedVersion) dirty = false;
    }

    private void markChanged() {
        dirty = true;
        version++;
    }

    private static void requireNonNegative(long value, String field) {
        if (value < 0) throw new IllegalArgumentException("O valor de " + field + " não pode ser negativo.");
    }

    private static double requireNonNegative(double value, String field) {
        requireFinite(value, field);
        if (value < 0) throw new IllegalArgumentException("O valor de " + field + " não pode ser negativo.");
        return value;
    }

    private static double requireFinite(double value, String field) {
        if (!Double.isFinite(value)) throw new IllegalArgumentException("O valor de " + field + " deve ser finito.");
        return value;
    }
}
