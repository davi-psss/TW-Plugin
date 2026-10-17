package br.com.tw.caixas.model;

public final class RewardGrant {
    private final CrateReward reward;
    private final long amount;
    private final boolean critical;

    public RewardGrant(CrateReward reward, long amount, boolean critical) {
        if (reward == null || amount < 1) throw new IllegalArgumentException("Prêmio inválido.");
        this.reward = reward;
        this.amount = amount;
        this.critical = critical;
    }

    public CrateReward getReward() { return reward; }
    public long getAmount() { return amount; }
    public boolean isCritical() { return critical; }
}
