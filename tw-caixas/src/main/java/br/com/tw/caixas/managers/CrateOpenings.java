package br.com.tw.caixas.managers;

import br.com.tw.caixas.TWCaixasPlugin;
import br.com.tw.caixas.config.CrateCatalog;
import br.com.tw.caixas.model.CrateDefinition;
import br.com.tw.caixas.model.CrateReward;
import br.com.tw.caixas.model.CrateType;
import br.com.tw.caixas.model.RewardGrant;
import br.com.tw.caixas.menus.ManualOpeningMenu;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

public final class CrateOpenings {
    private final TWCaixasPlugin plugin;
    private final CrateService service;
    private final RewardDelivery delivery;
    private final Set<UUID> activePlayers = ConcurrentHashMap.newKeySet();

    public CrateOpenings(TWCaixasPlugin plugin, CrateService service, RewardDelivery delivery) {
        this.plugin = plugin;
        this.service = service;
        this.delivery = delivery;
    }

    public void openManual(Player player, CrateType type) {
        if (!begin(player)) return;
        service.reserveOpening(player.getUniqueId(), type, 1, opening -> {
            if (!opening.isPresent()) {
                activePlayers.remove(player.getUniqueId());
                player.sendMessage("§cNão tens chaves suficientes ou não foi possível iniciar a abertura.");
                return;
            }
            if (!player.isOnline()) {
                service.cancelOpening(player.getUniqueId(), opening.get());
                activePlayers.remove(player.getUniqueId());
                return;
            }
            try {
                ManualOpeningMenu menu = new ManualOpeningMenu(plugin, service, this,
                        player.getUniqueId(), opening.get(), type,
                        () -> activePlayers.remove(player.getUniqueId()));
                player.openInventory(menu.getInventory());
            } catch (RuntimeException e) {
                plugin.getLogger().log(java.util.logging.Level.SEVERE, "Não foi possível abrir as cartas.", e);
                service.cancelOpening(player.getUniqueId(), opening.get());
                activePlayers.remove(player.getUniqueId());
                player.sendMessage("§cNão foi possível abrir as cartas; a chave será devolvida.");
            }
        });
    }

    public void openBulk(Player player, CrateType type, int amount) {
        if (!begin(player)) return;
        CrateCatalog catalog = service.getCatalog();
        if (amount < 1 || amount > catalog.getBulkLimit()) {
            activePlayers.remove(player.getUniqueId());
            player.sendMessage("§cA quantidade máxima por abertura é " + catalog.getBulkLimit() + ".");
            return;
        }
        service.reserveOpening(player.getUniqueId(), type, amount, opening -> {
            if (!opening.isPresent()) {
                activePlayers.remove(player.getUniqueId());
                player.sendMessage("§cNão tens chaves suficientes para essa abertura.");
                return;
            }
            if (!player.isOnline()) {
                service.cancelOpening(player.getUniqueId(), opening.get());
                activePlayers.remove(player.getUniqueId());
                return;
            }
            service.completeOpening(player.getUniqueId(), opening.get(), type, amount,
                    catalog.getBulkPityUnits(), guarantees -> {
                        try {
                            boolean divineSeal = hasDivineSeal(player);
                            List<RewardGrant> grants = rollBulk(type, amount, guarantees, divineSeal);
                            List<String> report = delivery.deliver(player, grants);
                            if (divineSeal) consumeDivineSeal(player);
                            player.sendMessage("§aAbriste §e" + amount + " §acaixas " + catalog.get(type).getDisplayName()
                                    + "§a. Cada caixa sorteou uma recompensa; pity avançou à metade.");
                            sendReport(player, report);
                        } catch (RuntimeException e) {
                            plugin.getLogger().log(java.util.logging.Level.SEVERE,
                                    "Falha ao entregar recompensas de caixa para " + player.getName() + ".", e);
                            player.sendMessage("§cHouve um erro ao entregar os prémios. Contacta a equipa.");
                        } finally {
                            activePlayers.remove(player.getUniqueId());
                        }
                    }, () -> {
                        activePlayers.remove(player.getUniqueId());
                        player.sendMessage("§cNão foi possível concluir a abertura. Confirma o teu saldo; contacta a equipa se faltarem chaves.");
                    });
        });
    }

