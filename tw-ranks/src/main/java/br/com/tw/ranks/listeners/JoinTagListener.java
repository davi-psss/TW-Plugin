package br.com.tw.ranks.listeners;

import br.com.tw.core.TWCorePlugin;
import br.com.tw.core.data.TWUser;
import br.com.tw.core.utils.TWFormat;
import br.com.tw.ranks.TWRanksPlugin;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class JoinTagListener {

    private final TWRanksPlugin plugin;
    private final List<String> rankOrder;
    private final Set<String> configuredRanks;
    private final Set<String> configuredPrestiges;
    private final Map<UUID, String> lastHeaderFooters = new HashMap<>();
    private BukkitTask updateTask;

    public JoinTagListener(TWRanksPlugin plugin) {
        this.plugin = plugin;
        rankOrder = plugin.getRanksConfig().getStringList("rank-order");
        configuredRanks = getConfiguredIds("ranks");
        configuredPrestiges = getConfiguredIds("prestiges");
    }

    private Set<String> getConfiguredIds(String path) {
        ConfigurationSection section = plugin.getRanksConfig().getConfigurationSection(path);
        if (section == null) {
            plugin.getLogger().warning("A secao '" + path
                    + "' nao existe em ranks.yml; sera usada a ordenacao de fallback no TAB.");
            return new LinkedHashSet<>();
        }
        return new LinkedHashSet<>(section.getKeys(false));
    }

    public void start() {
        updateTask = Bukkit.getScheduler().runTaskTimer(plugin, this::updateAll, 1L, 20L);
    }

    public void stop() {
        if (updateTask != null) {
            updateTask.cancel();
        }
        lastHeaderFooters.clear();
    }

    private void updateAll() {
        Collection<? extends Player> players = Bukkit.getOnlinePlayers();
        for (Player viewer : players) {
            updateTabForViewer(viewer, players);
        }
        for (Iterator<UUID> iterator = lastHeaderFooters.keySet().iterator(); iterator.hasNext();) {
            if (Bukkit.getPlayer(iterator.next()) == null) {
                iterator.remove();
            }
        }
    }

    private void updateTabForViewer(Player viewer, Collection<? extends Player> players) {
        Scoreboard scoreboard = viewer.getScoreboard();
        for (Player listedPlayer : players) {
            TWUser user = TWCorePlugin.getInstance().getUserManager().getUser(listedPlayer.getUniqueId());
            if (user == null) continue;

            Team team = getOrCreateTeam(scoreboard, user);
            String displayName = getDisplayName(listedPlayer, user);
            if (!displayName.equals(listedPlayer.getDisplayName())) {
                listedPlayer.setDisplayName(displayName);
            }

            Team currentTeam = scoreboard.getEntryTeam(listedPlayer.getName());
            if (currentTeam != team) {
                team.addEntry(listedPlayer.getName());
            }
        }

        TWUser user = TWCorePlugin.getInstance().getUserManager().getUser(viewer.getUniqueId());
        String content = createHeaderFooter(viewer, user);
        if (!content.equals(lastHeaderFooters.get(viewer.getUniqueId()))) {
            TabHeaderFooter.send(viewer, content);
            lastHeaderFooters.put(viewer.getUniqueId(), content);
        }
    }

    private Team getOrCreateTeam(Scoreboard scoreboard, TWUser user) {
        String teamName = getTeamName(user);
        Team team = scoreboard.getTeam(teamName);
        if (team == null) {
            team = scoreboard.registerNewTeam(teamName);
        }
        String prefix = getTabPrefix(user);
        if (!prefix.equals(team.getPrefix())) {
            team.setPrefix(prefix);
        }
        return team;
    }

    private String getTeamName(TWUser user) {
        String rankId = user.getRankId();
        String prestigeId = user.getPrestigeId();
        int rankIndex = getIndex(configuredRanks, rankOrder, rankId);
        int prestigeIndex = getIndex(configuredPrestiges, null, prestigeId);
        return String.format(Locale.ROOT, "twtab%02d%02d", rankIndex, prestigeIndex);
    }

    private int getIndex(Set<String> configured, List<String> preferredOrder, String id) {
        if (id == null || id.equalsIgnoreCase("NENHUM")) return 99;
        if (preferredOrder != null) {
            for (int i = 0; i < preferredOrder.size(); i++) {
                String rank = preferredOrder.get(i);
                if (rank != null && rank.equalsIgnoreCase(id)) return i;
            }
        }
        int index = 0;
        for (String value : configured) {
            if (value.equalsIgnoreCase(id)) return index;
            index++;
        }
        return 98;
    }

    private String getTabPrefix(TWUser user) {
        String rank = user.getRankId();
        String rankColor = getConfiguredColor("ranks." + rank + ".prefix", ChatColor.GRAY);
        String prefix = "";
        String prestige = user.getPrestigeId();
        if (prestige != null && !prestige.equalsIgnoreCase("NENHUM")) {
            String prestigeColor = getConfiguredColor("prestiges." + prestige + ".prefix", ChatColor.RED);
            prefix = prestigeColor + "[" + abbreviation(prestige) + "] ";
        }
        return prefix + rankColor + "[" + abbreviation(rank) + "] ";
    }

    private String getDisplayName(Player player, TWUser user) {
        String rank = user.getRankId();
        String prefix = plugin.getRanksConfig().getString("ranks." + rank + ".prefix", "&7[" + rank + "]");
        String prestige = user.getPrestigeId();
        if (prestige != null && !prestige.equalsIgnoreCase("NENHUM")) {
            String prestigePrefix = plugin.getRanksConfig()
                    .getString("prestiges." + prestige + ".prefix", "&c[" + prestige + "]");
            prefix = prestigePrefix + " " + prefix;
        }
        return ChatColor.translateAlternateColorCodes('&', prefix) + " "
                + ChatColor.GRAY + player.getName() + ChatColor.RESET;
    }

    private String getConfiguredColor(String path, ChatColor fallback) {
        String value = plugin.getRanksConfig().getString(path, "");
        String translated = ChatColor.translateAlternateColorCodes('&', value);
        for (int i = 0; i < translated.length() - 1; i++) {
            if (translated.charAt(i) == ChatColor.COLOR_CHAR) {
                ChatColor color = ChatColor.getByChar(translated.charAt(i + 1));
                if (color != null) return color.toString();
            }
        }
        return fallback.toString();
    }

    private String abbreviation(String id) {
        String[] words = id.split("[^A-Za-z0-9]+");
        StringBuilder abbreviation = new StringBuilder();
        if (words.length > 1) {
            for (String word : words) {
                if (!word.isEmpty()) abbreviation.append(Character.toUpperCase(word.charAt(0)));
            }
        } else {
            abbreviation.append(id.substring(0, Math.min(3, id.length())).toUpperCase(Locale.ROOT));
        }
        return abbreviation.toString();
    }

    private String createHeaderFooter(Player player, TWUser user) {
        int online = Bukkit.getOnlinePlayers().size();
        String header = "§8§m--------------------\n§b§lTW NETWORK\n"
                + "§7A aventura começa aqui\n§8§m--------------------";
        String rankText = user == null ? "§7Carregando..." : getTabPrefix(user).trim();
        String balanceText = user == null ? "§7..." : "§a$" + TWFormat.formatarDinheiro(user.getBalance());
        String world = player.getWorld().getName();
        String footer = "§7Online: §f" + online + " §8| §7Mundo: §f" + world
                + "\n§7Rank: " + rankText + " §8| §7Saldo: " + balanceText
                + "\n§bplay.twnetwork.com";
        return header + "\n" + footer;
    }

    private static final class TabHeaderFooter {
        private static void send(Player player, String content) {
            int separator = content.indexOf("\n§7Online:");
            String header = separator < 0 ? content : content.substring(0, separator);
            String footer = separator < 0 ? "" : content.substring(separator + 1);
            player.setPlayerListHeaderFooter(header, footer);
        }
    }
}
