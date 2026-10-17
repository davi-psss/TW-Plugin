package br.com.tw.maquinas.listeners;

import br.com.tw.maquinas.TWMaquinasPlugin;
import br.com.tw.maquinas.managers.MachineManager;
import br.com.tw.maquinas.menus.MachinePanelMenu;
import br.com.tw.maquinas.models.MachineType;
import br.com.tw.maquinas.models.PlacedMachine;
import org.bukkit.ChatColor;
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

        PlacedMachine nearby = manager.findNearbySameMachine(block.getLocation(), player.getUniqueId(), typeId, 3);
        if (nearby != null) {
            event.setCancelled(true);
            nearby.addStack(stackToPlace);
            manager.saveMachineAsync(nearby);

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

        PlacedMachine placed = new PlacedMachine(
                PlacedMachine.toKey(block.getLocation()),
                player.getUniqueId(),
                player.getName(),
                typeId,
                stackToPlace,
                100L,
                0.0
        );
        manager.registerMachine(placed);

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
        player.playSound(player.getLocation(), Sound.VILLAGER_NO, 1f, 1f);
        player.sendMessage(ChatColor.RED + "§lAtenção! §cPara recolheres a máquina, clica nela e usa o botão §4'Recolher' §cno menu.");
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

        if (hand != null && hand.hasItemMeta() && hand.getItemMeta().hasLore()) {
            List<String> lore = hand.getItemMeta().getLore();
            String lastLine = ChatColor.stripColor(lore.get(lore.size() - 1));

            if (lastLine.startsWith("TWFUEL:")) {
                if (!machine.getOwnerUuid().equals(player.getUniqueId()) && !player.hasPermission("tw.admin")) {
                    player.sendMessage(ChatColor.RED + "Apenas o dono pode abastecer esta máquina!");
                    return;
                }

                long unitsPerItem = Long.parseLong(lastLine.split(":")[1]);

                // NOVO: Se o jogador estiver no Shift, consome o pack inteiro. Se não, consome só 1.
                int itemsToConsume = player.isSneaking() ? hand.getAmount() : 1;
                long totalUnits = unitsPerItem * itemsToConsume;

                machine.addFuel(totalUnits);
                manager.saveMachineAsync(machine);

                MachineType type = manager.getMachineTypes().get(machine.getTypeId());
                if (type != null) {
                    TWMaquinasPlugin.getInstance().getHologramManager().updateOrCreateHologram(machine, type);
                }

                if (hand.getAmount() > itemsToConsume) {
                    hand.setAmount(hand.getAmount() - itemsToConsume);
                } else {
                    player.setItemInHand(null); // Gastou o pack todo
                }

                player.updateInventory();
                player.playSound(player.getLocation(), Sound.FIZZ, 1f, 1.5f);
                player.sendMessage(ChatColor.GREEN + "Abasteceste a máquina com +" + totalUnits + " ciclos de combustível!");
                return;
            }
        }

        new MachinePanelMenu(player, machine).open();
    }

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
            org.bukkit.Location loc = m.getBukkitLocation();
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