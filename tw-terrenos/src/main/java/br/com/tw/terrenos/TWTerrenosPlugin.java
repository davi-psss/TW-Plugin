package br.com.tw.terrenos;

import br.com.tw.terrenos.commands.TerrenoCommand;
import br.com.tw.terrenos.listeners.TerrenoListener;
import org.bukkit.plugin.java.JavaPlugin;

public class TWTerrenosPlugin extends JavaPlugin {
    private static TWTerrenosPlugin instance;

    @Override
    public void onEnable() {
        instance = this;

        getCommand("terreno").setExecutor(new TerrenoCommand());
        getServer().getPluginManager().registerEvents(new TerrenoListener(), this);

        getLogger().info("[TW-Terrenos] Modulo de terrenos hibrido ativado!");
    }

    public static TWTerrenosPlugin getInstance() {
        return instance;
    }
}