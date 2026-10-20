package br.com.tw.farms.listeners;

import br.com.tw.core.TWCorePlugin;
import br.com.tw.core.utils.TWItemTags;
import br.com.tw.farms.TWFarmsPlugin;
import br.com.tw.farms.managers.FarmManager;
import br.com.tw.farms.menus.FarmPanelMenu;
import br.com.tw.farms.models.FarmType;
import br.com.tw.farms.models.PhysicalCrop;
import br.com.tw.farms.models.PlacedFarm;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.ItemSpawnEvent;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockBurnEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.block.BlockGrowEvent;
import org.bukkit.event.block.BlockPistonExtendEvent;
import org.bukkit.event.block.BlockPistonRetractEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;

public final class FarmListener implements Listener {
    private final FarmManager manager;

    public FarmListener(FarmManager manager) {
        this.manager = manager;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onFarmPlace(BlockPlaceEvent event) {
        ItemStack hand = event.getItemInHand();
        if (hand == null || !TWItemTags.has(hand, "tw_farm_type")) return;

        Player player = event.getPlayer();
        Block block = event.getBlockPlaced();
        PlacedFarm farm;
        try {
            farm = manager.farmFromItem(hand, block.getLocation(), player);
        } catch (RuntimeException e) {
            event.setCancelled(true);
            player.sendMessage(ChatColor.RED + "Os dados desta farm estão inválidos. A colocação foi cancelada.");
            TWFarmsPlugin.getInstance().getLogger().log(Level.WARNING,
                    "Item de farm inválido usado por " + player.getName() + ".", e);
            return;
        }
        if (farm == null) {
            event.setCancelled(true);
            player.sendMessage(ChatColor.RED + "Este item de farm não corresponde a um tipo configurado.");
            return;
        }
        FarmType type = manager.getFarmTypes().get(farm.getTypeId());
        PlacedFarm existing = manager.findNearbySameFarm(block.getLocation(), player.getUniqueId(), type.getId(), 3);
        if (existing != null) {
            event.setCancelled(true);
            try {
                existing.mergeFrom(farm);
            } catch (ArithmeticException e) {
                player.sendMessage(ChatColor.RED + "O stack desta farm atingiu o limite numérico.");
                return;
            }
            consumeOne(hand, player);
            manager.saveFarmAsync(existing);
            TWFarmsPlugin.getInstance().getHologramManager().updateOrCreate(existing, type);
            player.sendMessage(ChatColor.GREEN + "Farm agrupada. Stack atual: §e" + existing.getStackAmount() + "x");
            return;
        }
        manager.registerFarm(farm);
        TWFarmsPlugin.getInstance().getHologramManager().updateOrCreate(farm, type);
        player.sendMessage(ChatColor.GREEN + "Colocaste "
                + ChatColor.translateAlternateColorCodes('&', type.getDisplayName()) + " §7(x"
                + farm.getStackAmount() + ").");
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPlantPlace(BlockPlaceEvent event) {
        if (TWItemTags.has(event.getItemInHand(), "tw_farm_type")) return;
        if (!isCropWorld(event.getBlockPlaced())) return;
        Block block = event.getBlockPlaced();
        for (FarmType type : manager.getFarmTypes().values()) {
            if (type.getPlantMaterials().contains(block.getType())) {
                manager.registerCrop(new PhysicalCrop(PlacedFarm.toKey(block.getLocation()),
                        event.getPlayer().getUniqueId(), event.getPlayer().getName(), type.getId()));
                return;
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onFarmBreak(BlockBreakEvent event) {
        Block block = event.getBlock();
        PlacedFarm farm = manager.getFarmAt(block.getLocation());
        if (farm != null) {
            event.setCancelled(true);
            event.getPlayer().sendMessage(ChatColor.RED
                    + "Esta farm não pode ser quebrada. Clica com o botão direito e usa Desmontar.");
            return;
        }

        PhysicalCrop crop = manager.findCropFor(block);
        if (crop == null) {
            if (!isCropWorld(block) || !manager.isFarmCropMaterial(block.getType())) return;
            FarmType type = manager.getTypeForHarvestMaterial(block.getType());
            if (type == null) type = manager.getTypeForPlantMaterial(block.getType());
            if (type == null) {
                return;
            }
            Block cropRoot = findRoot(block);
            crop = new PhysicalCrop(PlacedFarm.toKey(cropRoot.getLocation()),
                    event.getPlayer().getUniqueId(), event.getPlayer().getName(), type.getId());
            manager.registerCrop(crop);
        }
        PhysicalCrop tracked = crop;
        Bukkit.getScheduler().runTask(TWFarmsPlugin.getInstance(), () -> {
            if (block.getType() == Material.AIR
                    && manager.getCropAt(block.getLocation()) == tracked) {
                manager.unregisterCrop(tracked);
            }
        });
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPhysicalCropDrop(ItemSpawnEvent event) {
        if (manager.capturePhysicalDrop(event.getLocation(), event.getEntity().getItemStack())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK || event.getClickedBlock() == null) return;
        Player player = event.getPlayer();
        Block clicked = event.getClickedBlock();
        PlacedFarm farm = manager.getFarmAt(clicked.getLocation());
        ItemStack hand = player.getInventory().getItemInMainHand();

        if (farm != null) {
            event.setCancelled(true);
            if (!canManage(player, farm)) {
                player.sendMessage(ChatColor.RED + "Apenas o proprietário pode gerir esta farm.");
                return;
            }
            new FarmPanelMenu(player, farm).open();
            return;
        }

        PhysicalCrop crop = manager.findCropFor(clicked);
        if (crop != null && hand != null && TWItemTags.has(hand, "tw_farm_booster_mult")) {
            event.setCancelled(true);
            if (!crop.getOwnerUuid().equals(player.getUniqueId()) && !player.hasPermission("tw.admin")) {
                player.sendMessage(ChatColor.RED + "Apenas o proprietário da cultura pode aplicar o booster.");
                return;
            }
            applyGrowthBooster(player, hand, crop);
            return;
        }

        if (hand != null && TWItemTags.getBoolean(hand, "tw_farm_harvester", false)) {
            event.setCancelled(true);
            harvestWithHoe(player, clicked, hand);
        }
    }

    private void applyGrowthBooster(Player player, ItemStack hand, PhysicalCrop crop) {
        double multiplier = TWItemTags.getDouble(hand, "tw_farm_booster_mult", 0.0);
        long duration = TWItemTags.getLong(hand, "tw_farm_booster_seconds", 0);
        boolean activated;
        try {
            activated = manager.applyGrowthBooster(crop, multiplier, duration);
        } catch (IllegalArgumentException | ArithmeticException e) {
            player.sendMessage(ChatColor.RED + "O booster está inválido ou excede a duração permitida.");
            return;
        }
        if (!activated) {
            player.sendMessage(ChatColor.YELLOW
                    + "Já há um booster igual ou mais forte ativo. A duração não acumula.");
            return;
        }
        consume(hand, player, 1);
        player.sendMessage(ChatColor.GREEN + "Booster x" + multiplier
                + " aplicado às tuas culturas deste tipo. A duração não acumula; "
                + "um booster mais forte só melhora a potência do efeito atual.");
    }

    private void harvestWithHoe(Player player, Block center, ItemStack hoe) {
        boolean goldenScythe = "golden_scythe".equals(TWItemTags.getString(hoe, "tw_crate_effect"));
        long scytheCharges = TWItemTags.getLong(hoe, "tw_crate_charges", 0L);
        if (goldenScythe && scytheCharges < 1L) {
            player.getInventory().setItemInMainHand(null);
            player.sendMessage(ChatColor.RED + "A Foice Dourada já esgotou os seus usos.");
            return;
        }
        int radius = TWFarmsPlugin.getInstance().getConfig().getInt("farm.physical-crop-harvest-radius", 1);
        if (radius < 0 || radius > 5) throw new IllegalStateException("Raio da enxada deve estar entre 0 e 5.");
        Set<String> visited = new HashSet<>();
        long count = 0;
        for (int x = -radius; x <= radius && (!goldenScythe || scytheCharges > 0L); x++) {
            for (int y = -radius; y <= radius && (!goldenScythe || scytheCharges > 0L); y++) {
                for (int z = -radius; z <= radius && (!goldenScythe || scytheCharges > 0L); z++) {
                    Block block = center.getRelative(x, y, z);
                    PhysicalCrop crop = manager.findCropFor(block);
                    if (crop == null || !visited.add(crop.getLocationKey())) continue;
                    int yieldMultiplier = goldenScythe && scytheCharges > 0 ? 2 : 1;
                    if (manager.harvestPhysical(block, false, yieldMultiplier)) {
                        count++;
                        if (goldenScythe && scytheCharges > 0) {
                            scytheCharges--;
                            java.util.Map<String, Object> tags = new java.util.HashMap<>();
                            tags.put("tw_crate_charges", scytheCharges);
                            if (scytheCharges == 0) tags.put("tw_farm_harvester", false);
                            hoe = TWItemTags.set(hoe, tags);
                            player.getInventory().setItemInMainHand(hoe);
                        }
                    }
                }
            }
        }
        if (goldenScythe && scytheCharges == 0L) {
            player.getInventory().setItemInMainHand(null);
            player.sendMessage(ChatColor.YELLOW + "A Foice Dourada partiu após esgotar as cargas.");
            return;
        }
        long total = TWItemTags.getLong(hoe, "tw_farm_hoe_harvests", 0);
        Map<String, Object> tags = new java.util.HashMap<>();
        tags.put("tw_farm_harvester", true);
        tags.put("tw_farm_hoe_harvests", Math.addExact(total, count));
        player.getInventory().setItemInMainHand(TWItemTags.set(hoe, tags));
        if (count == 0) player.sendMessage(ChatColor.YELLOW + "Não há plantações maduras registadas por perto.");
        else player.sendMessage(ChatColor.GREEN + "A enxada enviou " + count
                + " colheita(s) ao Armazém da Farm. Total: " + (total + count) + ".");
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onCropMature(BlockGrowEvent event) {
        if (manager.accelerateGrowth(event.getBlock(), event.getNewState())) event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPistonExtend(BlockPistonExtendEvent event) {
        protectPiston(event.getBlocks(), event::setCancelled);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPistonRetract(BlockPistonRetractEvent event) {
        protectPiston(event.getBlocks(), event::setCancelled);
    }

    private void protectPiston(Iterable<Block> blocks, java.util.function.Consumer<Boolean> cancel) {
        boolean containsFarm = false;
        for (Block block : blocks) {
            if (manager.getFarmAt(block.getLocation()) != null) containsFarm = true;
        }
        if (containsFarm) cancel.accept(true);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onExplosion(EntityExplodeEvent event) {
        protectExplosion(event.blockList());
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockExplosion(BlockExplodeEvent event) {
        protectExplosion(event.blockList());
    }

    private void protectExplosion(java.util.List<Block> blocks) {
        blocks.removeIf(block -> {
            if (manager.getFarmAt(block.getLocation()) != null) return true;
            return false;
        });
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBurn(BlockBurnEvent event) {
        if (manager.getFarmAt(event.getBlock().getLocation()) != null) {
            event.setCancelled(true);
        }
    }

    private boolean isCropWorld(Block block) {
        return !TWFarmsPlugin.getInstance().getConfig().getBoolean("farm.crop-owner-worlds-only", true)
                || TWCorePlugin.getInstance().isPlotsWorld(block.getWorld().getName());
    }

    private boolean canManage(Player player, PlacedFarm farm) {
        return farm.getOwnerUuid().equals(player.getUniqueId()) || player.hasPermission("tw.admin");
    }

    private Block findRoot(Block block) {
        Material material = block.getType();
        if (material != Material.CACTUS && material != Material.SUGAR_CANE) return block;
        Block root = block;
        while (root.getRelative(0, -1, 0).getType() == material) root = root.getRelative(0, -1, 0);
        return root;
    }

    private void consumeOne(ItemStack hand, Player player) {
        consume(hand, player, 1);
    }

    private void consume(ItemStack hand, Player player, int amount) {
        if (hand.getAmount() > amount) hand.setAmount(hand.getAmount() - amount);
        else player.getInventory().setItemInMainHand(null);
        player.updateInventory();
    }
}
