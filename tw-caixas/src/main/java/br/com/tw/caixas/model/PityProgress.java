package br.com.tw.caixas.model;

public final class PityProgress {
    public static final int UNITS_PER_MANUAL_OPEN = 1000;

    private PityProgress() {
    }

    public static Step advance(int progress, int threshold, int units) {
        if (progress < 0 || threshold < 1 || units < 1) {
            throw new IllegalArgumentException("Parâmetros de pity inválidos.");
        }
        int target = Math.multiplyExact(threshold, UNITS_PER_MANUAL_OPEN);
        int updated = Math.addExact(progress, units);
        return updated >= target ? new Step(0, true) : new Step(updated, false);
    }

    public static int openingsUntilGuaranteed(int progress, int threshold) {
        int remaining = Math.max(0, Math.multiplyExact(threshold, UNITS_PER_MANUAL_OPEN) - progress);
        return (remaining + UNITS_PER_MANUAL_OPEN - 1) / UNITS_PER_MANUAL_OPEN;
    }

    public static final class Step {
        private final int progress;
        private final boolean guaranteed;

        private Step(int progress, boolean guaranteed) {
            this.progress = progress;
            this.guaranteed = guaranteed;
        }

        public int getProgress() { return progress; }
        public boolean isGuaranteed() { return guaranteed; }
    }
}
