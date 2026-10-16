package br.com.tw.maquinas.listeners;

import br.com.tw.core.TWCorePlugin;
import br.com.tw.core.data.TWUser;
import br.com.tw.core.utils.TWFormat;
import br.com.tw.economia.TWEconomiaPlugin;
import br.com.tw.maquinas.TWMaquinasPlugin;
import br.com.tw.maquinas.managers.MachineManager;
import br.com.tw.maquinas.menus.MachinePanelMenu;
import br.com.tw.maquinas.models.MachineType;
import br.com.tw.maquinas.models.PlacedMachine;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPistonExtendEvent;
import org.bukkit.event.block.BlockPistonRetractEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

import java.util.List;

public class MachineListener implements Listener {

    private final MachineManager manager;

    public MachineListener(MachineManager manager) {
        this.manager = manager;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPlace(BlockPlaceEvent event) {
        ItemStack hand = event.getItemInHand();
        if (hand == null || !hand.hasItemMeta() || !hand.getItemMeta().hasLore()) return;

        List<String> lore = hand.getItemMeta().getLore();
        String lastLine = ChatColor.stripColor(lore.get(lore.size() - 1));
        if (!lastLine.startsWith("TWMACHINE:")) return;

        String[] split = lastLine.split(":");
        String typeId = split[1];
        long stackToPlace = Long.parseLong(split[2]) * hand.getAmount();

        MachineType type = manager.getMachineTypes().get(typeId);
        if (type == null) return;

        Player player = event.getPlayer();
        Block block = event.getBlockPlaced();

        // 1. Agrupamento (Stacking) se existir máquina idêntica num raio de 3 blocos
        PlacedMachine nearby = manager.findNearbySameMachine(block.getLocation(), player.getUniqueId(), typeId, 3);
        if (nearby != null) {
            event.setCancelled(true);
            nearby.addStack(stackToPlace);
            manager.saveMachineAsync(nearby);

            // Atualiza o holograma da máquina agrupada
            TWMaquinasPlugin.getInstance().getHologramManager().updateOrCreateHologram(nearby, type);

            player.setItemInHand(null);
            player.playSound(player.getLocation(), Sound.ORB_PICKUP, 1f, 1.5f);
            player.sendMessage(ChatColor.GREEN + "+" + stackToPlace + "x " + type.getDisplayName() +
                    ChatColor.GREEN + " agrupada! Total no stack: " + ChatColor.YELLOW + nearby.getStackAmount() + "x");
            return;
        }

        if (hand.getAmount() > 1) {
            player.setItemInHand(null);
        }

        // 2. Registo de nova máquina
        PlacedMachine placed = new PlacedMachine(
                PlacedMachine.toKey(block.getLocation()),
                player.getUniqueId(),
                player.getName(),
                typeId,
                stackToPlace,
                1,
                100L, // Inicia com 100 ciclos de combustível
                0.0
        );
        manager.registerMachine(placed);

        // Cria o holograma flutuante sobre o bloco
        TWMaquinasPlugin.getInstance().getHologramManager().updateOrCreateHologram(placed, type);

        player.playSound(block.getLocation(), Sound.ANVIL_LAND, 0.6f, 1.4f);
        player.sendMessage(ChatColor.GREEN + "Colocaste " + stackToPlace + "x " + type.getDisplayName() + ChatColor.GREEN + "!");
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBreak(BlockBreakEvent event) {
        Block block = event.getBlock();
        PlacedMachine machine = manager.getMachineAt(block.getLocation());
        if (machine == null) return;

        event.setCancelled(true);
        Player player = event.getPlayer();

        if (!machine.getOwnerUuid().equals(player.getUniqueId()) && !player.hasPermission("tw.admin")) {
            player.sendMessage(ChatColor.RED + "Não és o proprietário desta máquina!");
            return;
        }

        MachineType type = manager.getMachineTypes().get(machine.getTypeId());
        if (type != null) {
            // Paga os lucros acumulados pendentes
            if (machine.getStoredEarnings() > 0) {
                TWUser user = TWCorePlugin.getInstance().getUserManager().getUser(player.getUniqueId());
                if (user != null) {
                    double mult = manager.getPrestigeMultiplier(user.getPrestigeId()) * TWEconomiaPlugin.getInstance().getGlobalBooster();
                    double payout = machine.getStoredEarnings() * mult;
                    user.addBalance(payout);
                    TWCorePlugin.getInstance().getUserManager().saveUserAsync(user);
                    player.sendMessage(ChatColor.GREEN + "Lucro acumulado de $" + TWFormat.formatarDinheiro(payout) + " depositado na tua conta!");
                }
            }
            player.getInventory().addItem(manager.createMachineItem(type, machine.getStackAmount()));
        }

        // Remove o holograma do ecrã
        TWMaquinasPlugin.getInstance().getHologramManager().removeHologram(machine.getLocationKey());

        block.setType(Material.AIR);
        manager.removeMachine(block.getLocation());
        player.playSound(player.getLocation(), Sound.ITEM_PICKUP, 1f, 1f);
        player.sendMessage(ChatColor.YELLOW + "Máquina recolhida com sucesso!");
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        Block block = event.getClickedBlock();
        if (block == null) return;

        PlacedMachine machine = manager.getMachineAt(block.getLocation());
        if (machine == null) return;

        event.setCancelled(true);
        Player player = event.getPlayer();
        ItemStack hand = player.getItemInHand();

        // Abastecimento de combustível
        if (hand != null && hand.hasItemMeta() && hand.getItemMeta().hasLore()) {
            List<String> lore = hand.getItemMeta().getLore();
            String lastLine = ChatColor.stripColor(lore.get(lore.size() - 1));
            if (lastLine.startsWith("TWFUEL:")) {
                long units = Long.parseLong(lastLine.split(":")[1]);
                machine.addFuel(units);
                manager.saveMachineAsync(machine);

                MachineType type = manager.getMachineTypes().get(machine.getTypeId());
                if (type != null) {
                    TWMaquinasPlugin.getInstance().getHologramManager().updateOrCreateHologram(machine, type);
                }

                if (hand.getAmount() > 1) {
                    hand.setAmount(hand.getAmount() - 1);
                } else {
                    player.setItemInHand(null);
                }
                player.updateInventory();
                player.playSound(player.getLocation(), Sound.FIZZ, 1f, 1.5f);
                player.sendMessage(ChatColor.GREEN + "Abasteceste a máquina com +" + units + " ciclos de combustível!");
                return;
            }
        }

        // Abre o menu de gestão da máquina
        new MachinePanelMenu(player, machine).open();
    }

    // --- PROTEÇÕES FÍSICAS: PISTÕES E EXPLOSÕES ---

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPistonExtend(BlockPistonExtendEvent event) {
        for (Block block : event.getBlocks()) {
            if (manager.getMachineAt(block.getLocation()) != null) {
                event.setCancelled(true);
                return;
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPistonRetract(BlockPistonRetractEvent event) {
        for (Block block : event.getBlocks()) {
            if (manager.getMachineAt(block.getLocation()) != null) {
                event.setCancelled(true);
                return;
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onExplosion(EntityExplodeEvent event) {
        event.blockList().removeIf(block -> manager.getMachineAt(block.getLocation()) != null);
    }

    @EventHandler
    public void onChunkLoad(org.bukkit.event.world.ChunkLoadEvent event) {
        for (PlacedMachine m : manager.getPlacedMachines().values()) {
            Location loc = m.getBukkitLocation();
            if (loc != null && loc.getWorld().equals(event.getWorld())) {
                if ((loc.getBlockX() >> 4) == event.getChunk().getX() && (loc.getBlockZ() >> 4) == event.getChunk().getZ()) {
                    MachineType type = manager.getMachineTypes().get(m.getTypeId());
                    if (type != null) {
                        TWMaquinasPlugin.getInstance().getHologramManager().updateOrCreateHologram(m, type);
                    }
                }
            }
        }
    }
}