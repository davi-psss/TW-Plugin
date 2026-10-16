package br.com.tw.core.managers;

import br.com.tw.core.TWCorePlugin;
import br.com.tw.core.data.TWUser;
import br.com.tw.core.utils.TWFormat;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

public class TWScoreboardManager {

    private final TWCorePlugin plugin;

    public TWScoreboardManager(TWCorePlugin plugin) {
        this.plugin = plugin;
        startUpdateTask();
    }

    private void startUpdateTask() {
        Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            for (Player player : Bukkit.getOnlinePlayers()) {
                updateScoreboard(player);
            }
        }, 20L, 20L);
    }

    private void updateScoreboard(Player player) {
        Scoreboard board = player.getScoreboard();
        if (board == Bukkit.getScoreboardManager().getMainScoreboard()) {
            board = Bukkit.getScoreboardManager().getNewScoreboard();
            Objective obj = board.registerNewObjective("twboard", "dummy");
            obj.setDisplaySlot(DisplaySlot.SIDEBAR);
            obj.setDisplayName("§b§lTW NETWORK");
            player.setScoreboard(board);
        }

        Objective obj = board.getObjective("twboard");
        TWUser user = plugin.getUserManager().getUser(player.getUniqueId());
        if (user == null) return;

        String world = player.getWorld().getName().toLowerCase();

        // Limpa as linhas atuais para recriar o contexto
        for (String entry : board.getEntries()) {
            board.resetScores(entry);
        }

        int line = 15;
        setLine(board, obj, line--, "§1");
        setLine(board, obj, line--, "§fJogador: §a" + player.getName());
        setLine(board, obj, line--, "§fRank: " + getRankPrefix(user.getRankId()));
        setLine(board, obj, line--, "§2");
        setLine(board, obj, line--, "§fSaldo: §2$" + TWFormat.formatarDinheiro(user.getBalance()));
        setLine(board, obj, line--, "§fTokens: §b" + TWFormat.formatarDinheiro(user.getTokens()));
        setLine(board, obj, line--, "§3");

        if (world.equals("minas")) {
            setLine(board, obj, line--, "§e§lMina Local:");
            setLine(board, obj, line--, "§fPicareta Nível: §a" + user.getPickaxeLevel());
            setLine(board, obj, line--, "§fBlocos: §e" + TWFormat.formatarDinheiro(user.getBlocosQuebrados()));
            // Integração com Happy Hour pode ser puxada via API do tw-minas
        } else if (world.equals("terrenos") || world.equals("plots")) {
            setLine(board, obj, line--, "§a§lO Teu Terreno:");
            setLine(board, obj, line--, "§fMáquinas: §eAtivas");
            setLine(board, obj, line--, "§fGeradores: §eAtivos");
        } else {
            setLine(board, obj, line--, "§d§lLobby Central");
            setLine(board, obj, line--, "§fPing: §a" + getPing(player) + "ms");
        }

        setLine(board, obj, line--, "§4");
        setLine(board, obj, line--, "§e jogar.twnetwork.com");
    }

    private void setLine(Scoreboard board, Objective obj, int line, String text) {
        // Uso de Teams para evitar o limite de 16 caracteres do 1.8.8 e evitar flickers visuais
        String teamName = "line" + line;
        Team team = board.getTeam(teamName);
        if (team == null) {
            team = board.registerNewTeam(teamName);
            team.addEntry(ChatColor.values()[line].toString());
        }

        String prefix = text.length() > 16 ? text.substring(0, 16) : text;
        String suffix = text.length() > 16 ? text.substring(16, Math.min(text.length(), 32)) : "";

        team.setPrefix(prefix);
        team.setSuffix(suffix);
        obj.getScore(ChatColor.values()[line].toString()).setScore(line);
    }

    private String getRankPrefix(String rankId) {
        // Lógica simples; idealmente puxa da config do tw-ranks
        return "§7[" + rankId + "]";
    }

    private int getPing(Player player) {
        try {
            Object entityPlayer = player.getClass().getMethod("getHandle").invoke(player);
            return (int) entityPlayer.getClass().getField("ping").get(entityPlayer);
        } catch (Exception e) { return 0; }
    }
}