package br.com.tw.maquinas;

import br.com.tw.core.TWCorePlugin;
import br.com.tw.maquinas.commands.MaquinasCommand;
import br.com.tw.maquinas.data.MachineRepository;
import br.com.tw.maquinas.listeners.MachineListener;
import br.com.tw.maquinas.managers.HologramManager;
import br.com.tw.maquinas.managers.MachineManager;
import org.bukkit.plugin.java.JavaPlugin;

public class TWMaquinasPlugin extends JavaPlugin {

    private static TWMaquinasPlugin instance;
    private MachineManager machineManager;
    private HologramManager hologramManager;

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();

        // Inicializa o gerenciador de hologramas nativos
        this.hologramManager = new HologramManager(this);

        MachineRepository repo = new MachineRepository(TWCorePlugin.getInstance().getDatabaseManager());
        this.machineManager = new MachineManager(this, repo);

        getCommand("maquinas").setExecutor(new MaquinasCommand());
        getServer().getPluginManager().registerEvents(new MachineListener(machineManager), this);

        getLogger().info("[TW-Maquinas] Sistema de maquinas e hologramas nativos ativado com sucesso!");
    }

    @Override
    public void onDisable() {
        if (machineManager != null) {
            machineManager.saveAllSync();
        }
        if (hologramManager != null) {
            hologramManager.clearAll();
        }
    }

    public static TWMaquinasPlugin getInstance() {
        return instance;
    }

    public MachineManager getMachineManager() {
        return machineManager;
    }

    public HologramManager getHologramManager() {
        return hologramManager;
    }
}