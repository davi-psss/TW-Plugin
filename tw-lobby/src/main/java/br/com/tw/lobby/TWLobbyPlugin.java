package br.com.tw.lobby;

import br.com.tw.lobby.commands.LobbyCommand;
import br.com.tw.lobby.commands.SetLobbyCommand;
import br.com.tw.lobby.listeners.DoubleJumpListener;
import br.com.tw.lobby.listeners.MundoListener;
import br.com.tw.lobby.listeners.ProtecaoListener;
import br.com.tw.lobby.managers.InventoryManager;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.plugin.java.JavaPlugin;

public class TWLobbyPlugin extends JavaPlugin {
    private static TWLobbyPlugin instance;
    private InventoryManager inventoryManager;

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig(); // Cria a config.yml se não existir

        inventoryManager = new InventoryManager(this);

        // Registar Listeners
        getServer().getPluginManager().registerEvents(new MundoListener(inventoryManager), this);
        getServer().getPluginManager().registerEvents(new ProtecaoListener(), this);
        getServer().getPluginManager().registerEvents(new DoubleJumpListener(), this);

        // Registar Comandos
        getCommand("setlobby").setExecutor(new SetLobbyCommand(this));
        getCommand("lobby").setExecutor(new LobbyCommand(this));

        getLogger().info("[TW-Lobby] Sistema de transicao e seguranca iniciado!");
    }

    public static TWLobbyPlugin getInstance() {
        return instance;
    }

    // Método para ler o Lobby da configuração
    public Location getLobbyLocation() {
        if (getConfig().contains("lobby.world")) {
            World w = Bukkit.getWorld(getConfig().getString("lobby.world"));
            double x = getConfig().getDouble("lobby.x");
            double y = getConfig().getDouble("lobby.y");
            double z = getConfig().getDouble("lobby.z");
            float yaw = (float) getConfig().getDouble("lobby.yaw");
            float pitch = (float) getConfig().getDouble("lobby.pitch");
            if (w != null) return new Location(w, x, y, z, yaw, pitch);
        }
        // Fallback: se não estiver setado, pega o spawn do mundo 0
        return Bukkit.getWorlds().get(0).getSpawnLocation();
    }

    public String getLobbyWorldName() {
        if (getConfig().contains("lobby.world")) {
            return getConfig().getString("lobby.world");
        }
        return Bukkit.getWorlds().get(0).getName();
    }
}