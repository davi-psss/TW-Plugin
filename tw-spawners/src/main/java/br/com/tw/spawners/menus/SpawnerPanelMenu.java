package br.com.tw.spawners.menus;

import br.com.tw.core.TWCorePlugin;
import br.com.tw.core.data.TWUser;
import br.com.tw.core.menus.TWMenu;
import br.com.tw.core.utils.TWFormat;
import br.com.tw.core.utils.TWMenuFrame;
import br.com.tw.economia.TWEconomiaPlugin;
import br.com.tw.spawners.TWSpawnersPlugin;
import br.com.tw.spawners.managers.SpawnerManager;
import br.com.tw.spawners.models.PlacedSpawner;
import br.com.tw.spawners.models.SpawnerType;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Arrays;

public class SpawnerPanelMenu implements TWMenu {

    private final Inventory inventory;
    private final Player player;
    private final PlacedSpawner spawner;
    private final SpawnerType type;
    private final SpawnerManager manager;

    public SpawnerPanelMenu(Player player, PlacedSpawner spawner) {
        this.player = player;
        this.spawner = spawner;
        this.manager = TWSpawnersPlugin.getInstance().getSpawnerManager();
        this.type = manager.getSpawnerTypes().get(spawner.getTypeId());

        this.inventory = Bukkit.createInventory(this, 54, ChatColor.DARK_GRAY + "Painel do Spawner");
        setupItems();
    }

