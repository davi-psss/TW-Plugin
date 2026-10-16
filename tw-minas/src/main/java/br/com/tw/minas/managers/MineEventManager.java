package br.com.tw.minas.managers;

import br.com.tw.core.TWCorePlugin;
import br.com.tw.core.data.TWUser;
import br.com.tw.core.utils.TWFormat;
import br.com.tw.minas.TWMinasPlugin;
import br.com.tw.minas.utils.TWBossBar;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Effect;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

public class MineEventManager {

    private static final long COMMUNITY_GOAL = 5000L;
    private long communityProgress = 0L;
    private long happyHourEndTime = 0L;

    private final Map<Location, Long> activeGifts = new ConcurrentHashMap<>();
    private final Map<Location, Integer> activeMeteorites = new ConcurrentHashMap<>();

    private final Random random = new Random();

    public MineEventManager(TWMinasPlugin plugin) {
        // Task 1: Atualização contínua da BossBar para quem está no mundo das minas
        Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            float progress = (float) communityProgress / COMMUNITY_GOAL;
            String barMessage = isHappyHourActive()
                    ? "§d§lHAPPY HOUR ATIVO! TUDO A 2.0x!"
                    : "§b§lMETA COMUNITÁRIA: §e" + communityProgress + " §7/ §e" + COMMUNITY_GOAL + " blocos";

            for (Player p : Bukkit.getOnlinePlayers()) {
                if (p.getWorld().getName().equalsIgnoreCase("minas")) {
                    TWBossBar.sendBossBar(p, barMessage, isHappyHourActive() ? 1.0f : progress);
                } else {
                    TWBossBar.removeBossBar(p);
                }
            }
        }, 10L, 10L);

        // Task 2: Limpeza automática de Caixas dos Duendes que expiraram (12s)
        Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            long now = System.currentTimeMillis();
            for (Map.Entry<Location, Long> entry : activeGifts.entrySet()) {
                if (now > entry.getValue()) {
                    Location loc = entry.getKey();
                    activeGifts.remove(loc);
                    if (loc.getBlock().getType() == Material.ENDER_CHEST) {
                        loc.getBlock().setType(Material.AIR);
                    }
                }
            }
        }, 20L, 20L);
    }

    public boolean isHappyHourActive() {
        return System.currentTimeMillis() < happyHourEndTime;
    }

    public double getEventMultiplier() {
        return isHappyHourActive() ? 2.0 : 1.0;
    }

    public long getCommunityProgress() {
        return communityProgress;
    }

    public long getCommunityGoal() {
        return COMMUNITY_GOAL;
    }

    public void addCommunityBlocks(Player triggerPlayer, Block brokenBlock, int count) {
        if (!isHappyHourActive()) {
            communityProgress += count;
            if (communityProgress >= COMMUNITY_GOAL) {
                communityProgress = 0L;
                happyHourEndTime = System.currentTimeMillis() + (3 * 60 * 1000L); // 3 minutos
                Bukkit.broadcastMessage("");
                Bukkit.broadcastMessage(ChatColor.AQUA + "" + ChatColor.BOLD + "[TW-MINAS] " + ChatColor.YELLOW + "" + ChatColor.BOLD + "HAPPY HOUR NATALINO ATIVADO!");
                Bukkit.broadcastMessage(ChatColor.GREEN + "A meta comunitária de " + COMMUNITY_GOAL + " blocos foi atingida!");
                Bukkit.broadcastMessage(ChatColor.GOLD + "Todos os blocos e tokens nas minas valem " + ChatColor.BOLD + "2.0x" + ChatColor.GOLD + " pelos próximos 3 minutos!");
                Bukkit.broadcastMessage("");
                for (Player p : Bukkit.getOnlinePlayers()) {
                    p.playSound(p.getLocation(), Sound.ENDERDRAGON_GROWL, 0.7f, 1.4f);
                }
            }
        }

        // 0.3% de chance de spawnar uma Caixa dos Duendes
        if (random.nextDouble() < 0.003 && !activeGifts.containsKey(brokenBlock.getLocation()) && !activeMeteorites.containsKey(brokenBlock.getLocation())) {
            Location loc = brokenBlock.getLocation();
            loc.getBlock().setType(Material.ENDER_CHEST);
            activeGifts.put(loc, System.currentTimeMillis() + 12000L);

            triggerPlayer.playSound(loc, Sound.CHEST_OPEN, 1f, 1.5f);
            triggerPlayer.sendMessage(ChatColor.LIGHT_PURPLE + "" + ChatColor.BOLD + "PRESENTE OCULTO! " +
                    ChatColor.YELLOW + "Encontraste uma Caixa dos Duendes! Clica nela antes que desapareça (12s)!");
        }
        // 0.04% de chance de spawnar um Meteorito Estelar
        else if (random.nextDouble() < 0.0004 && !activeMeteorites.containsKey(brokenBlock.getLocation()) && !activeGifts.containsKey(brokenBlock.getLocation())) {
            Location loc = brokenBlock.getLocation();
            loc.getBlock().setType(Material.SEA_LANTERN);
            activeMeteorites.put(loc, 30);

            loc.getWorld().playEffect(loc, Effect.EXPLOSION_LARGE, 0);
            triggerPlayer.playSound(loc, Sound.EXPLODE, 1f, 0.8f);
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

        player.addPotionEffect(new PotionEffect(PotionEffectType.FAST_DIGGING, 20 * 15, 3), true);
        player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 20 * 15, 2), true);

        player.getWorld().playEffect(loc.add(0.5, 0.5, 0.5), Effect.HEART, 0);
        player.playSound(player.getLocation(), Sound.LEVEL_UP, 1f, 2f);
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
            player.playSound(loc, Sound.EXPLODE, 1f, 1.5f);
            player.sendMessage(ChatColor.GOLD + "" + ChatColor.BOLD + "METEORITO DESTRUÍDO! " +
                    ChatColor.AQUA + "Ganhaste o bónus final de +" + finalBonus + " Tokens!");
        } else {
            activeMeteorites.put(loc, newHp);
            player.playSound(loc, Sound.ORB_PICKUP, 0.6f, 1.8f);
            loc.getWorld().playEffect(loc.clone().add(0.5, 0.5, 0.5), Effect.LAVA_POP, 0);
        }
        return true;
    }

    public void cleanupAll() {
        for (Location loc : activeGifts.keySet()) {
            loc.getBlock().setType(Material.AIR);
        }
        for (Location loc : activeMeteorites.keySet()) {
            loc.getBlock().setType(Material.AIR);
        }
        activeGifts.clear();
        activeMeteorites.clear();
    }
}