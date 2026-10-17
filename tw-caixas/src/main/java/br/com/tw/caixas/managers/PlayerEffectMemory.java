package br.com.tw.caixas.managers;

import br.com.tw.caixas.TWCaixasPlugin;
import br.com.tw.caixas.data.CrateRepository;
import br.com.tw.caixas.model.CrateEffect;
import br.com.tw.caixas.model.PlayerCrateEffects;
import br.com.tw.core.utils.TWAsyncTaskQueue;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class PlayerEffectMemory {
    private final TWCaixasPlugin plugin;
    private final CrateRepository repository;
    private final TWAsyncTaskQueue persistenceQueue;
    private final Map<UUID, PlayerCrateEffects> effects = new HashMap<>();
    private final Set<UUID> loading = new HashSet<>();
    private final Map<UUID, Integer> pendingFlushes = new HashMap<>();

    public PlayerEffectMemory(TWCaixasPlugin plugin, CrateRepository repository) {
        this.plugin = plugin;
        this.repository = repository;
        this.persistenceQueue = new TWAsyncTaskQueue(plugin, "tw-crate-effects-db");
    }

    public void load(UUID playerId) {
        if (effects.containsKey(playerId) || !loading.add(playerId)) return;
        persistenceQueue.submit("Falha ao carregar consumíveis ativos de " + playerId + ".", () -> {
            Map<CrateEffect, Long> loaded;
            try {
                loaded = repository.loadPlayerEffects(playerId);
            } catch (RuntimeException e) {
                plugin.getLogger().log(java.util.logging.Level.SEVERE,
                        "Não foi possível carregar os consumíveis ativos de " + playerId + ".", e);
                if (plugin.isEnabled()) {
                    Bukkit.getScheduler().runTask(plugin, () -> {
                        loading.remove(playerId);
                        Player player = Bukkit.getPlayer(playerId);
                        if (player != null) {
                            player.sendMessage("§cNão foi possível carregar os teus consumíveis; tenta novamente em instantes.");
                        }
                    });
                }
                return;
            }
            if (!plugin.isEnabled()) return;
            Bukkit.getScheduler().runTask(plugin, () -> {
                loading.remove(playerId);
                PlayerCrateEffects state = new PlayerCrateEffects();
                for (Map.Entry<CrateEffect, Long> entry : loaded.entrySet()) {
                    state.set(entry.getKey(), entry.getValue());
                }
                effects.put(playerId, state);
                Player player = Bukkit.getPlayer(playerId);
                if (player != null && player.isOnline()) {
                    if (!loaded.isEmpty()) player.sendMessage("§aOs teus consumíveis ativos foram carregados.");
                }
            });
        });
    }

    public boolean isLoaded(UUID playerId) {
        return effects.containsKey(playerId);
    }

    public long getCharges(UUID playerId, CrateEffect effect) {
        PlayerCrateEffects state = effects.get(playerId);
        return state == null ? 0L : state.get(effect);
    }

    public Map<CrateEffect, Long> getSnapshot(UUID playerId) {
        PlayerCrateEffects state = effects.get(playerId);
        return state == null ? null : state.snapshot();
    }

    public long addCharges(UUID playerId, CrateEffect effect, long amount) {
        PlayerCrateEffects state = requireLoaded(playerId);
        long total = state.add(effect, amount);
        scheduleSave(playerId);
        return total;
    }

    public long consume(UUID playerId, CrateEffect effect, long amount) {
        PlayerCrateEffects state = requireLoaded(playerId);
        long consumed = state.consume(effect, amount);
        if (consumed > 0L) scheduleSave(playerId);
        return consumed;
    }

    public void flush(UUID playerId) {
        Integer taskId = pendingFlushes.remove(playerId);
        if (taskId != null) Bukkit.getScheduler().cancelTask(taskId);
        save(playerId);
    }

    public void close() {
        for (Integer taskId : pendingFlushes.values()) Bukkit.getScheduler().cancelTask(taskId);
        pendingFlushes.clear();
        for (UUID playerId : effects.keySet()) save(playerId);
        persistenceQueue.shutdownAndAwait();
    }

    private PlayerCrateEffects requireLoaded(UUID playerId) {
        PlayerCrateEffects state = effects.get(playerId);
        if (state == null) throw new IllegalStateException("Os consumíveis do jogador ainda estão a carregar.");
        return state;
    }

    private void scheduleSave(UUID playerId) {
        if (pendingFlushes.containsKey(playerId)) return;
        int taskId = Bukkit.getScheduler().runTaskLater(plugin, () -> {
            pendingFlushes.remove(playerId);
            save(playerId);
        }, 40L).getTaskId();
        pendingFlushes.put(playerId, taskId);
    }

    private void save(UUID playerId) {
        PlayerCrateEffects state = effects.get(playerId);
        if (state == null) return;
        Map<CrateEffect, Long> snapshot = state.snapshot();
        persistenceQueue.submit("Falha ao guardar consumíveis ativos de " + playerId + ".",
                () -> repository.savePlayerEffects(playerId, snapshot));
    }
}
