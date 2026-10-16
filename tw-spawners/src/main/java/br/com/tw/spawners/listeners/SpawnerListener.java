package br.com.tw.spawners.listeners;

import br.com.tw.core.TWCorePlugin;
import br.com.tw.core.data.TWUser;
import br.com.tw.core.utils.TWFormat;
import br.com.tw.economia.TWEconomiaPlugin;
import br.com.tw.spawners.TWSpawnersPlugin;
import br.com.tw.spawners.managers.SpawnerManager;
import br.com.tw.spawners.menus.SpawnerPanelMenu;
import br.com.tw.spawners.models.PlacedSpawner;
import br.com.tw.spawners.models.SpawnerType;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.block.CreatureSpawner;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPistonExtendEvent;
import org.bukkit.event.block.BlockPistonRetractEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.entity.SpawnerSpawnEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.List;

public class SpawnerListener implements Listener {

    private final SpawnerManager manager;

    public SpawnerListener(SpawnerManager manager) {
        this.manager = manager;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPlace(BlockPlaceEvent event) {
        ItemStack hand = event.getItemInHand();
        if (hand == null || hand.getType() != Material.MOB_SPAWNER || !hand.hasItemMeta() || !hand.getItemMeta().hasLore()) return;

        List<String> lore = hand.getItemMeta().getLore();
        String lastLine = ChatColor.stripColor(lore.get(lore.size() - 1));
        if (!lastLine.startsWith("TWSPAWNER:")) return;

        String[] split = lastLine.split(":");
        String typeId = split[1];
        long stackToPlace = Long.parseLong(split[2]) * hand.getAmount();

        SpawnerType type = manager.getSpawnerTypes().get(typeId);
        if (type == null) return;

        Player player = event.getPlayer();
        Block block = event.getBlockPlaced();

        // 1. Stacking se houver outro spawner do mesmo tipo perto
        PlacedSpawner nearby = manager.findNearbySameSpawner(block.getLocation(), player.getUniqueId(), typeId, 3);
        if (nearby != null) {
            event.setCancelled(true);
            nearby.addStack(stackToPlace);
            manager.saveSpawnerAsync(nearby);

            TWSpawnersPlugin.getInstance().getHologramManager().updateOrCreateHologram(nearby, type);

            player.setItemInHand(null);
            player.playSound(player.getLocation(), Sound.ORB_PICKUP, 1f, 1.5f);
            player.sendMessage(ChatColor.GREEN + "+" + stackToPlace + "x " + type.getDisplayName() +
                    ChatColor.GREEN + " agrupado! Total: " + ChatColor.YELLOW + nearby.getStackAmount() + "x");
            return;
        }

        if (hand.getAmount() > 1) {
            player.setItemInHand(null);
        }

        if (block.getState() instanceof CreatureSpawner) {
            CreatureSpawner cs = (CreatureSpawner) block.getState();
            cs.setSpawnedType(type.getEntityType());
            cs.update();
        }

        PlacedSpawner placed = new PlacedSpawner(
                PlacedSpawner.toKey(block.getLocation()),
                player.getUniqueId(),
                player.getName(),
                typeId,
                stackToPlace,
                1,
                true,
                System.currentTimeMillis(),
                0L
        );
        manager.registerSpawner(placed);

        // Cria o holograma flutuante
        TWSpawnersPlugin.getInstance().getHologramManager().updateOrCreateHologram(placed, type);

        player.playSound(block.getLocation(), Sound.ANVIL_LAND, 0.7f, 1.2f);
        player.sendMessage(ChatColor.GREEN + "Colocaste " + stackToPlace + "x " + type.getDisplayName() + ChatColor.GREEN + "!");
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onSpawnerInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        Block b = event.getClickedBlock();
        if (b == null || b.getType() != Material.MOB_SPAWNER) return;

        PlacedSpawner spawner = manager.getSpawnerAt(b.getLocation());
        if (spawner == null) return;

        // Se estiver com outro spawner na mão, deixa colocar/agrupar
        ItemStack hand = event.getItem();
        if (hand != null && hand.getType() == Material.MOB_SPAWNER) return;

        event.setCancelled(true);
        // Abre o menu gráfico
        new SpawnerPanelMenu(event.getPlayer(), spawner).open();
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBreak(BlockBreakEvent event) {
        Block block = event.getBlock();
        if (block.getType() != Material.MOB_SPAWNER) return;

        PlacedSpawner spawner = manager.getSpawnerAt(block.getLocation());
        if (spawner == null) return;

        event.setCancelled(true);
        Player player = event.getPlayer();

        if (!spawner.getOwnerUuid().equals(player.getUniqueId()) && !player.hasPermission("tw.admin")) {
            player.sendMessage(ChatColor.RED + "Apenas o dono pode recolher este gerador!");
            return;
        }

        SpawnerType type = manager.getSpawnerTypes().get(spawner.getTypeId());
        if (type != null) {
            player.getInventory().addItem(manager.createSpawnerItem(type, spawner.getStackAmount()));
        }

        TWSpawnersPlugin.getInstance().getHologramManager().removeHologram(spawner.getLocationKey());

        block.setType(Material.AIR);
        manager.removeSpawner(block.getLocation());
        player.playSound(player.getLocation(), Sound.ITEM_PICKUP, 1f, 1f);
        player.sendMessage(ChatColor.YELLOW + "Gerador recolhido com sucesso!");
    }

    @EventHandler
    public void onVanillaSpawnerSpawn(SpawnerSpawnEvent event) {
        if (manager.getSpawnerAt(event.getSpawner().getLocation()) != null) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onStackedEntityDeath(EntityDeathEvent event) {
        LivingEntity entity = event.getEntity();
        if (!entity.hasMetadata(SpawnerManager.META_STACK) || !entity.hasMetadata(SpawnerManager.META_TYPE)) return;

        event.getDrops().clear();
        event.setDroppedExp(0);

        long currentStack = entity.getMetadata(SpawnerManager.META_STACK).get(0).asLong();
        String typeId = entity.getMetadata(SpawnerManager.META_TYPE).get(0).asString();
        SpawnerType type = manager.getSpawnerTypes().get(typeId);
        if (type == null) return;

        Player killer = entity.getKiller();
        long killedAmount = (killer != null && killer.isSneaking()) ? currentStack : 1L;
        long remaining = currentStack - killedAmount;

        if (killer != null) {
            TWUser user = TWCorePlugin.getInstance().getUserManager().getUser(killer.getUniqueId());
            if (user != null) {
                int lootingLvl = 0;
                if (killer.getItemInHand() != null) {
                    lootingLvl = killer.getItemInHand().getEnchantmentLevel(Enchantment.LOOT_BONUS_MOBS);
                }

                double lootingBonus = 1.0 + (lootingLvl * 0.15);
                double prestigeMult = manager.getPrestigeMultiplier(user.getPrestigeId());
                double globalBooster = TWEconomiaPlugin.getInstance().getGlobalBooster();

                double totalEarned = type.getValuePerKill() * killedAmount * lootingBonus * prestigeMult * globalBooster;
                user.addBalance(totalEarned);

                long tokensWon = 0;
                if (Math.random() < type.getTokenChance()) {
                    tokensWon = Math.max(1, killedAmount / 5);
                    user.addTokens(tokensWon);
                }

                killer.playSound(killer.getLocation(), Sound.ORB_PICKUP, 0.4f, 1.8f);
                killer.sendMessage(ChatColor.GREEN + "+ $" + TWFormat.formatarDinheiro(totalEarned) +
                        ChatColor.GRAY + " (" + killedAmount + "x " + ChatColor.stripColor(type.getDisplayName()) + ")" +
                        (tokensWon > 0 ? ChatColor.AQUA + " | +" + tokensWon + " Tokens" : ""));
            }
        }

        if (remaining > 0) {
            LivingEntity next = (LivingEntity) entity.getWorld().spawnEntity(entity.getLocation(), type.getEntityType());
            next.setRemoveWhenFarAway(true);
            next.setCanPickupItems(false);
            next.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, Integer.MAX_VALUE, 100, false, false));
            manager.updateEntityStack(next, type, remaining);
        }
    }

    @EventHandler
    public void onChunkLoad(ChunkLoadEvent event) {
        for (PlacedSpawner s : manager.getPlacedSpawners().values()) {
            Location loc = s.getBukkitLocation();
            if (loc != null && loc.getWorld().equals(event.getWorld())) {
                if ((loc.getBlockX() >> 4) == event.getChunk().getX() && (loc.getBlockZ() >> 4) == event.getChunk().getZ()) {
                    SpawnerType type = manager.getSpawnerTypes().get(s.getTypeId());
                    if (type != null) {
                        TWSpawnersPlugin.getInstance().getHologramManager().updateOrCreateHologram(s, type);
                    }
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPistonExtend(BlockPistonExtendEvent event) {
        for (Block block : event.getBlocks()) {
            if (manager.getSpawnerAt(block.getLocation()) != null) {
                event.setCancelled(true);
                return;
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPistonRetract(BlockPistonRetractEvent event) {
        for (Block block : event.getBlocks()) {
            if (manager.getSpawnerAt(block.getLocation()) != null) {
                event.setCancelled(true);
                return;
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onExplosion(EntityExplodeEvent event) {
        event.blockList().removeIf(block -> manager.getSpawnerAt(block.getLocation()) != null);
    }
}