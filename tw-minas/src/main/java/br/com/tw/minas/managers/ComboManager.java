package br.com.tw.minas.managers;

import br.com.tw.minas.TWMinasPlugin;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class ComboManager {

    private final Map<UUID, Integer> combos = new ConcurrentHashMap<>();
    private final Map<UUID, Long> expireTimes = new ConcurrentHashMap<>();

    public ComboManager() {
        new BukkitRunnable() {
            @Override
            public void run() {
                long now = System.currentTimeMillis();
                for (Map.Entry<UUID, Long> entry : expireTimes.entrySet()) {
                    UUID uuid = entry.getKey();
                    if (now > entry.getValue()) {
                        Player p = Bukkit.getPlayer(uuid);
                        if (p != null && p.isOnline()) {
                            p.playSound(p.getLocation(), Sound.GLASS, 1f, 0.5f);
                            p.sendMessage("§c§lCOMBO PERDIDO! §7Demoraste muito tempo para quebrar outro bloco.");
                        }
                        combos.remove(uuid);
                        expireTimes.remove(uuid);
                    }
                }
            }
        }.runTaskTimer(TWMinasPlugin.getInstance(), 20L, 20L);
    }

    public int addCombo(Player player, int bateriaLvl) {
        UUID uuid = player.getUniqueId();
        int current = combos.getOrDefault(uuid, 0) + 1;
        combos.put(uuid, current);

        long timeToLive = 3000L + (bateriaLvl * 500L);
        expireTimes.put(uuid, System.currentTimeMillis() + timeToLive);
        return current;
    }

    public void clearPlayer(UUID uuid) {
        combos.remove(uuid);
        expireTimes.remove(uuid);
    }
}