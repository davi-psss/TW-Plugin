package br.com.tw.maquinas.menus;

import br.com.tw.core.TWCorePlugin;
import br.com.tw.core.data.TWUser;
import br.com.tw.core.menus.TWMenu;
import br.com.tw.core.utils.TWFormat;
import br.com.tw.core.utils.TWMenuFrame;
import br.com.tw.economia.TWEconomiaPlugin;
import br.com.tw.maquinas.TWMaquinasPlugin;
import br.com.tw.maquinas.managers.MachineManager;
import br.com.tw.maquinas.models.MachineType;
import br.com.tw.maquinas.models.PlacedMachine;
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

public class MachinePanelMenu implements TWMenu {

    private final Inventory inventory;
    private final Player player;
    private final PlacedMachine machine;
    private final MachineType type;
    private final MachineManager manager;

    public MachinePanelMenu(Player player, PlacedMachine machine) {
        this.player = player;
        this.machine = machine;
        this.manager = TWMaquinasPlugin.getInstance().getMachineManager();
        this.type = manager.getMachineTypes().get(machine.getTypeId());

        this.inventory = Bukkit.createInventory(this, 45, ChatColor.DARK_GRAY + "Painel da Máquina");
        setupItems();
    }

    private void setupItems() {
        for (int i = 0; i < 45; i++) {
            ItemStack glass = TWMenuFrame.stainedGlassPane(15);
            inventory.setItem(i, glass);
        }
        TWMenuFrame.apply(inventory);

        if (type == null) return;

        TWUser user = TWCorePlugin.getInstance().getUserManager().getUser(player.getUniqueId());
        if (user == null) return;

        double maxStorage = (type.getValuePerCycle() * machine.getStackAmount()) * (10 * machine.getStorageLevel());
        double prestigeMult = manager.getPrestigeMultiplier(user.getPrestigeId());
        double globalBooster = TWEconomiaPlugin.getInstance().getGlobalBooster();
        double currentPayout = machine.getStoredEarnings() * prestigeMult * globalBooster;

        ItemStack info = new ItemStack(type.getMaterial());
        ItemMeta iMeta = info.getItemMeta();
        iMeta.setDisplayName(type.getDisplayName());
        iMeta.setLore(Arrays.asList(
                "§7Dono: §f" + machine.getOwnerName(),
                "§7Stack Agrupado: §e" + machine.getStackAmount() + "x",
                "§7Combustível Restante: §c" + machine.getFuel() + " ciclos",
                "§7Armazenamento: §a" + TWFormat.formatarDinheiro(machine.getStoredEarnings()) + " §8/ §2" + TWFormat.formatarDinheiro(maxStorage),
                "§7Total Gerado (Histórico): §6$" + TWFormat.formatarDinheiro(machine.getTotalGenerated())
        ));
        info.setItemMeta(iMeta);
        inventory.setItem(13, info);

        long speedCost = machine.getSpeedLevel() * 1000L;
        ItemStack speedUp = new ItemStack(Material.SUGAR);
        ItemMeta sMeta = speedUp.getItemMeta();
        sMeta.setDisplayName("§b§lUpgrade: Velocidade");
        sMeta.setLore(Arrays.asList(
                "§7Reduz o tempo de cada ciclo em §a0.5s§7.",
                "§7Nível Atual: §f" + machine.getSpeedLevel(),
                "§7Custo: §b" + TWFormat.formatarDinheiro(speedCost) + " Tokens",
                "",
                "§eClica para evoluir a Velocidade!"
        ));
        speedUp.setItemMeta(sMeta);
        inventory.setItem(29, speedUp);

        long storageCost = machine.getStorageLevel() * 800L;
        ItemStack storageUp = new ItemStack(Material.CHEST);
        ItemMeta stMeta = storageUp.getItemMeta();
        stMeta.setDisplayName("§6§lUpgrade: Armazenamento");
        stMeta.setLore(Arrays.asList(
                "§7Aumenta o limite de lucro acumulado.",
                "§7Nível Atual: §f" + machine.getStorageLevel(),
                "§7Custo: §b" + TWFormat.formatarDinheiro(storageCost) + " Tokens",
                "",
                "§eClica para evoluir o Armazenamento!"
        ));
        storageUp.setItemMeta(stMeta);
        inventory.setItem(31, storageUp);

        long fuelEffCost = machine.getFuelEfficiencyLevel() * 500L;
        ItemStack fuelUp = new ItemStack(Material.COAL);
        ItemMeta fMeta = fuelUp.getItemMeta();
        fMeta.setDisplayName("§8§lUpgrade: Eficiência de Combustível");
        fMeta.setLore(Arrays.asList(
                "§7Garante §a+5%§7 de chance por nível",
                "§7de não gastar combustível num ciclo.",
                "§7Nível Atual: §f" + machine.getFuelEfficiencyLevel(),
                "§7Custo: §b" + TWFormat.formatarDinheiro(fuelEffCost) + " Tokens",
                "",
                "§eClica para evoluir o Combustível!"
        ));
        fuelUp.setItemMeta(fMeta);
        inventory.setItem(33, fuelUp);

        ItemStack collect = new ItemStack(Material.GOLD_INGOT);
        ItemMeta cMeta = collect.getItemMeta();
        cMeta.setDisplayName("§a§lRecolher Produção");
        cMeta.setLore(Arrays.asList(
                "§7Valor Acumulado Base: §a$" + TWFormat.formatarDinheiro(machine.getStoredEarnings()),
                "§7Bónus Atual (Rank/Global): §e" + (prestigeMult * globalBooster) + "x",
                "§7Total a Receber: §2§l$" + TWFormat.formatarDinheiro(currentPayout),
                "",
                "§eClica para transferir para a tua conta!"
        ));
        collect.setItemMeta(cMeta);
        inventory.setItem(11, collect);

        ItemStack pickup = new ItemStack(Material.HOPPER);
        ItemMeta pMeta = pickup.getItemMeta();
        pMeta.setDisplayName("§c§lDesmontar Máquina");
        pMeta.setLore(Arrays.asList(
                "§7Guarda o bloco da máquina (" + machine.getStackAmount() + "x)",
                "§7de volta no teu inventário.",
                "",
                "§c§lAtenção: §7Recolhe primeiro os lucros,",
                "§7ou serás penalizado ao desmontar!",
                "",
                "§4Clica para recolher."
        ));
        pickup.setItemMeta(pMeta);
        inventory.setItem(40, pickup);
    }

