package br.com.tw.minas;

import br.com.tw.minas.commands.MinaCommand;
import br.com.tw.minas.listeners.MineProtectionListener;
import br.com.tw.minas.listeners.MineracaoListener;
import br.com.tw.minas.managers.CartinhasManager;
import br.com.tw.minas.managers.MineEventManager;
import org.bukkit.plugin.java.JavaPlugin;

public class TWMinasPlugin extends JavaPlugin {

    private static TWMinasPlugin instance;
    private CartinhasManager cartinhasManager;
    private MineEventManager mineEventManager;

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();

        this.cartinhasManager = new CartinhasManager();
        this.mineEventManager = new MineEventManager(this);

        getServer().getPluginManager().registerEvents(new MineracaoListener(), this);
        getServer().getPluginManager().registerEvents(new MineProtectionListener(), this);
        getCommand("mina").setExecutor(new MinaCommand());

        getLogger().info("[TW-Minas] Motor de mineracao, Cartinhas (Missoes) e Eventos carregados com sucesso!");
    }

    @Override
    public void onDisable() {
        if (mineEventManager != null) {
            mineEventManager.cleanupAll();
        }
    }

    public static TWMinasPlugin getInstance() { return instance; }
    public CartinhasManager getCartinhasManager() { return cartinhasManager; }
    public MineEventManager getMineEventManager() { return mineEventManager; }
}