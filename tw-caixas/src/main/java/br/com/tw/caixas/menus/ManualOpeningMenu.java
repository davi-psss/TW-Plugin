package br.com.tw.caixas.menus;

import br.com.tw.caixas.TWCaixasPlugin;
import br.com.tw.caixas.managers.CrateOpenings;
import br.com.tw.caixas.managers.CrateService;
import br.com.tw.caixas.model.CrateReward;
import br.com.tw.caixas.model.CrateType;
import br.com.tw.core.menus.TWMenu;
import br.com.tw.core.utils.TWMenuFrame;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitTask;

import java.util.Arrays;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

public final class ManualOpeningMenu implements TWMenu {
    private static final int REWARD_SLOT = 13;

    private final TWCaixasPlugin plugin;
    private final CrateService service;
    private final CrateOpenings openings;
    private final UUID playerId;
    private final UUID openingId;
    private final CrateType type;
    private final Runnable onCancelled;
    private final Inventory inventory;
    private final BukkitTask animationTask;
    private boolean completing;
    private int elapsedTicks;

    public ManualOpeningMenu(TWCaixasPlugin plugin, CrateService service, CrateOpenings openings,
                             UUID playerId, UUID openingId, CrateType type,
                             Runnable onCancelled) {
        this.plugin = plugin;
        this.service = service;
        this.openings = openings;
        this.playerId = playerId;
        this.openingId = openingId;
        this.type = type;
        this.onCancelled = onCancelled;
        inventory = Bukkit.createInventory(this, 27, "§8Caixa: " + service.getCatalog().get(type).getDisplayName());
        fillBackground();
        inventory.setItem(REWARD_SLOT, rollingIcon());
        Player player = Bukkit.getPlayer(playerId);
        if (player != null) {
            player.sendMessage("§eA caixa está a sortear uma recompensa...");
        }
        animationTask = Bukkit.getScheduler().runTaskTimer(plugin, this::tickAnimation, 0L, 3L);
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }

    @Override
    public void onClick(InventoryClickEvent event) {
        event.setCancelled(true);
    }

    public void onClose() {
        if (completing) return;
        completing = true;
        animationTask.cancel();
        service.cancelOpening(playerId, openingId);
        onCancelled.run();
        Player player = Bukkit.getPlayer(playerId);
        if (player != null) player.sendMessage("§7A abertura foi cancelada e a chave foi devolvida.");
    }

    private void tickAnimation() {
        Player player = Bukkit.getPlayer(playerId);
        if (player == null || !player.isOnline()) {
            onClose();
            return;
        }
        elapsedTicks += 3;
        if (elapsedTicks >= service.getCatalog().getManualSeconds() * 20) {
            complete(player);
            return;
        }

        CrateReward preview = service.getCatalog().get(type).draw();
        inventory.setItem(REWARD_SLOT, CrateMenuItems.rewardIcon(preview, preview.rollAmount(), false));
        player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.3F, 1.2F + ThreadLocalRandom.current().nextFloat());
    }

    private void complete(Player player) {
        if (completing) return;
        completing = true;
        animationTask.cancel();
        openings.completeManual(playerId, openingId, type, player, grant -> {
            inventory.setItem(REWARD_SLOT, CrateMenuItems.rewardIcon(
                    grant.getReward(), grant.getAmount(), grant.isCritical()));
            player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0F, 1.2F);
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                if (player.isOnline()
                        && player.getOpenInventory().getTopInventory().getHolder() == this) {
                    player.closeInventory();
                }
            }, 40L);
        }, onCancelled);
    }

    private ItemStack rollingIcon() {
        ItemStack item = new ItemStack(Material.HOPPER);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName("§e§lA sortear...");
        meta.setLore(Arrays.asList("§7Uma caixa,", "§7uma recompensa."));
        item.setItemMeta(meta);
        return item;
    }

    private void fillBackground() {
        for (int slot = 0; slot < inventory.getSize(); slot++) {
            if (slot == REWARD_SLOT) continue;
            ItemStack pane = TWMenuFrame.stainedGlassPane(slot % 2 == 0 ? 15 : 7);
            inventory.setItem(slot, pane);
        }
    }
}
