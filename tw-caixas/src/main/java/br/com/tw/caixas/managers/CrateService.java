package br.com.tw.caixas.managers;

import br.com.tw.caixas.TWCaixasPlugin;
import br.com.tw.caixas.config.CrateCatalog;
import br.com.tw.caixas.data.CrateAccount;
import br.com.tw.caixas.data.CrateRepository;
import br.com.tw.caixas.model.CrateType;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

public final class CrateService {
    private final TWCaixasPlugin plugin;
    private final CrateRepository repository;
    private volatile CrateCatalog catalog;
    private final ConcurrentHashMap<UUID, CrateAccount> accounts = new ConcurrentHashMap<>();
    private final Set<UUID> loading = ConcurrentHashMap.newKeySet();
    private final Set<String> mutantCrops = ConcurrentHashMap.newKeySet();

    public CrateService(TWCaixasPlugin plugin, CrateRepository repository, CrateCatalog catalog) {
        this.plugin = plugin;
        this.repository = repository;
        this.catalog = catalog;
    }

    public CrateCatalog getCatalog() { return catalog; }
    public void setCatalog(CrateCatalog catalog) { this.catalog = catalog; }
    public CrateAccount getCachedAccount(UUID playerId) { return accounts.get(playerId); }
    public boolean isMutantCrop(String locationKey) { return mutantCrops.contains(locationKey); }

