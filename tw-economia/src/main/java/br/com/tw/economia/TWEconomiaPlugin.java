package br.com.tw.economia;

import br.com.tw.economia.commands.BoosterCommand;
import br.com.tw.economia.commands.MoneyCommand;
import org.bukkit.Material;
import org.bukkit.plugin.java.JavaPlugin;

public class TWEconomiaPlugin extends JavaPlugin {
    private static TWEconomiaPlugin instance;
    private double globalBooster = 1.0;

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();

        getCommand("booster").setExecutor(new BoosterCommand(this));
        getCommand("money").setExecutor(new MoneyCommand());
        getCommand("pay").setExecutor(new br.com.tw.economia.commands.PayCommand());
        getCommand("baltop").setExecutor(new br.com.tw.economia.commands.BaltopCommand(this));
        getCommand("eco").setExecutor(new br.com.tw.economia.commands.EcoAdminCommand());
        getCommand("cheque").setExecutor(new br.com.tw.economia.commands.ChequeCommand());
        getServer().getPluginManager().registerEvents(new br.com.tw.economia.listeners.ChequeListener(), this);
        if (getServer().getPluginManager().getPlugin("PlaceholderAPI") != null) {
            new br.com.tw.economia.hooks.TWEcoExpansion().register();
        }
        getLogger().info("[TW-Economia] Motor de precos e multiplicadores iniciado com sucesso de forma independente!");
    }

    public static TWEconomiaPlugin getInstance() {
        return instance;
    }

    public double getGlobalBooster() {
        return globalBooster;
    }

    public void setGlobalBooster(double globalBooster) {
        this.globalBooster = globalBooster;
    }

    public double getBlockPrice(Material material, byte data) {
        String specificPath = "prices." + material.name() + ":" + data;
        if (getConfig().contains(specificPath)) {
            return getConfig().getDouble(specificPath);
        }

        String generalPath = "prices." + material.name();
        if (getConfig().contains(generalPath)) {
            return getConfig().getDouble(generalPath);
        }

        return 0.0;
    }
}