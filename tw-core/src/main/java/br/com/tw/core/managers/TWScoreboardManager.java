package br.com.tw.core.managers;

import br.com.tw.core.TWCorePlugin;
import br.com.tw.core.data.TWUser;
import br.com.tw.core.utils.TWFormat;
import br.com.tw.core.utils.TWConfigValidation;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class TWScoreboardManager {

    private final TWCorePlugin plugin;
    private BukkitTask updateTask;
    private boolean providerFailureLogged;
    private boolean playerUpdateFailureLogged;

    public TWScoreboardManager(TWCorePlugin plugin) {
        this.plugin = plugin;
        startUpdateTask();
    }

    private void startUpdateTask() {
        int updateTicks = TWConfigValidation.requireIntRange(plugin, "scoreboard.update-ticks",
                plugin.getConfig().getInt("scoreboard.update-ticks", 20), 10, 200);
        updateTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            for (Player player : Bukkit.getOnlinePlayers()) {
                try {
                    updateScoreboard(player);
                } catch (RuntimeException e) {
                    if (!playerUpdateFailureLogged) {
                        plugin.getLogger().log(java.util.logging.Level.SEVERE,
                                "Falha ao atualizar o scoreboard de " + player.getName() + ".", e);
                        playerUpdateFailureLogged = true;
                    }
                }
            }
        }, updateTicks, updateTicks);
    }

    public void stop() {
        if (updateTask != null) {
            updateTask.cancel();
        }
    }

    private void updateScoreboard(Player player) {
        Scoreboard board = player.getScoreboard();
        Objective obj = board.getObjective("twboard");
        if (obj == null) {
            board = Bukkit.getScoreboardManager().getNewScoreboard();
            obj = board.registerNewObjective("twboard", "dummy");
            obj.setDisplaySlot(DisplaySlot.SIDEBAR);
            obj.setDisplayName("§b§lTW NETWORK");
            player.setScoreboard(board);
        } else if (obj.getDisplaySlot() != DisplaySlot.SIDEBAR) {
            obj.setDisplaySlot(DisplaySlot.SIDEBAR);
        }

        TWUser user = plugin.getUserManager().getUser(player.getUniqueId());
        if (user == null) return;

        String world = player.getWorld().getName().toLowerCase();
        List<String> lines = new ArrayList<>();
        lines.add("§1");
        lines.add("§fJogador: §a" + player.getName());
        lines.add("§fRank: " + getRankPrefix(user.getRankId()));
        lines.add("§2");
        lines.add("§fSaldo: §2$" + TWFormat.formatarDinheiro(user.getBalance()));
        lines.add("§fTokens: §b" + TWFormat.formatarDinheiro(user.getTokens()));
        lines.add("§3");

        if (plugin.isMineWorld(world)) {
            lines.add("§e§lMina");
            lines.add("§fPicareta Nível: §a" + user.getPickaxeLevel());
            lines.add("§fBlocos: §e" + TWFormat.formatarDinheiro(user.getBlocosQuebrados()));
        } else if (plugin.isPlotsWorld(world)) {
            lines.add("§a§lTerrenos");
            lines.add("§fJogadores: §a" + player.getWorld().getPlayers().size());
            for (RegisteredServiceProvider<TWScoreboardStatsProvider> registration
                    : Bukkit.getServicesManager().getRegistrations(TWScoreboardStatsProvider.class)) {
                try {
                    List<String> stats = registration.getProvider().getScoreboardLines(player);
                    if (stats != null) {
                        for (String stat : stats) {
                            if (stat != null && lines.size() < 13) lines.add(stat);
                        }
                    }
                } catch (RuntimeException e) {
                    if (!providerFailureLogged) {
                        plugin.getLogger().log(java.util.logging.Level.SEVERE,
                                "Um provedor de estatísticas do scoreboard falhou.", e);
                        providerFailureLogged = true;
                    }
                }
            }
        } else {
            lines.add("§d§lLobby Central");
            lines.add("§fJogadores: §a" + player.getWorld().getPlayers().size());
            int ping = getPing(player);
            lines.add(ping < 0 ? "§fPing: §7indisponível" : "§fPing: §a" + ping + "ms");
        }

        lines.add("§4");
        lines.add("§e jogar.twnetwork.com");
        Set<String> desiredEntries = new HashSet<>();
        int visibleLines = Math.min(15, lines.size());
        for (int i = 0; i < visibleLines; i++) {
            int score = 15 - i;
            String entry = ChatColor.values()[score].toString();
            desiredEntries.add(entry);
            setLine(board, obj, score, entry, lines.get(i));
        }
        for (String entry : board.getEntries()) {
            if (!desiredEntries.contains(entry)) {
                board.resetScores(entry);
                for (Team team : board.getTeams()) {
                    if (team.hasEntry(entry)) team.removeEntry(entry);
                }
            }
        }
    }

    private void setLine(Scoreboard board, Objective obj, int line, String entry, String text) {
        String teamName = "line" + line;
        Team team = board.getTeam(teamName);
        if (team == null) {
            team = board.registerNewTeam(teamName);
            team.addEntry(entry);
        }

        int split = Math.min(16, text.length());
        if (split < text.length() && split > 0 && text.charAt(split - 1) == '§') split--;
        String prefix = text.substring(0, split);
        String suffix = text.length() > split ? text.substring(split, Math.min(text.length(), split + 16)) : "";

        if (!prefix.equals(team.getPrefix())) team.setPrefix(prefix);
        if (!suffix.equals(team.getSuffix())) team.setSuffix(suffix);
        if (!board.getEntries().contains(entry) || obj.getScore(entry).getScore() != line) {
            obj.getScore(entry).setScore(line);
        }
    }

    private String getRankPrefix(String rankId) {
        // Lógica simples; idealmente puxa da config do tw-ranks
        return "§7[" + rankId + "]";
    }

    private int getPing(Player player) {
        return player.getPing();
    }
}