    @Override
    public void onClick(InventoryClickEvent event) {
        if (!machine.getOwnerUuid().equals(player.getUniqueId()) && !player.hasPermission("tw.admin")) {
            player.sendMessage(ChatColor.RED + "Apenas o proprietário pode gerir esta máquina!");
            return;
        }

        TWUser user = TWCorePlugin.getInstance().getUserManager().getUser(player.getUniqueId());
        if (user == null) return;

        int slot = event.getSlot();

        if (slot == 11) {
            double baseStored = machine.getStoredEarnings();
            if (baseStored <= 0) {
                player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
                player.sendMessage(ChatColor.RED + "Esta máquina ainda não tem recursos acumulados.");
                return;
            }

            double prestigeMult = manager.getPrestigeMultiplier(user.getPrestigeId());
            double globalBooster = TWEconomiaPlugin.getInstance().getGlobalBooster();
            double finalPayout = baseStored * prestigeMult * globalBooster;

            machine.setStoredEarnings(0);
            manager.saveMachineAsync(machine);
            TWMaquinasPlugin.getInstance().getHologramManager().updateOrCreateHologram(machine, type);

            user.addBalance(finalPayout);
            TWCorePlugin.getInstance().getUserManager().saveUserAsync(user);

            player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 1.5f);

            try {
                Class<?> actionClass = Class.forName("br.com.tw.minas.utils.ActionBarUtil");
                actionClass.getMethod("send", Player.class, String.class)
                        .invoke(null, player, "§a✔ Recolheste §2$" + TWFormat.formatarDinheiro(finalPayout) + " §ada máquina!");
            } catch (Exception e) {
                player.sendMessage("§a✔ Recolheste §2$" + TWFormat.formatarDinheiro(finalPayout) + " §ada máquina!");
            }

            setupItems();
        }
        else if (slot == 29) {
            long cost = machine.getSpeedLevel() * 1000L;
            if (user.removeTokens(cost)) {
                machine.upgradeSpeedLevel();
                processUpgrade(user);
            } else {
                sendTokenError(cost);
            }
        }
        else if (slot == 31) {
            long cost = machine.getStorageLevel() * 800L;
            if (user.removeTokens(cost)) {
                machine.upgradeStorageLevel();
                processUpgrade(user);
            } else {
                sendTokenError(cost);
            }
        }
        else if (slot == 33) {
            long cost = machine.getFuelEfficiencyLevel() * 500L;
            if (user.removeTokens(cost)) {
                machine.upgradeFuelEfficiencyLevel();
                processUpgrade(user);
            } else {
                sendTokenError(cost);
            }
        }
        else if (slot == 40) {
            if (player.getInventory().firstEmpty() == -1) {
                player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
                player.sendMessage(ChatColor.RED + "O teu inventário está cheio!");
                return;
            }

            player.closeInventory();
            player.getInventory().addItem(manager.createMachineItem(type, machine.getStackAmount()));

            TWMaquinasPlugin.getInstance().getHologramManager().removeHologram(machine.getLocationKey());
            org.bukkit.Location loc = machine.getBukkitLocation();
            if (loc != null) loc.getBlock().setType(Material.AIR);

            manager.removeMachine(loc);

            player.playSound(player.getLocation(), Sound.ENTITY_ITEM_PICKUP, 1f, 1f);
            player.sendMessage(ChatColor.YELLOW + "Desmontaste a máquina com sucesso!");
        }
    }

    private void processUpgrade(TWUser user) {
        manager.saveMachineAsync(machine);
        TWMaquinasPlugin.getInstance().getHologramManager().updateOrCreateHologram(machine, type);
        TWCorePlugin.getInstance().getUserManager().saveUserAsync(user);

        player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_USE, 1f, 1.5f);
        player.sendMessage(ChatColor.GREEN + "Atributo da máquina evoluído com sucesso!");
        setupItems();
    }

    private void sendTokenError(long cost) {
        player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
        player.sendMessage(ChatColor.RED + "Não tens Tokens suficientes! Precisas de " + TWFormat.formatarDinheiro(cost) + " Tokens.");
    }

    @Override
    public Inventory getInventory() { return inventory; }

    public void open() {
        player.openInventory(inventory);
        player.playSound(player.getLocation(), Sound.BLOCK_CHEST_OPEN, 0.5f, 1f);
    }
}