    RewardGrant finishManual(CrateType type, List<Boolean> guarantees, Player player) {
        if (guarantees.size() != 1) throw new IllegalStateException("Resultado de pity manual inválido.");
        boolean divineSeal = hasDivineSeal(player);
        CrateDefinition definition = service.getCatalog().get(type);
        CrateReward reward = guarantees.get(0) || divineSeal
                ? definition.drawLegendary() : definition.draw();
        RewardGrant grant = new RewardGrant(reward, reward.rollAmount(), false);
        List<String> report = delivery.deliver(player, java.util.Collections.singletonList(grant));
        if (divineSeal) consumeDivineSeal(player);
        player.sendMessage("§aAbertura concluída: §e1 §arecompensa sorteada.");
        for (String line : report) player.sendMessage(" §8• " + line);
        return grant;
    }

    public void completeManual(UUID playerId, UUID openingId, CrateType type, Player player,
                               Consumer<RewardGrant> onReveal, Runnable onDone) {
        service.completeOpening(playerId, openingId, type, 1,
                PityProgressUnits.MANUAL, guarantees -> {
                    try {
                        RewardGrant grant = finishManual(type, guarantees, player);
                        onReveal.accept(grant);
                    } catch (RuntimeException e) {
                        plugin.getLogger().log(java.util.logging.Level.SEVERE,
                                "Falha ao entregar abertura manual para " + player.getName() + ".", e);
                        player.sendMessage("§cHouve um erro ao entregar os prémios. Contacta a equipa.");
                    } finally {
                        activePlayers.remove(playerId);
                        onDone.run();
                    }
                }, () -> {
                    player.sendMessage("§cNão foi possível concluir a abertura. Confirma o teu saldo; contacta a equipa se faltarem chaves.");
                    activePlayers.remove(playerId);
                    onDone.run();
                });
    }

    void release(UUID playerId) {
        activePlayers.remove(playerId);
    }

    private boolean begin(Player player) {
        if (activePlayers.add(player.getUniqueId())) return true;
        player.sendMessage("§cJá tens uma abertura de caixa em andamento.");
        return false;
    }

    private List<RewardGrant> rollBulk(CrateType type, int crateCount, List<Boolean> guarantees,
                                       boolean divineSeal) {
        return rollBulkRewards(service.getCatalog().get(type), crateCount, guarantees, divineSeal);
    }

    static List<RewardGrant> rollBulkRewards(CrateDefinition definition, int crateCount,
                                              List<Boolean> guarantees, boolean divineSeal) {
        if (guarantees.size() != crateCount) throw new IllegalStateException("Resultado de pity em massa inválido.");
        Map<CrateReward, Long> totals = new IdentityHashMap<>();
        for (int crateIndex = 0; crateIndex < guarantees.size(); crateIndex++) {
            boolean guaranteed = guarantees.get(crateIndex) || (divineSeal && crateIndex == 0);
            CrateReward reward = guaranteed ? definition.drawLegendary() : definition.draw();
            totals.merge(reward, reward.rollAmount(), Math::addExact);
        }
        List<RewardGrant> grants = new ArrayList<>();
        for (Map.Entry<CrateReward, Long> entry : totals.entrySet()) {
            grants.add(new RewardGrant(entry.getKey(), entry.getValue(), false));
        }
        return grants;
    }

    private boolean hasDivineSeal(Player player) {
        for (org.bukkit.inventory.ItemStack item : player.getInventory().getContents()) {
            if ("divine_seal".equals(br.com.tw.core.utils.TWItemTags.getString(item, "tw_crate_effect"))
                    && br.com.tw.core.utils.TWItemTags.getLong(item, "tw_crate_charges", 0L) > 0L) {
                return true;
            }
        }
        return false;
    }

    private void consumeDivineSeal(Player player) {
        org.bukkit.inventory.ItemStack[] contents = player.getInventory().getContents();
        for (int slot = 0; slot < contents.length; slot++) {
            org.bukkit.inventory.ItemStack item = contents[slot];
            if ("divine_seal".equals(br.com.tw.core.utils.TWItemTags.getString(item, "tw_crate_effect"))
                    && br.com.tw.core.utils.TWItemTags.getLong(item, "tw_crate_charges", 0L) > 0L) {
                if (item.getAmount() <= 1) player.getInventory().setItem(slot, null);
                else {
                    item.setAmount(item.getAmount() - 1);
                    player.getInventory().setItem(slot, item);
                }
                player.updateInventory();
                player.sendMessage("§dO Selo da Fortuna Divina garantiu uma recompensa lendária.");
                return;
            }
        }
    }

    private void sendReport(Player player, List<String> report) {
        int maxLines = Math.min(8, report.size());
        for (int i = 0; i < maxLines; i++) player.sendMessage(" §8• " + report.get(i));
        if (report.size() > maxLines) {
            player.sendMessage(" §7... e mais " + (report.size() - maxLines) + " tipos de prémio.");
        }
    }

    private static final class PityProgressUnits {
        private static final int MANUAL = 1000;
    }
}
