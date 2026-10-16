package br.com.tw.ranks;

import br.com.tw.ranks.commands.AdminRanksCommand;
import br.com.tw.ranks.commands.PrestigioCommand;
import br.com.tw.ranks.commands.RanksCommand;
import br.com.tw.ranks.commands.RankupCommand;
import br.com.tw.ranks.commands.RankupMaxCommand;
import br.com.tw.ranks.hooks.TWRankExpansion;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;

public class TWRanksPlugin extends JavaPlugin {
    private static TWRanksPlugin instance;
    private FileConfiguration ranksConfig;

    @Override
    public void onEnable() {
        instance = this;
        createCustomConfig();

        // Carrega a configuração do ranks.yml com as novas opções
        this.ranksConfig = YamlConfiguration.loadConfiguration(new File(getDataFolder(), "ranks.yml"));

        // Registo da Expansão do PlaceholderAPI
        if (getServer().getPluginManager().getPlugin("PlaceholderAPI") != null) {
            new TWRankExpansion().register();
            getLogger().info("[TW-Ranks] PlaceholderAPI detetado! Variaveis %tw_rank%, %tw_prestigio% e %tw_tokens% ativadas.");
        }

        // Registo de todos os comandos
        getCommand("rankup").setExecutor(new RankupCommand(this));
        getCommand("prestigio").setExecutor(new PrestigioCommand(this));
        getCommand("twadmin").setExecutor(new AdminRanksCommand());
        getCommand("ranks").setExecutor(new RanksCommand());
        getCommand("rankupmax").setExecutor(new RankupMaxCommand(this));
        getServer().getPluginManager().registerEvents(new br.com.tw.ranks.listeners.JoinTagListener(), this);

        getLogger().info("[TW-Ranks] Sistema de progressao iniciado!");
    }

    private void createCustomConfig() {
        File configFile = new File(getDataFolder(), "ranks.yml");
        if (!configFile.exists()) {
            saveResource("ranks.yml", false);
        }
    }

    public static TWRanksPlugin getInstance() {
        return instance;
    }

    public FileConfiguration getRanksConfig() {
        return ranksConfig;
    }
}