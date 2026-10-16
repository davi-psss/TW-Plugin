package br.com.tw.core.data;

import br.com.tw.core.TWCorePlugin;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class UserManager implements Listener {

    private final Map<UUID, TWUser> usersCache = new ConcurrentHashMap<>();
    private final UserRepository repository;
    private final TWCorePlugin plugin;

    public UserManager(TWCorePlugin plugin, UserRepository repository) {
        this.plugin = plugin;
        this.repository = repository;
        startAutoSaveTask();
    }

    private void startAutoSaveTask() {
        // Salva a cada 2 minutos (2400 ticks) de forma assíncrona apenas os jogadores com alterações (Dirty Checking)
        Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, () -> {
            List<TWUser> snapshot = new ArrayList<>(usersCache.values());
            for (TWUser user : snapshot) {
                if (user.isDirty()) {
                    repository.saveUser(user);
                }
            }
        }, 2400L, 2400L);
    }

    public TWUser getUser(UUID uuid) {
        return usersCache.get(uuid);
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();
        String name = player.getName();

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            TWUser user = repository.loadUser(uuid, name);
            Bukkit.getScheduler().runTask(plugin, () -> {
                usersCache.put(uuid, user);
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "lp user " + name + " parent set rank_" + user.getRankId().toLowerCase());
            });
        });
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        UUID uuid = event.getPlayer().getUniqueId();
        TWUser user = usersCache.remove(uuid);
        if (user != null) {
            Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> repository.saveUser(user));
        }
    }

    public void saveAllSync() {
        for (TWUser user : usersCache.values()) {
            repository.saveUser(user);
        }
        usersCache.clear();
    }

    public void saveUserAsync(TWUser user) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> repository.saveUser(user));
    }

    public Map<String, Double> getTopBalances(int limit) {
        return repository.getTopBalances(limit);
    }

    public boolean updateOfflineBalance(String playerName, double amount, String action) {
        return repository.updateOfflineBalance(playerName, amount, action);
    }

    public Map<String, Long> getTopMiners(int limit) {
        return repository.getTopMiners(limit);
    }

    public UserRepository getRepository() {
        return this.repository;
    }
}