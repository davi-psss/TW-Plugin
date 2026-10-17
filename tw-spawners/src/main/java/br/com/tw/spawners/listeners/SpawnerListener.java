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
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
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
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class SpawnerListener implements Listener {

    private final SpawnerManager manager;

    // Mapa para o sistema de compra com chat
    public static final Map<UUID, SpawnerType> pendingPurchases = new ConcurrentHashMap<>();

    public SpawnerListener(SpawnerManager manager) {
        this.manager = manager;
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onChatPurchase(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        if (pendingPurchases.containsKey(player.getUniqueId())) {
            event.setCancelled(true);
            SpawnerType type = pendingPurchases.remove(player.getUniqueId());

            if (event.getMessage().equalsIgnoreCase("cancelar")) {
                player.sendMessage("§cCompra cancelada.");
                return;
            }

            try {
                long amount = Long.parseLong(event.getMessage());
                if (amount <= 0) throw new NumberFormatException();

                // Volta para a thread principal para dar os itens e remover saldo com segurança
                Bukkit.getScheduler().runTask(TWSpawnersPlugin.getInstance(), () -> {
                    if (player.getInventory().firstEmpty() == -1) {
                        player.playSound(player.getLocation(), Sound.VILLAGER_NO, 1f, 1f);
                        player.sendMessage(ChatColor.RED + "O teu inventário está cheio!");
                        return;
                    }

                    TWUser user = TWCorePlugin.getInstance().getUserManager().getUser(player.getUniqueId());
                    double totalCost = type.getBuyCost() * amount;

                    if (user != null && user.removeBalance(totalCost)) {
                        TWCorePlugin.getInstance().getUserManager().saveUserAsync(user);
                        player.getInventory().addItem(manager.createSpawnerItem(type, amount));
                        player.playSound(player.getLocation(), Sound.LEVEL_UP, 1f, 1.5f);
                        player.sendMessage(ChatColor.GREEN + "Compraste " + amount + "x " + type.getDisplayName() + ChatColor.GREEN + " por $" + TWFormat.formatarDinheiro(totalCost) + "!");
                    } else {
                        player.playSound(player.getLocation(), Sound.VILLAGER_NO, 1f, 1f);
                        player.sendMessage(ChatColor.RED + "Faltam $" + TWFormat.formatarDinheiro(totalCost - (user != null ? user.getBalance() : 0)) + ".");
                    }
                });

            } catch (NumberFormatException e) {
                player.sendMessage("§cQuantidade inválida. Operação cancelada.");
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPlace(BlockPlaceEvent event) {
        ItemStack hand = event.getItemInHand();
        if (hand == null || !hand.hasItemMeta() || !hand.getItemMeta().hasLore()) return;

        List<String> lore = hand.getItemMeta().getLore();
        String lastLine = ChatColor.stripColor(lore.get(lore.size() - 1));
        if (!lastLine.startsWith("TWSPAWNER:")) return;

        String[] split = lastLine.split(":");
        String typeId = split[1];
        long stackToPlace = Long.parseLong(split[2]) * hand.getAmount();

        // Leitura dos Upgrades Salvos (com compatibilidade para os antigos)
        int speedLvl = 1;
        int lootLvl = 1;
        int amtLvl = 1;
        boolean autoSell = false;

        if (split.length >= 7) {
            speedLvl = Integer.parseInt(split[3]);
            lootLvl = Integer.parseInt(split[4]);
            amtLvl = Integer.parseInt(split[5]);
            autoSell = Boolean.parseBoolean(split[6]);
        }

        SpawnerType type = manager.getSpawnerTypes().get(typeId);
        if (type == null) return;

        Player player = event.getPlayer();
        Block block = event.getBlockPlaced();

        PlacedSpawner nearby = manager.findNearbySameSpawner(block.getLocation(), player.getUniqueId(), typeId, 3);
        if (nearby != null) {
            event.setCancelled(true);
            nearby.addStack(stackToPlace);

            // Fusão Inteligente: Mantém sempre o maior nível entre o chão e a mão!
            if (speedLvl > nearby.getSpeedLevel()) nearby.setSpeedLevel(speedLvl);
            if (lootLvl > nearby.getLootLevel()) nearby.setLootLevel(lootLvl);
            if (amtLvl > nearby.getAmountLevel()) nearby.setAmountLevel(amtLvl);
            if (autoSell) nearby.setAutoSell(true);

            manager.saveSpawnerAsync(nearby);
            TWSpawnersPlugin.getInstance().getHologramManager().updateOrCreateHologram(nearby, type);

            player.setItemInHand(null);
            player.playSound(player.getLocation(), Sound.ORB_PICKUP, 1f, 1.5f);
            player.sendMessage(ChatColor.GREEN + "+" + stackToPlace + "x " + type.getDisplayName() +
                    ChatColor.GREEN + " agrupado! Total: " + ChatColor.YELLOW + nearby.getStackAmount() + "x");
            return;
        }

        if (hand.getAmount() > 1) player.setItemInHand(null);

        PlacedSpawner placed = new PlacedSpawner(
                PlacedSpawner.toKey(block.getLocation()), player.getUniqueId(), player.getName(), typeId, stackToPlace
        );

        // Aplica os atributos recuperados à nova máquina recém-colocada
        placed.setSpeedLevel(speedLvl);
        placed.setLootLevel(lootLvl);
        placed.setAmountLevel(amtLvl);
        placed.setAutoSell(autoSell);

        manager.registerSpawner(placed);

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

        event.setCancelled(true);
        Player player = event.getPlayer();
        ItemStack hand = player.getItemInHand();

        if (hand != null && hand.hasItemMeta() && hand.getItemMeta().hasLore()) {
            List<String> lore = hand.getItemMeta().getLore();
            String lastLine = ChatColor.stripColor(lore.get(lore.size() - 1));

            if (lastLine.startsWith("TWBOOSTER:")) {
                if (!spawner.getOwnerUuid().equals(player.getUniqueId()) && !player.hasPermission("tw.admin")) {
                    player.sendMessage(ChatColor.RED + "Apenas o dono pode aplicar boosters!");
                    return;
                }

                String[] parts = lastLine.split(":");
                double mult = Double.parseDouble(parts[1]);
                int mins = Integer.parseInt(parts[2]);

                spawner.setBoosterMultiplier(mult);
                spawner.setBoosterEndTime((System.currentTimeMillis() / 1000L) + (mins * 60L));
                manager.saveSpawnerAsync(spawner);

                SpawnerType type = manager.getSpawnerTypes().get(spawner.getTypeId());
                if (type != null) TWSpawnersPlugin.getInstance().getHologramManager().updateOrCreateHologram(spawner, type);

                if (hand.getAmount() > 1) hand.setAmount(hand.getAmount() - 1);
                else player.setItemInHand(null);

                player.updateInventory();
                player.playSound(player.getLocation(), Sound.LEVEL_UP, 1f, 2.0f);
                player.sendMessage("§b🔥 Aplicaste um Booster de " + mult + "x por " + mins + " Minutos no spawner!");
                return;
            }
        }

        new SpawnerPanelMenu(player, spawner).open();
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBreak(BlockBreakEvent event) {
        if (event.getBlock().getType() != Material.MOB_SPAWNER) return;
        if (manager.getSpawnerAt(event.getBlock().getLocation()) != null) {
            event.setCancelled(true);
            event.getPlayer().sendMessage(ChatColor.RED + "§lAtenção! §cPara desmontares o spawner, abre o painel dele e clica no botão §4'Desmontar'§c.");
        }
    }

    @EventHandler
    public void onVanillaSpawnerSpawn(SpawnerSpawnEvent event) {
        if (manager.getSpawnerAt(event.getSpawner().getLocation()) != null) event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onMobDamage(org.bukkit.event.entity.EntityDamageEvent event) {
        if (event.getEntity() instanceof LivingEntity) {
            LivingEntity entity = (LivingEntity) event.getEntity();
            if (entity.hasMetadata(SpawnerManager.META_STACK) && entity.hasMetadata(SpawnerManager.META_LOC)) {
                if (event.getCause() == org.bukkit.event.entity.EntityDamageEvent.DamageCause.SUFFOCATION) {
                    event.setCancelled(true);
                    return;
                }
                Location anchor = (Location) entity.getMetadata(SpawnerManager.META_LOC).get(0).value();
                Bukkit.getScheduler().runTaskLater(TWSpawnersPlugin.getInstance(), () -> {
                    if (entity.isValid() && !entity.isDead()) {
                        entity.setVelocity(new org.bukkit.util.Vector(0, 0, 0));
                        entity.teleport(anchor);
                    }
                }, 1L);
            }
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
        int lootLevel = entity.hasMetadata(SpawnerManager.META_LOOT) ? entity.getMetadata(SpawnerManager.META_LOOT).get(0).asInt() : 1;

        Location anchor = entity.hasMetadata(SpawnerManager.META_LOC) ? (Location) entity.getMetadata(SpawnerManager.META_LOC).get(0).value() : entity.getLocation();
        SpawnerType type = manager.getSpawnerTypes().get(typeId);
        if (type == null) return;

        Player killer = entity.getKiller();
        long killedAmount = (killer != null && killer.isSneaking()) ? currentStack : 1L;
        long remaining = currentStack - killedAmount;

        if (killer != null) {
            TWUser user = TWCorePlugin.getInstance().getUserManager().getUser(killer.getUniqueId());
            if (user != null) {
                int lootingEnchantLvl = killer.getItemInHand() != null ? killer.getItemInHand().getEnchantmentLevel(Enchantment.LOOT_BONUS_MOBS) : 0;

                double swordLootingBonus = 1.0 + (lootingEnchantLvl * 0.15);
                double spawnerLootBonus = 1.0 + ((lootLevel - 1) * 0.10);
                double prestigeMult = manager.getPrestigeMultiplier(user.getPrestigeId());
                double globalBooster = TWEconomiaPlugin.getInstance().getGlobalBooster();

                double totalEarned = type.getValuePerKill() * killedAmount * swordLootingBonus * spawnerLootBonus * prestigeMult * globalBooster;
                user.addBalance(totalEarned);

                long tokensWon = 0;
                long soulsWon = 0;
                double tokenChance = type.getTokenChance() * (1.0 + ((lootLevel - 1) * 0.05));

                for (int i = 0; i < killedAmount; i++) {
                    if (Math.random() < tokenChance) tokensWon += 1;
                    if (Math.random() < 0.001) soulsWon += 1;
                }

                if (tokensWon > 0) user.addTokens(tokensWon);
                if (soulsWon > 0) {
                    if (killer.getInventory().firstEmpty() != -1) killer.getInventory().addItem(manager.createSoulItem(soulsWon));
                    else killer.getWorld().dropItem(killer.getLocation(), manager.createSoulItem(soulsWon));
                }

                killer.playSound(killer.getLocation(), Sound.ORB_PICKUP, 0.4f, 1.8f);

                try {
                    Class<?> actionClass = Class.forName("br.com.tw.minas.utils.ActionBarUtil");
                    actionClass.getMethod("send", Player.class, String.class)
                            .invoke(null, killer, "§c⚔ Abate: §a+$" + TWFormat.formatarDinheiro(totalEarned) + (tokensWon > 0 ? " §8| §b+" + tokensWon + " T" : "") + (soulsWon > 0 ? " §8| §d+" + soulsWon + " Almas" : ""));
                } catch (Exception e) {
                    killer.sendMessage("§c⚔ Abate: §a+$" + TWFormat.formatarDinheiro(totalEarned) + (tokensWon > 0 ? " §8| §b+" + tokensWon + " T" : "") + (soulsWon > 0 ? " §8| §d+" + soulsWon + " Almas" : ""));
                }
            }
        }

        if (remaining > 0) {
            LivingEntity next = (LivingEntity) entity.getWorld().spawnEntity(anchor, type.getEntityType());
            next.setRemoveWhenFarAway(true); next.setCanPickupItems(false);
            next.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, Integer.MAX_VALUE, 250, false, false));
            next.addPotionEffect(new PotionEffect(PotionEffectType.JUMP, Integer.MAX_VALUE, 250, false, false));
            manager.updateEntityStack(next, type, remaining, lootLevel, anchor);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPistonExtend(BlockPistonExtendEvent event) {
        for (Block block : event.getBlocks()) if (manager.getSpawnerAt(block.getLocation()) != null) event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPistonRetract(BlockPistonRetractEvent event) {
        for (Block block : event.getBlocks()) if (manager.getSpawnerAt(block.getLocation()) != null) event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onExplosion(EntityExplodeEvent event) {
        event.blockList().removeIf(block -> manager.getSpawnerAt(block.getLocation()) != null);
    }

    @EventHandler
    public void onChunkLoad(ChunkLoadEvent event) {
        for (PlacedSpawner s : manager.getPlacedSpawners().values()) {
            Location loc = s.getBukkitLocation();
            if (loc != null && loc.getWorld().equals(event.getWorld())) {
                if ((loc.getBlockX() >> 4) == event.getChunk().getX() && (loc.getBlockZ() >> 4) == event.getChunk().getZ()) {
                    SpawnerType type = manager.getSpawnerTypes().get(s.getTypeId());
                    if (type != null) TWSpawnersPlugin.getInstance().getHologramManager().updateOrCreateHologram(s, type);
                }
            }
        }
    }
}