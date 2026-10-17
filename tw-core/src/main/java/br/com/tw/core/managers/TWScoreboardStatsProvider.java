package br.com.tw.core.managers;

import org.bukkit.entity.Player;

import java.util.List;

public interface TWScoreboardStatsProvider {

    List<String> getScoreboardLines(Player player);
}