    public void loadMutantCrops() {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                mutantCrops.addAll(repository.loadMutantCrops());
            } catch (RuntimeException e) {
                plugin.getLogger().log(java.util.logging.Level.SEVERE,
                        "Não foi possível carregar as plantações mutantes.", e);
            }
        });
    }

    public void markMutantCrop(String locationKey, UUID ownerId, Consumer<Boolean> callback) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                boolean created = repository.markMutantCrop(locationKey, ownerId);
                if (created) mutantCrops.add(locationKey);
                Bukkit.getScheduler().runTask(plugin, () -> callback.accept(created));
            } catch (RuntimeException e) {
                plugin.getLogger().log(java.util.logging.Level.SEVERE,
                        "Não foi possível transformar a plantação " + locationKey + ".", e);
                Bukkit.getScheduler().runTask(plugin, () -> callback.accept(false));
            }
        });
    }

    public void loadAccount(UUID playerId, Consumer<CrateAccount> callback, Runnable onFailure) {
        CrateAccount cached = accounts.get(playerId);
        if (cached != null) {
            callback.accept(cached);
            return;
        }
        if (!loading.add(playerId)) {
            Bukkit.getScheduler().runTaskLater(plugin,
                    () -> loadAccount(playerId, callback, onFailure), 2L);
            return;
        }
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                CrateAccount account = repository.loadAccount(playerId);
                accounts.put(playerId, account);
                loading.remove(playerId);
                Bukkit.getScheduler().runTask(plugin, () -> callback.accept(account));
            } catch (RuntimeException e) {
                loading.remove(playerId);
                plugin.getLogger().log(java.util.logging.Level.SEVERE,
                        "Não foi possível carregar a conta de caixas " + playerId + ".", e);
                Bukkit.getScheduler().runTask(plugin, onFailure);
            }
        });
    }

    public void unload(UUID playerId) {
        accounts.remove(playerId);
    }

    public void grantKeys(UUID playerId, CrateType type, long amount, Consumer<Boolean> callback) {
        runRepository(() -> {
            repository.grantKeys(playerId, type, amount);
            return true;
        }, callback, playerId);
    }

    public void setKeys(UUID playerId, CrateType type, long amount, Consumer<Boolean> callback) {
        runRepository(() -> {
            repository.setKeys(playerId, type, amount);
            return true;
        }, callback, playerId);
    }

    public void transferKeys(UUID from, UUID to, CrateType type, long amount, Consumer<Boolean> callback) {
        if (from.equals(to)) {
            callback.accept(false);
            return;
        }
        runRepository(() -> repository.transferKeys(from, to, type, amount), callback, from, to);
    }

    public void forgeKeys(UUID playerId, CrateType fromType, CrateType toType,
                          long cost, Consumer<Boolean> callback) {
        runRepository(() -> repository.forgeKeys(playerId, fromType, toType, cost), callback, playerId);
    }

    public void reserveOpening(UUID playerId, CrateType type, int amount, Consumer<Optional<UUID>> callback) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                Optional<UUID> opening = repository.reserveOpening(playerId, type, amount);
                if (opening.isPresent()) refreshAccounts(playerId);
                Bukkit.getScheduler().runTask(plugin, () -> callback.accept(opening));
            } catch (RuntimeException e) {
                plugin.getLogger().log(java.util.logging.Level.SEVERE, "Não foi possível reservar as chaves.", e);
                Bukkit.getScheduler().runTask(plugin, () -> callback.accept(Optional.empty()));
            }
        });
    }

    public void completeOpening(UUID playerId, UUID openingId, CrateType type, int amount,
                                int pityUnitsPerCrate, Consumer<List<Boolean>> callback, Runnable onFailure) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            List<Boolean> guarantees;
            try {
                guarantees = repository.completeOpening(playerId, openingId,
                        catalog.getPityThreshold(), pityUnitsPerCrate);
            } catch (RuntimeException e) {
                plugin.getLogger().log(java.util.logging.Level.SEVERE, "Não foi possível concluir a abertura.", e);
                try {
                    repository.cancelOpening(playerId, openingId);
                } catch (RuntimeException refundError) {
                    plugin.getLogger().log(java.util.logging.Level.SEVERE,
                            "Falha ao devolver chaves após erro ao concluir abertura.", refundError);
                }
                Bukkit.getScheduler().runTask(plugin, onFailure);
                return;
            }
            try {
                CrateAccount refreshed = repository.loadAccount(playerId);
                accounts.put(playerId, refreshed);
            } catch (RuntimeException e) {
                plugin.getLogger().log(java.util.logging.Level.WARNING,
                        "A abertura foi registada, mas a conta não pôde ser atualizada em cache.", e);
            }
            List<Boolean> result = guarantees;
            Bukkit.getScheduler().runTask(plugin, () -> callback.accept(result));
        });
    }

    public void cancelOpening(UUID playerId, UUID openingId) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                repository.cancelOpening(playerId, openingId);
                refreshAccounts(playerId);
            } catch (RuntimeException e) {
                plugin.getLogger().log(java.util.logging.Level.SEVERE,
                        "Falha ao devolver chaves de abertura cancelada para " + playerId + ".", e);
                Player player = Bukkit.getPlayer(playerId);
                if (player != null) player.sendMessage("§cNão foi possível devolver as chaves agora. A equipa foi notificada.");
            }
        });
    }

    public void refreshPhysicalCrates(Consumer<List<br.com.tw.caixas.data.PhysicalCrate>> callback) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                List<br.com.tw.caixas.data.PhysicalCrate> crates = repository.loadPhysicalCrates();
                Bukkit.getScheduler().runTask(plugin, () -> callback.accept(crates));
            } catch (RuntimeException e) {
                plugin.getLogger().log(java.util.logging.Level.SEVERE, "Falha ao carregar caixas físicas.", e);
                Bukkit.getScheduler().runTask(plugin, () -> callback.accept(java.util.Collections.emptyList()));
            }
        });
    }

    public void addPhysicalCrate(String world, int x, int y, int z, CrateType type, Consumer<Boolean> callback) {
        runRepository(() -> {
            repository.addPhysicalCrate(world, x, y, z, type);
            return true;
        }, callback);
    }

    public void removePhysicalCrate(String world, int x, int y, int z, Consumer<Boolean> callback) {
        runRepository(() -> repository.removePhysicalCrate(world, x, y, z), callback);
    }

    public void storeMail(UUID playerId, List<ItemStack> items) {
        if (items == null || items.isEmpty()) return;
        List<ItemStack> copies = new java.util.ArrayList<>();
        for (ItemStack item : items) copies.add(item.clone());
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                repository.storeMail(playerId, copies);
                Bukkit.getScheduler().runTask(plugin, () -> {
                    Player player = Bukkit.getPlayer(playerId);
                    if (player != null) player.sendMessage("§eAlguns itens foram guardados. Usa §f/key correio §epara os recolher.");
                });
            } catch (RuntimeException e) {
                plugin.getLogger().log(java.util.logging.Level.SEVERE,
                        "Não foi possível guardar os itens excedentes de " + playerId + ".", e);
                Bukkit.getScheduler().runTask(plugin, () -> {
                    Player player = Bukkit.getPlayer(playerId);
                    if (player != null) player.sendMessage("§cFalha ao guardar itens excedentes; contacta a equipa.");
                });
            }
        });
    }

    public void storeMail(UUID playerId, ItemStack item) {
        storeMail(playerId, java.util.Collections.singletonList(item));
    }

    public void claimMail(UUID playerId, Consumer<List<ItemStack>> callback, Runnable onFailure) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                List<ItemStack> items = repository.claimMail(playerId);
                Bukkit.getScheduler().runTask(plugin, () -> callback.accept(items));
            } catch (RuntimeException e) {
                plugin.getLogger().log(java.util.logging.Level.SEVERE,
                        "Não foi possível recolher o correio de " + playerId + ".", e);
                Bukkit.getScheduler().runTask(plugin, onFailure);
            }
        });
    }

    private void runRepository(RepositoryOperation operation, Consumer<Boolean> callback, UUID... refresh) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            boolean result;
            try {
                result = operation.run();
                if (result) refreshAccounts(refresh);
            } catch (RuntimeException e) {
                plugin.getLogger().log(java.util.logging.Level.SEVERE, "Operação do sistema de caixas falhou.", e);
                result = false;
            }
            boolean finalResult = result;
            Bukkit.getScheduler().runTask(plugin, () -> callback.accept(finalResult));
        });
    }

    private void refreshAccounts(UUID... playerIds) {
        for (UUID playerId : playerIds) {
            Player player = Bukkit.getPlayer(playerId);
            if (player == null || !player.isOnline()) continue;
            try {
                accounts.put(playerId, repository.loadAccount(playerId));
            } catch (RuntimeException e) {
                plugin.getLogger().log(java.util.logging.Level.SEVERE,
                        "Não foi possível atualizar a conta de caixas " + playerId + ".", e);
            }
        }
    }

    private interface RepositoryOperation {
        boolean run();
    }
}