    private void setupItems() {
        for (int i = 0; i < 54; i++) {
            ItemStack glass = new ItemStack(Material.STAINED_GLASS_PANE, 1, (short) 15);
            ItemMeta gMeta = glass.getItemMeta();
            gMeta.setDisplayName(" ");
            glass.setItemMeta(gMeta);
            inventory.setItem(i, glass);
        }
        TWMenuFrame.apply(inventory);

        if (type == null) return;
        TWUser user = TWCorePlugin.getInstance().getUserManager().getUser(player.getUniqueId());
        if (user == null) return;

        double prestigeMult = manager.getPrestigeMultiplier(user.getPrestigeId());
        double globalBooster = TWEconomiaPlugin.getInstance().getGlobalBooster();
        double currentPayoutMoney = spawner.getStoredMoney() * prestigeMult * globalBooster;

        ItemStack info = new ItemStack(Material.MOB_SPAWNER);
        ItemMeta iMeta = info.getItemMeta();
        iMeta.setDisplayName(type.getDisplayName());
        iMeta.setLore(Arrays.asList(
                "§7Dono: §f" + spawner.getOwnerName(),
                "§7Entidade: §a" + type.getEntityType().name(),
                "§7Stack Acumulado: §e" + spawner.getStackAmount() + "x",
                "§7Abates Históricos: §c" + TWFormat.formatarDinheiro(spawner.getTotalKills())
        ));
        info.setItemMeta(iMeta);
        inventory.setItem(13, info);

        ItemStack mode = new ItemStack(Material.REDSTONE_COMPARATOR);
        ItemMeta mMeta = mode.getItemMeta();
        boolean isVirtual = spawner.isVirtualMode();
        mMeta.setDisplayName("§e§lAlternar Modo");
        mMeta.setLore(Arrays.asList(
                "§7Modo Atual: " + (isVirtual ? "§d§lVIRTUAL (AUTO-KILL)" : "§a§lFÍSICO (SPAWN NORMAL)"),
                "",
                "§7No Modo Virtual, os monstros não nascem.",
                "§7O spawner acumula recursos no Cofre.",
                "",
                "§eClica para alternar o modo!"
        ));
        mode.setItemMeta(mMeta);
        inventory.setItem(11, mode);

        ItemStack cofre = new ItemStack(Material.GOLD_INGOT);
        ItemMeta cMeta = cofre.getItemMeta();
        cMeta.setDisplayName("§a§lCofre do Spawner");
        cMeta.setLore(Arrays.asList(
                "§7Dinheiro a Receber: §2§l$" + TWFormat.formatarDinheiro(currentPayoutMoney),
                "§7Tokens a Receber: §b§l" + TWFormat.formatarDinheiro(spawner.getStoredTokens()),
                "§7XP Oculto: §a" + TWFormat.formatarDinheiro(spawner.getStoredXp()) + " XP",
                "§7Almas Ocultas: §d" + spawner.getStoredSouls() + "x",
                "",
                "§eClica para depositar tudo na tua conta!"
        ));
        cofre.setItemMeta(cMeta);
        inventory.setItem(15, cofre);

        // UPGRADES
        long speedCost = spawner.getSpeedLevel() * 1500L;
        ItemStack speedUp = new ItemStack(Material.SUGAR);
        ItemMeta sMeta = speedUp.getItemMeta();
        sMeta.setDisplayName("§b§lUpgrade: Velocidade");
        sMeta.setLore(Arrays.asList(
                "§7Reduz o tempo de spawn em §a0.5s§7.",
                "§7Nível Atual: §f" + spawner.getSpeedLevel(),
                "§7Custo: §b" + TWFormat.formatarDinheiro(speedCost) + " Tokens",
                "",
                "§eClica para melhorar!"
        ));
        speedUp.setItemMeta(sMeta);
        inventory.setItem(29, speedUp);

        long lootCost = spawner.getLootLevel() * 2000L;
        ItemStack lootUp = new ItemStack(Material.EMERALD);
        ItemMeta lMeta = lootUp.getItemMeta();
        lMeta.setDisplayName("§a§lUpgrade: Fortuna (Loot)");
        lMeta.setLore(Arrays.asList(
                "§7Aumenta o dinheiro por abate em §a+10%§7",
                "§7e a chance de tokens em §b+5%§7 por nível.",
                "§7Nível Atual: §f" + spawner.getLootLevel(),
                "§7Custo: §b" + TWFormat.formatarDinheiro(lootCost) + " Tokens",
                "",
                "§eClica para melhorar!"
        ));
        lootUp.setItemMeta(lMeta);
        inventory.setItem(31, lootUp);

        long amountCost = spawner.getAmountLevel() * 2500L;
        ItemStack amountUp = new ItemStack(Material.EGG);
        ItemMeta aMeta = amountUp.getItemMeta();
        aMeta.setDisplayName("§6§lUpgrade: Quantidade (+1)");
        aMeta.setLore(Arrays.asList(
                "§7Multiplica a quantidade de monstros",
                "§7que nascem por cada ciclo de spawn.",
                "§7Nível Atual: §f" + spawner.getAmountLevel(),
                "§7Custo: §b" + TWFormat.formatarDinheiro(amountCost) + " Tokens",
                "",
                "§eClica para melhorar!"
        ));
        amountUp.setItemMeta(aMeta);
        inventory.setItem(33, amountUp);

        // AUTO SELL UPGRADE
        ItemStack autoSell = new ItemStack(Material.HOPPER);
        ItemMeta asMeta = autoSell.getItemMeta();
        asMeta.setDisplayName("§b§lMódulo: Auto-Sell");
        if (spawner.isAutoSell()) {
            asMeta.setLore(Arrays.asList("§7O Auto-Sell está §a§lATIVO§7.", "§7(Dinheiro e Tokens são vendidos automaticamente)"));
        } else {
            asMeta.setLore(Arrays.asList(
                    "§7O spawner deposita Dinheiro e Tokens",
                    "§7diretamente na tua conta a cada 10s.",
                    "",
                    "§7Custo Único: §b150,000 Tokens",
                    "§eClica para ativar!"
            ));
        }
        autoSell.setItemMeta(asMeta);
        inventory.setItem(40, autoSell);

        // DESMONTAR
        ItemStack pickup = new ItemStack(Material.TNT);
        ItemMeta pMeta = pickup.getItemMeta();
        pMeta.setDisplayName("§c§lDesmontar Spawner");
        pMeta.setLore(Arrays.asList("§7Guarda o spawner no teu inventário.", "§4Clica para desmontar."));
        pickup.setItemMeta(pMeta);
        inventory.setItem(49, pickup);
    }

