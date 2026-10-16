package br.com.tw.core;

import br.com.tw.core.data.UserManager;
import br.com.tw.core.data.UserRepository;
import br.com.tw.core.database.DatabaseManager;
import org.bukkit.plugin.java.JavaPlugin;

public class TWCorePlugin extends JavaPlugin {

    private static TWCorePlugin instance;
    private DatabaseManager databaseManager;
    private UserManager userManager;

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();

        databaseManager = new DatabaseManager(this);
        try {
            databaseManager.connect();
        } catch (Exception e) {
            getLogger().severe("Falha ao conectar no banco de dados!");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        // Inicializa o Repositório (que cria a tabela) e o Manager
        UserRepository userRepository = new UserRepository(databaseManager);
        userManager = new UserManager(this, userRepository);

        // Registra os eventos de Join e Quit
        getServer().getPluginManager().registerEvents(userManager, this);

        getServer().getPluginManager().registerEvents(new br.com.tw.core.menus.MenuListener(), this);

        getLogger().info("[TW-Core] Nucleo de dados carregado com sucesso!");
    }

    @Override
    public void onDisable() {
        if (userManager != null) {
            getLogger().info("[TW-Core] Salvando dados de todos os jogadores...");
            userManager.saveAllSync(); // Salva de forma síncrona para não cortar o processo no meio
        }
        if (databaseManager != null) {
            databaseManager.disconnect();
        }
    }

    public static TWCorePlugin getInstance() {
        return instance;
    }

    public DatabaseManager getDatabaseManager() {
        return databaseManager;
    }

    public UserManager getUserManager() {
        return userManager;
    }
}