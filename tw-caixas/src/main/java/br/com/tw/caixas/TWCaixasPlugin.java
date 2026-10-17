package br.com.tw.caixas;

import br.com.tw.caixas.commands.CaixaCommand;
import br.com.tw.caixas.commands.KeyCommand;
import br.com.tw.caixas.config.CrateCatalog;
import br.com.tw.caixas.data.CrateRepository;
import br.com.tw.caixas.listeners.CrateListener;
import br.com.tw.caixas.managers.CrateOpenings;
import br.com.tw.caixas.managers.CrateService;
import br.com.tw.caixas.managers.PlayerEffectMemory;
import br.com.tw.caixas.managers.RewardDelivery;
import br.com.tw.core.TWCorePlugin;
import org.bukkit.plugin.java.JavaPlugin;

public final class TWCaixasPlugin extends JavaPlugin {
    private CrateRepository repository;
    private CrateService crateService;
    private CrateOpenings openings;
    private RewardDelivery rewardDelivery;
    private PlayerEffectMemory effectMemory;
    private CrateListener crateListener;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        if (TWCorePlugin.getInstance() == null || TWCorePlugin.getInstance().getDatabaseManager() == null) {
            throw new IllegalStateException("tw-core precisa iniciar antes de tw-caixas.");
        }
        if (br.com.tw.economia.TWEconomiaPlugin.getInstance() == null) {
            throw new IllegalStateException("tw-economia precisa iniciar antes de tw-caixas.");
        }
        CrateCatalog catalog = CrateCatalog.load(this);
        repository = new CrateRepository(TWCorePlugin.getInstance().getDatabaseManager());
        repository.initialize();
        crateService = new CrateService(this, repository, catalog);
        effectMemory = new PlayerEffectMemory(this, repository);
        rewardDelivery = new RewardDelivery(this, crateService);
        openings = new CrateOpenings(this, crateService, rewardDelivery);
        crateListener = new CrateListener(this, crateService, openings, rewardDelivery, effectMemory);
        crateService.loadMutantCrops();

        getCommand("key").setExecutor(new KeyCommand(this, crateService, openings, effectMemory));
        getCommand("caixa").setExecutor(new CaixaCommand(this, crateService, crateListener));
        getServer().getPluginManager().registerEvents(crateListener, this);
        for (org.bukkit.entity.Player player : getServer().getOnlinePlayers()) {
            crateService.loadAccount(player.getUniqueId(), account -> { },
                    () -> getLogger().warning("Não foi possível carregar o saldo de caixas de um jogador online."));
            effectMemory.load(player.getUniqueId());
        }
        crateService.refreshPhysicalCrates(crateListener::reloadPhysicalCrates);
        getLogger().info("[TW-Caixas] Chaves virtuais, pity, forja e caixas físicas ativados.");
    }

    @Override
    public void onDisable() {
        if (effectMemory != null) effectMemory.close();
        if (crateListener != null) crateListener.removeHolograms();
    }

    public CrateRepository getRepository() { return repository; }
    public CrateService getCrateService() { return crateService; }
    public CrateOpenings getOpenings() { return openings; }
}