    @Override
    public void onClick(InventoryClickEvent event) {
        if (!spawner.getOwnerUuid().equals(player.getUniqueId()) && !player.hasPermission("tw.admin")) {
            player.playSound(player.getLocation(), Sound.VILLAGER_NO, 1f, 1f);
            player.sendMessage(ChatColor.RED + "Apenas o proprietário pode gerir este spawner!");
            return;
        }

        TWUser user = TWCorePlugin.getInstance().getUserManager().getUser(player.getUniqueId());
        if (user == null) return;

        int slot = event.getSlot();

        if (slot == 11) {
            spawner.setVirtualMode(!spawner.isVirtualMode());
            manager.saveSpawnerAsync(spawner);
            TWSpawnersPlugin.getInstance().getHologramManager().updateOrCreateHologram(spawner, type);
            player.playSound(player.getLocation(), Sound.CLICK, 1f, 1f);
            setupItems();
        }
        else if (slot == 15) {
            if (spawner.getStoredMoney() <= 0 && spawner.getStoredTokens() <= 0 && spawner.getStoredXp() <= 0 && spawner.getStoredSouls() <= 0) return;

            double prestigeMult = manager.getPrestigeMultiplier(user.getPrestigeId());
            double globalBooster = TWEconomiaPlugin.getInstance().getGlobalBooster();
            double finalPayout = spawner.getStoredMoney() * prestigeMult * globalBooster;
            long finalTokens = spawner.getStoredTokens();
            long finalXp = spawner.getStoredXp();
            long finalSouls = spawner.getStoredSouls();

            spawner.setStoredMoney(0); spawner.setStoredTokens(0); spawner.setStoredXp(0); spawner.setStoredSouls(0);
            manager.saveSpawnerAsync(spawner);
            TWSpawnersPlugin.getInstance().getHologramManager().updateOrCreateHologram(spawner, type);

            user.addBalance(finalPayout);
            user.addTokens(finalTokens);
            TWCorePlugin.getInstance().getUserManager().saveUserAsync(user);

            if (finalXp > 0) player.giveExp((int) finalXp);
            if (finalSouls > 0) {
                if (player.getInventory().firstEmpty() != -1) {
                    player.getInventory().addItem(manager.createSoulItem(finalSouls));
                } else {
                    player.getWorld().dropItem(player.getLocation(), manager.createSoulItem(finalSouls));
                }
            }

            player.playSound(player.getLocation(), Sound.LEVEL_UP, 1f, 1.5f);
            player.sendMessage("§a✔ Cofre Resgatado! §2$" + TWFormat.formatarDinheiro(finalPayout) + " §8| §b" + TWFormat.formatarDinheiro(finalTokens) + " T §8| §a" + finalXp + " XP §8| §d" + finalSouls + " Almas");
            setupItems();
        }
        else if (slot == 29) {
            long cost = spawner.getSpeedLevel() * 1500L;
            if (user.removeTokens(cost)) { spawner.upgradeSpeedLevel(); processUpgrade(user); } else sendTokenError(cost);
        }
        else if (slot == 31) {
            long cost = spawner.getLootLevel() * 2000L;
            if (user.removeTokens(cost)) { spawner.upgradeLootLevel(); processUpgrade(user); } else sendTokenError(cost);
        }
        else if (slot == 33) {
            long cost = spawner.getAmountLevel() * 2500L;
            if (user.removeTokens(cost)) { spawner.upgradeAmountLevel(); processUpgrade(user); } else sendTokenError(cost);
        }
        else if (slot == 40 && !spawner.isAutoSell()) {
            if (user.removeTokens(150000L)) {
                spawner.setAutoSell(true);
                processUpgrade(user);
            } else sendTokenError(150000L);
        }
        else if (slot == 49) {
            if (player.getInventory().firstEmpty() == -1) {
                player.playSound(player.getLocation(), Sound.VILLAGER_NO, 1f, 1f);
                player.sendMessage(ChatColor.RED + "Inventário cheio!");
                return;
            }
            player.closeInventory();

            // AQUI ESTÁ O SEGREDO: Usamos o método que passa o 'spawner' para salvar os upgrades
            player.getInventory().addItem(manager.createSpawnerItem(spawner, type));

            TWSpawnersPlugin.getInstance().getHologramManager().removeHologram(spawner.getLocationKey());
            org.bukkit.Location loc = spawner.getBukkitLocation();
            if (loc != null) loc.getBlock().setType(Material.AIR);

            manager.removeSpawner(loc);

            player.playSound(player.getLocation(), Sound.ITEM_PICKUP, 1f, 1f);
            player.sendMessage("§eDesmontaste o spawner com sucesso! §aOs teus upgrades foram salvos no item.");
        }
    }

    private void processUpgrade(TWUser user) {
        manager.saveSpawnerAsync(spawner);
        TWSpawnersPlugin.getInstance().getHologramManager().updateOrCreateHologram(spawner, type);
        TWCorePlugin.getInstance().getUserManager().saveUserAsync(user);
        player.playSound(player.getLocation(), Sound.ANVIL_USE, 1f, 1.5f);
        setupItems();
    }

    private void sendTokenError(long cost) {
        player.playSound(player.getLocation(), Sound.VILLAGER_NO, 1f, 1f);
        player.sendMessage(ChatColor.RED + "Não tens Tokens suficientes! Custo: " + TWFormat.formatarDinheiro(cost));
    }

    @Override
    public Inventory getInventory() { return inventory; }

    public void open() { player.openInventory(inventory); }
}