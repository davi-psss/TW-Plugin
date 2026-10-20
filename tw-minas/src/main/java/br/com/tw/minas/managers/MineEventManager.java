package br.com.tw.minas.managers;

import br.com.tw.core.TWCorePlugin;
import br.com.tw.core.data.TWUser;
import br.com.tw.core.utils.TWFormat;
import br.com.tw.core.utils.TWConfigValidation;
import br.com.tw.minas.TWMinasPlugin;
import br.com.tw.minas.utils.TWBossBar;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitTask;

import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

public class MineEventManager implements Listener {

    private final long communityGoal;
    private final long happyHourDurationMillis;
    private final double happyHourMultiplier;
    private final double giftChance;
    private final long giftLifetimeMillis;
    private final double meteoriteChance;
    private final int meteoriteHealth;
    private long communityProgress = 0L;
    private long happyHourEndTime = 0L;

    private final Map<Location, Long> activeGifts = new ConcurrentHashMap<>();
    private final Map<Location, Integer> activeMeteorites = new ConcurrentHashMap<>();
    private final BukkitTask bossBarTask;
    private final BukkitTask cleanupTask;

    private final Random random = new Random();

    public MineEventManager(TWMinasPlugin plugin) {
        communityGoal = requireLongRange(plugin, "events.community-goal", 1L, 1_000_000_000L);
        happyHourDurationMillis = requireLongRange(plugin, "events.happy-hour-seconds", 1L, 86400L) * 1000L;
        happyHourMultiplier = TWConfigValidation.requireFiniteAtLeast(plugin,
                "events.happy-hour-multiplier", plugin.getConfig().getDouble("events.happy-hour-multiplier", Double.NaN), 1.0);
        giftChance = TWConfigValidation.requireProbability(plugin, "events.gift-chance",
                plugin.getConfig().getDouble("events.gift-chance", Double.NaN));
        giftLifetimeMillis = requireLongRange(plugin, "events.gift-lifetime-seconds", 1L, 3600L) * 1000L;
        meteoriteChance = TWConfigValidation.requireProbability(plugin, "events.meteorite-chance",
                plugin.getConfig().getDouble("events.meteorite-chance", Double.NaN));
        meteoriteHealth = TWConfigValidation.requireIntRange(plugin, "events.meteorite-health",
                plugin.getConfig().getInt("events.meteorite-health", -1), 1, 10000);
        long actionBarTicks = requireLongRange(plugin, "events.actionbar-update-ticks", 1L, 1200L);
        long cleanupTicks = requireLongRange(plugin, "events.cleanup-interval-ticks", 1L, 1200L);
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
        bossBarTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            boolean happyHour = isHappyHourActive();
            long remainingMillis = Math.max(0L, happyHourEndTime - System.currentTimeMillis());
            float progress = happyHour
                    ? Math.min(1.0f, (float) remainingMillis / happyHourDurationMillis)
                    : Math.min(1.0f, (float) communityProgress / communityGoal);
            String barMessage = happyHour
                    ? "§d§lHAPPY HOUR §8» §f" + happyHourMultiplier + "x §8| §e" + formatTime(remainingMillis) + " restante"
                    : "§b§lMETA DA MINA §8» §f" + communityProgress + "§7/§f" + communityGoal
                            + " blocos §8| §e" + (int) (progress * 100) + "%";

            for (Player p : Bukkit.getOnlinePlayers()) {
                if (TWCorePlugin.getInstance().isMineWorld(p.getWorld().getName())) {
                    TWBossBar.sendBossBar(p, barMessage, progress);
                } else {
                    TWBossBar.removeBossBar(p);
                }
            }
        }, actionBarTicks, actionBarTicks);

        cleanupTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            long now = System.currentTimeMillis();
            for (Map.Entry<Location, Long> entry : activeGifts.entrySet()) {
                Location loc = entry.getKey();
                if (!loc.getWorld().isChunkLoaded(loc.getBlockX() >> 4, loc.getBlockZ() >> 4)) continue;
                if (now > entry.getValue()) {
                    activeGifts.remove(loc);
                    if (loc.getBlock().getType() == Material.ENDER_CHEST) {
                        loc.getBlock().setType(Material.AIR);
                    }
                }
            }
        }, cleanupTicks, cleanupTicks);
    }

    @EventHandler
    public void onWorldChange(PlayerChangedWorldEvent event) {
        if (TWCorePlugin.getInstance().isMineWorld(event.getFrom().getName())) {
            TWBossBar.removeBossBar(event.getPlayer());
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        TWBossBar.removeBossBar(event.getPlayer());
    }

    private String formatTime(long millis) {
        long seconds = (millis + 999L) / 1000L;
        return (seconds / 60L) + ":" + String.format("%02d", seconds % 60L);
    }

    public boolean isHappyHourActive() {
        return System.currentTimeMillis() < happyHourEndTime;
    }

    public double getEventMultiplier() {
        return isHappyHourActive() ? happyHourMultiplier : 1.0;
    }

    public long getCommunityProgress() {
        return communityProgress;
    }

    public long getCommunityGoal() {
        return communityGoal;
    }

    public void addCommunityBlocks(Player triggerPlayer, Block brokenBlock, int count) {
        if (count <= 0 || !TWCorePlugin.getInstance().isMineWorld(triggerPlayer.getWorld().getName())) return;
        if (!isHappyHourActive()) {
            communityProgress = Math.min(communityGoal, communityProgress + count);
            if (communityProgress >= communityGoal) {
                communityProgress = 0L;
                happyHourEndTime = System.currentTimeMillis() + happyHourDurationMillis;
                Bukkit.broadcastMessage("");
                Bukkit.broadcastMessage(ChatColor.AQUA + "" + ChatColor.BOLD + "[TW-MINAS] " + ChatColor.YELLOW + "" + ChatColor.BOLD + "HAPPY HOUR NATALINO ATIVADO!");
                Bukkit.broadcastMessage(ChatColor.GREEN + "A meta comunitária de " + communityGoal + " blocos foi atingida!");
                Bukkit.broadcastMessage(ChatColor.GOLD + "Todos os blocos e tokens nas minas valem "
                        + ChatColor.BOLD + happyHourMultiplier + "x" + ChatColor.GOLD + " pelos próximos "
                        + (happyHourDurationMillis / 60000L) + " minutos!");
                Bukkit.broadcastMessage("");
                for (Player p : Bukkit.getOnlinePlayers()) {
                    p.playSound(p.getLocation(), Sound.ENTITY_ENDER_DRAGON_GROWL, 0.7f, 1.4f);
                }
            }
        }

        // 0.3% de chance de spawnar uma Caixa dos Duendes
        if (random.nextDouble() < giftChance && !activeGifts.containsKey(brokenBlock.getLocation()) && !activeMeteorites.containsKey(brokenBlock.getLocation())) {
            Location loc = brokenBlock.getLocation();
            loc.getBlock().setType(Material.ENDER_CHEST);
            activeGifts.put(loc, System.currentTimeMillis() + giftLifetimeMillis);

            triggerPlayer.playSound(loc, Sound.BLOCK_CHEST_OPEN, 1f, 1.5f);
            triggerPlayer.sendMessage(ChatColor.LIGHT_PURPLE + "" + ChatColor.BOLD + "PRESENTE OCULTO! " +
                    ChatColor.YELLOW + "Encontraste uma Caixa dos Duendes! Clica nela antes que desapareça ("
                            + (giftLifetimeMillis / 1000L) + "s)!");
        }
        // 0.04% de chance de spawnar um Meteorito Estelar
        else if (random.nextDouble() < meteoriteChance && !activeMeteorites.containsKey(brokenBlock.getLocation()) && !activeGifts.containsKey(brokenBlock.getLocation())) {
            Location loc = brokenBlock.getLocation();
            loc.getBlock().setType(Material.SEA_LANTERN);
            activeMeteorites.put(loc, meteoriteHealth);

            loc.getWorld().spawnParticle(Particle.EXPLOSION_EMITTER, loc, 1);
            triggerPlayer.playSound(loc, Sound.ENTITY_GENERIC_EXPLODE, 1f, 0.8f);
            Bukkit.broadcastMessage(ChatColor.GOLD + "" + ChatColor.BOLD + "[METEORITO] " +
                    ChatColor.YELLOW + "Um Meteorito Estelar caiu perto de " + ChatColor.AQUA + triggerPlayer.getName() +
                    ChatColor.YELLOW + " na mina! Quebrem-no para ganhar muitos Tokens!");
        }
    }

    public boolean isSpecialBlock(Location loc) {
        return activeGifts.containsKey(loc) || activeMeteorites.containsKey(loc);
    }

    public boolean handleGiftInteract(Player player, Block block) {
        Location loc = block.getLocation();
        if (!activeGifts.containsKey(loc)) return false;

        activeGifts.remove(loc);
        block.setType(Material.AIR);

        TWUser user = TWCorePlugin.getInstance().getUserManager().getUser(player.getUniqueId());
        if (user == null) return true;

        long bonusTokens = 50 + random.nextInt(151);
        double bonusMoney = (user.getPickaxeLevel() * 15000.0) * (1 + random.nextInt(4));

        user.addTokens(bonusTokens);
        user.addBalance(bonusMoney);

        player.addPotionEffect(new PotionEffect(PotionEffectType.HASTE, 20 * 15, 3), true);
        player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 20 * 15, 2), true);

        player.getWorld().spawnParticle(Particle.HEART, loc.add(0.5, 0.5, 0.5), 1);
        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1f, 2f);
        player.sendMessage(ChatColor.LIGHT_PURPLE + "" + ChatColor.BOLD + "CAIXA DOS DUENDES ABERTA! " +
                ChatColor.AQUA + "+" + bonusTokens + " Tokens" + ChatColor.WHITE + ", " +
                ChatColor.GREEN + "+$" + TWFormat.formatarDinheiro(bonusMoney) + ChatColor.WHITE + " e " +
                ChatColor.YELLOW + "Frenesi (15s)!");

        TWMinasPlugin.getInstance().getCartinhasManager().incrementProgress(
                player, user, CartinhasManager.MissionType.PRESENTES, 1, false
        );
        return true;
    }

    public boolean handleMeteoriteHit(Player player, Block block) {
        Location loc = block.getLocation();
        Integer hp = activeMeteorites.get(loc);
        if (hp == null) return false;

        TWUser user = TWCorePlugin.getInstance().getUserManager().getUser(player.getUniqueId());
        if (user == null) return true;

        int newHp = hp - 1;
        long hitTokens = 15 + random.nextInt(20);
        user.addTokens(hitTokens);

        if (newHp <= 0) {
            activeMeteorites.remove(loc);
            block.setType(Material.AIR);
            long finalBonus = 250L;
            user.addTokens(finalBonus);
            player.playSound(loc, Sound.ENTITY_GENERIC_EXPLODE, 1f, 1.5f);
            player.sendMessage(ChatColor.GOLD + "" + ChatColor.BOLD + "METEORITO DESTRUÍDO! " +
                    ChatColor.AQUA + "Ganhaste o bónus final de +" + finalBonus + " Tokens!");
        } else {
            activeMeteorites.put(loc, newHp);
            player.playSound(loc, Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.6f, 1.8f);
            loc.getWorld().spawnParticle(Particle.LAVA, loc.clone().add(0.5, 0.5, 0.5), 1);
        }
        return true;
    }

    public void cleanupAll() {
        bossBarTask.cancel();
        cleanupTask.cancel();
        TWBossBar.removeAll();
        for (Location loc : activeGifts.keySet()) {
            if (loc.getWorld().isChunkLoaded(loc.getBlockX() >> 4, loc.getBlockZ() >> 4)
                    && loc.getBlock().getType() == Material.ENDER_CHEST) loc.getBlock().setType(Material.AIR);
        }
        for (Location loc : activeMeteorites.keySet()) {
            if (loc.getWorld().isChunkLoaded(loc.getBlockX() >> 4, loc.getBlockZ() >> 4)
                    && loc.getBlock().getType() == Material.SEA_LANTERN) loc.getBlock().setType(Material.AIR);
        }
        activeGifts.clear();
        activeMeteorites.clear();
    }

    private long requireLongRange(TWMinasPlugin plugin, String path, long minimum, long maximum) {
        long value = plugin.getConfig().getLong(path, -1L);
        if (value < minimum || value > maximum) {
            throw new IllegalArgumentException("Configuração inválida em " + path + ": permitido de "
                    + minimum + " até " + maximum + ".");
        }
        return value;
    }
}