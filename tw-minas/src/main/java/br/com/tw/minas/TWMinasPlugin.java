package br.com.tw.minas;

import br.com.tw.minas.commands.MinaCommand;
import br.com.tw.minas.listeners.MineProtectionListener;
import br.com.tw.minas.listeners.MineracaoListener;
import br.com.tw.minas.managers.CartinhasManager;
import br.com.tw.minas.managers.MineEventManager;
import br.com.tw.minas.integrations.RealMinesBridge;
import org.bukkit.plugin.java.JavaPlugin;

public class TWMinasPlugin extends JavaPlugin {

    private static TWMinasPlugin instance;
    private CartinhasManager cartinhasManager;
    private MineEventManager mineEventManager;
    private RealMinesBridge realMinesBridge;

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();

        org.bukkit.plugin.Plugin realMines = getServer().getPluginManager().getPlugin("RealMines");
        if (realMines != null && realMines.isEnabled()) {
            try {
                realMinesBridge = new RealMinesBridge(realMines);
            } catch (ReflectiveOperationException e) {
                getLogger().log(java.util.logging.Level.SEVERE,
                        "RealMines foi detetado, mas a integração com a API carregada não pôde ser iniciada.", e);
                getServer().getPluginManager().disablePlugin(this);
                return;
            }
        }

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
    public RealMinesBridge getRealMinesBridge() { return realMinesBridge; }
}