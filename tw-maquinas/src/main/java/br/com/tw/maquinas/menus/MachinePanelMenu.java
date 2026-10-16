package br.com.tw.maquinas.menus;

import br.com.tw.core.TWCorePlugin;
import br.com.tw.core.data.TWUser;
import br.com.tw.core.menus.TWMenu;
import br.com.tw.core.utils.TWFormat;
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
        this.inventory = Bukkit.createInventory(this, 27, ChatColor.DARK_GRAY + "Painel da Máquina");
        setupItems();
    }

    private void setupItems() {
        inventory.clear();
        if (type == null) return;

        // Slot 11: Recolher Lucros (Com Multiplicadores de Prestígio e Booster Global)
        ItemStack collect = new ItemStack(Material.GOLD_INGOT);
        ItemMeta cMeta = collect.getItemMeta();
        cMeta.setDisplayName(ChatColor.GREEN + "" + ChatColor.BOLD + "Recolher Produção");
        cMeta.setLore(Arrays.asList(
                ChatColor.GRAY + "Valor Acumulado Base: " + ChatColor.GREEN + "$" + TWFormat.formatarDinheiro(machine.getStoredEarnings()),
                ChatColor.GRAY + "(Bónus de Prestígio e Booster aplicados ao vender)",
                "",
                ChatColor.YELLOW + "Clica para transferir para a tua conta!"
        ));
        collect.setItemMeta(cMeta);
        inventory.setItem(11, collect);

        // Slot 13: Informações da Máquina
        ItemStack info = new ItemStack(type.getMaterial());
        ItemMeta iMeta = info.getItemMeta();
        iMeta.setDisplayName(type.getDisplayName());
        iMeta.setLore(Arrays.asList(
                ChatColor.GRAY + "Dono: " + ChatColor.WHITE + machine.getOwnerName(),
                ChatColor.GRAY + "Stack Acumulado: " + ChatColor.YELLOW + machine.getStackAmount() + "x",
                ChatColor.GRAY + "Nível de Eficiência: " + ChatColor.AQUA + "Lv. " + machine.getLevel(),
                ChatColor.GRAY + "Combustível Restante: " + ChatColor.GOLD + machine.getFuel() + " ciclos"
        ));
        info.setItemMeta(iMeta);
        inventory.setItem(13, info);

        // Slot 15: Evoluir Nível (Com Tokens da Mina!)
        long tokenUpgradeCost = machine.getLevel() * 500L;
        ItemStack upgrade = new ItemStack(Material.EXP_BOTTLE);
        ItemMeta uMeta = upgrade.getItemMeta();
        uMeta.setDisplayName(ChatColor.AQUA + "" + ChatColor.BOLD + "Evoluir Eficiência (+25%)");
        uMeta.setLore(Arrays.asList(
                ChatColor.GRAY + "Nível Atual: " + ChatColor.WHITE + machine.getLevel(),
                ChatColor.GRAY + "Custo: " + ChatColor.AQUA + TWFormat.formatarDinheiro(tokenUpgradeCost) + " Tokens",
                "",
                ChatColor.YELLOW + "Clica para melhorar com Tokens!"
        ));
        upgrade.setItemMeta(uMeta);
        inventory.setItem(15, upgrade);
    }

    @Override
    public void onClick(InventoryClickEvent event) {
        if (!machine.getOwnerUuid().equals(player.getUniqueId()) && !player.hasPermission("tw.admin")) {
            player.sendMessage(ChatColor.RED + "Apenas o dono pode gerir esta máquina!");
            return;
        }

        TWUser user = TWCorePlugin.getInstance().getUserManager().getUser(player.getUniqueId());
        if (user == null) return;

        int slot = event.getSlot();
        if (slot == 11) {
            double baseStored = machine.getStoredEarnings();
            if (baseStored <= 0) {
                player.playSound(player.getLocation(), Sound.VILLAGER_NO, 1f, 1f);
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

            player.playSound(player.getLocation(), Sound.LEVEL_UP, 1f, 1.5f);
            player.sendMessage(ChatColor.GREEN + "Recolheste $" + TWFormat.formatarDinheiro(finalPayout) +
                    ChatColor.GRAY + " (Mult: " + (prestigeMult * globalBooster) + "x)!");
            setupItems();
        } else if (slot == 15) {
            long tokenCost = machine.getLevel() * 500L;
            if (user.removeTokens(tokenCost)) {
                machine.upgradeLevel();
                manager.saveMachineAsync(machine);
                TWMaquinasPlugin.getInstance().getHologramManager().updateOrCreateHologram(machine, type);
                TWCorePlugin.getInstance().getUserManager().saveUserAsync(user);

                player.playSound(player.getLocation(), Sound.ANVIL_USE, 1f, 1.5f);
                player.sendMessage(ChatColor.GREEN + "Máquina evoluída para o nível " + machine.getLevel() + "!");
                setupItems();
            } else {
                player.playSound(player.getLocation(), Sound.VILLAGER_NO, 1f, 1f);
                player.sendMessage(ChatColor.RED + "Não tens Tokens suficientes! Precisas de " + TWFormat.formatarDinheiro(tokenCost) + " Tokens.");
            }
        }
    }

    @Override
    public Inventory getInventory() { return inventory; }

    public void open() {
        player.openInventory(inventory);
        player.playSound(player.getLocation(), Sound.CLICK, 1f, 1f);
    }
}