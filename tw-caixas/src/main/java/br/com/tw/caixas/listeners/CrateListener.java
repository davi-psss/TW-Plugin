package br.com.tw.caixas.listeners;

import br.com.tw.caixas.TWCaixasPlugin;
import br.com.tw.caixas.data.PhysicalCrate;
import br.com.tw.caixas.managers.CrateOpenings;
import br.com.tw.caixas.managers.CrateService;
import br.com.tw.caixas.managers.PlayerEffectMemory;
import br.com.tw.caixas.managers.RewardDelivery;
import br.com.tw.caixas.menus.CrateBulkMenu;
import br.com.tw.caixas.menus.CratePreviewMenu;
import br.com.tw.caixas.menus.ManualOpeningMenu;
import br.com.tw.caixas.model.CrateType;
import br.com.tw.caixas.model.CrateEffect;
import br.com.tw.core.TWCorePlugin;
import br.com.tw.core.utils.TWLuckPermsGroups;
import br.com.tw.core.data.TWUser;
import br.com.tw.core.events.TWFarmHarvestEvent;
import br.com.tw.core.events.TWBlockMinedEvent;
import br.com.tw.core.events.TWMachineCycleEvent;
import br.com.tw.core.events.TWMiningPrepareEvent;
import br.com.tw.core.events.TWSpawnerKillEvent;
import br.com.tw.core.utils.TWItemTags;
import br.com.tw.maquinas.TWMaquinasPlugin;
import br.com.tw.maquinas.models.PlacedMachine;
import br.com.tw.ranks.TWRanksPlugin;
import br.com.tw.spawners.TWSpawnersPlugin;
import br.com.tw.spawners.models.PlacedSpawner;
import br.com.tw.spawners.models.SpawnerType;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.block.Action;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.FixedMetadataValue;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

public final class CrateListener implements Listener {
    private static final String HOLOGRAM_METADATA = "tw_crate_hologram";

    private final TWCaixasPlugin plugin;
    private final CrateService service;
    private final CrateOpenings openings;
    private final RewardDelivery rewardDelivery;
    private final PlayerEffectMemory effectMemory;
    private final Map<String, CrateType> physicalCrates = new ConcurrentHashMap<>();
    private final List<ArmorStand> holograms = new ArrayList<>();

    public CrateListener(TWCaixasPlugin plugin, CrateService service,
                         CrateOpenings openings, RewardDelivery rewardDelivery,
                         PlayerEffectMemory effectMemory) {
        this.plugin = plugin;
        this.service = service;
        this.openings = openings;
        this.rewardDelivery = rewardDelivery;
        this.effectMemory = effectMemory;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        service.loadAccount(event.getPlayer().getUniqueId(), account -> { },
                () -> event.getPlayer().sendMessage("§cNão foi possível carregar a tua conta de chaves."));
        effectMemory.load(event.getPlayer().getUniqueId());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        effectMemory.flush(event.getPlayer().getUniqueId());
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPhysicalCrateInteract(PlayerInteractEvent event) {
        Block block = event.getClickedBlock();
        if (block == null) return;
        CrateType type = physicalCrates.get(key(block.getWorld().getName(), block.getX(), block.getY(), block.getZ()));
        if (type == null) return;
        if (event.getAction() != org.bukkit.event.block.Action.LEFT_CLICK_BLOCK
                && event.getAction() != org.bukkit.event.block.Action.RIGHT_CLICK_BLOCK) return;
        event.setCancelled(true);
        Player player = event.getPlayer();
        if (event.getAction() == org.bukkit.event.block.Action.LEFT_CLICK_BLOCK) {
            player.openInventory(new CratePreviewMenu(service.getCatalog(), type).getInventory());
            return;
        }
        if (!player.isSneaking()) {
            openings.openManual(player, type);
            return;
        }
        service.loadAccount(player.getUniqueId(), account ->
                player.openInventory(new CrateBulkMenu(service, openings, player, account, type).getInventory()),
                () -> player.sendMessage("§cNão foi possível carregar o teu saldo de chaves."));
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPhysicalCrateBreak(BlockBreakEvent event) {
        Block block = event.getBlock();
        if (!physicalCrates.containsKey(key(block.getWorld().getName(), block.getX(), block.getY(), block.getZ()))) return;
        event.setCancelled(true);
        event.getPlayer().sendMessage("§cEsta caixa é protegida. Usa /caixa remover para a retirar.");
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPhysicalCrateExplosion(EntityExplodeEvent event) {
        event.blockList().removeIf(block -> physicalCrates.containsKey(
                key(block.getWorld().getName(), block.getX(), block.getY(), block.getZ())));
    }

    @EventHandler
    public void onMenuClose(InventoryCloseEvent event) {
        if (event.getInventory().getHolder() instanceof ManualOpeningMenu) {
            ((ManualOpeningMenu) event.getInventory().getHolder()).onClose();
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onMinePrepare(TWMiningPrepareEvent event) {
        Player player = event.getPlayer();
        if (!effectMemory.isLoaded(player.getUniqueId())
                || effectMemory.getCharges(player.getUniqueId(), CrateEffect.SEISMIC_CHARGE) < 1L) return;
        List<Block> blocks = event.getBlocksToBreak();
        if (blocks.isEmpty()) return;
        Block center = event.getOriginBlock() == null ? blocks.get(0) : event.getOriginBlock();
        Set<String> included = new HashSet<>();
        for (Block block : blocks) included.add(blockKey(block));
        boolean added = false;
        int addedBlocks = 0;
        for (int x = -1; x <= 1 && addedBlocks < 27; x++) {
            for (int y = -1; y <= 1 && addedBlocks < 27; y++) {
                for (int z = -1; z <= 1 && addedBlocks < 27; z++) {
                    Block block = center.getRelative(x, y, z);
                    if (block.getType() == Material.AIR || !included.add(blockKey(block))) continue;
                    blocks.add(block);
                    added = true;
                    addedBlocks++;
                }
            }
        }
        if (added) {
            effectMemory.consume(player.getUniqueId(), CrateEffect.SEISMIC_CHARGE, 1L);
            player.sendMessage("§cCarga Sísmica ativada: §e" + blocks.size()
                    + " blocos §cserão processados nesta picaretada.");
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onBlockMined(TWBlockMinedEvent event) {
        Player player = event.getPlayer();
        long keys = rollDrops(1L, service.getCatalog().getDropChance(CrateType.MINEIRA));
        if (keys > 0) awardKeys(player.getUniqueId(), CrateType.MINEIRA, keys);

        UUID playerId = player.getUniqueId();
        if (effectMemory.isLoaded(playerId)) {
            if (effectMemory.getCharges(playerId, CrateEffect.MINING_CONTRACT) > 0L
                    && multiplyPayout(event, 2.0)) {
                effectMemory.consume(playerId, CrateEffect.MINING_CONTRACT, 1L);
                notifyEffectMilestone(player, CrateEffect.MINING_CONTRACT);
            }
            if (effectMemory.getCharges(playerId, CrateEffect.AUTOSELL_DRILL) > 0L
                    && multiplyPayout(event, 1.2)) {
                effectMemory.consume(playerId, CrateEffect.AUTOSELL_DRILL, 1L);
                notifyEffectMilestone(player, CrateEffect.AUTOSELL_DRILL);
            }
        }
    }

    @EventHandler
    public void onMachineCycle(TWMachineCycleEvent event) {
        long keys = rollDrops(1L, service.getCatalog().getDropChance(CrateType.TECNOLOGICA));
        if (keys > 0) awardKeys(event.getOwnerId(), CrateType.TECNOLOGICA, keys);
    }

    @EventHandler
    public void onSpawnerKill(TWSpawnerKillEvent event) {
        Player killer = event.getKiller();
        if (killer == null) return;
        long keys = rollDrops(event.getKilledAmount(), service.getCatalog().getDropChance(CrateType.SANGUINARIA));
        if (keys > 0) awardKeys(killer.getUniqueId(), CrateType.SANGUINARIA, keys);

        if (event.getKilledAmount() < 1L || !effectMemory.isLoaded(killer.getUniqueId())) return;
        long charges = effectMemory.consume(killer.getUniqueId(), CrateEffect.SOUL_MAGNET, event.getKilledAmount());
        if (charges > 0L) {
            event.addBonusSouls(charges);
            notifyEffectMilestone(killer, CrateEffect.SOUL_MAGNET);
        }
    }

    @EventHandler
    public void onFarmHarvest(TWFarmHarvestEvent event) {
        long keys = rollDrops(event.getAmount(), service.getCatalog().getDropChance(CrateType.NATUREZA));
        if (keys > 0) awardKeys(event.getOwnerId(), CrateType.NATUREZA, keys);
        if (service.isMutantCrop(event.getLocationKey())) {
            long fragments = rollDrops(event.getAmount(), 0.15);
            Player player = Bukkit.getPlayer(event.getOwnerId());
            if (player != null && fragments > 0) rewardDelivery.deliverNatureFragments(player, fragments);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onMutantSeedUse(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK || event.getClickedBlock() == null) return;
        ItemStack seed = event.getItem();
        if (!"mutant_seed".equals(TWItemTags.getString(seed, "tw_crate_effect"))) return;
        event.setCancelled(true);
        Player player = event.getPlayer();
        br.com.tw.farms.TWFarmsPlugin farms = br.com.tw.farms.TWFarmsPlugin.getInstance();
        if (farms == null || farms.getFarmManager() == null) {
            player.sendMessage("§cO módulo de plantações não está disponível.");
            return;
        }
        br.com.tw.farms.models.PhysicalCrop crop = farms.getFarmManager().findCropFor(event.getClickedBlock());
        if (crop == null) {
            player.sendMessage("§cUsa a Semente Mutante numa plantação registada.");
            return;
        }
        if (!crop.getOwnerUuid().equals(player.getUniqueId()) && !player.hasPermission("tw.admin")) {
            player.sendMessage("§cSó o dono da plantação pode mutá-la.");
            return;
        }
        if (service.isMutantCrop(crop.getLocationKey())) {
            player.sendMessage("§cEsta plantação já é mutante.");
            return;
        }
        ItemStack consumedSeed = seed.clone();
        if (seed.getAmount() <= 1) player.getInventory().setItemInMainHand(null);
        else {
            seed.setAmount(seed.getAmount() - 1);
            player.getInventory().setItemInMainHand(seed);
        }
        service.markMutantCrop(crop.getLocationKey(), crop.getOwnerUuid(), success -> {
            if (success) {
                player.sendMessage("§dA plantação tornou-se mutante permanentemente! Fragmentos têm 15% de chance.");
            } else {
                service.storeMail(player.getUniqueId(), consumedSeed);
                player.sendMessage("§cA mutação falhou; a semente foi enviada para o correio.");
            }
        });
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onMachineAndSpawnerConsumableUse(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK || event.getClickedBlock() == null) return;
        ItemStack item = event.getItem();
        String effect = TWItemTags.getString(item, "tw_crate_effect");
        if (!"machine_battery".equals(effect) && !"machine_lubricant".equals(effect)
                && !"machine_overclock".equals(effect) && !"kill_capsule".equals(effect)) return;

        event.setCancelled(true);
        Player player = event.getPlayer();
        long charges = getCharges(item);
        if (charges < 1L) {
            player.sendMessage("§cEste item já não tem cargas.");
            return;
        }
        if ("kill_capsule".equals(effect)) {
            applyKillCapsule(player, event.getClickedBlock().getLocation(), charges);
            return;
        }

        TWMaquinasPlugin machines = TWMaquinasPlugin.getInstance();
        if (machines == null || machines.getMachineManager() == null) {
            player.sendMessage("§cO módulo de máquinas não está disponível.");
            return;
        }
        PlacedMachine machine = machines.getMachineManager().getMachineAt(event.getClickedBlock().getLocation());
        if (machine == null) {
            player.sendMessage("§cUsa este item num bloco de máquina registado.");
            return;
        }
        if (!machine.getOwnerUuid().equals(player.getUniqueId()) && !player.hasPermission("tw.admin")) {
            player.sendMessage("§cSó o dono da máquina pode usar este item.");
            return;
        }

        try {
            if ("machine_battery".equals(effect)) {
                machine.addBatteryCycles(charges);
                player.sendMessage("§bA bateria adicionou §e" + charges + " ciclos gratuitos§b à máquina.");
            } else if ("machine_lubricant".equals(effect)) {
                machine.addLubricantCycles(charges);
                player.sendMessage("§aO lubrificante adicionou §e" + charges + " ciclos triplos§a à máquina.");
            } else {
                double credited = machines.getMachineManager().applyOverclock(machine, charges);
                if (credited <= 0.0) {
                    player.sendMessage("§cO silo da máquina está cheio; o certificado não foi consumido.");
                    return;
                }
                player.sendMessage("§6Overclock concluído: §e$"
                        + br.com.tw.core.utils.TWFormat.formatarDinheiro(credited)
                        + " §6adicionados ao silo.");
            }
            if (!"machine_overclock".equals(effect)) {
                machines.getMachineManager().saveMachineAsync(machine);
            }
            consumeHeldItem(player);
        } catch (ArithmeticException | IllegalArgumentException e) {
            player.sendMessage("§cNão foi possível aplicar o consumível: os valores excedem o limite da máquina.");
        }
    }

    private void applyKillCapsule(Player player, Location location, long kills) {
        if (location.getBlock().getType() != Material.SPAWNER) {
            player.sendMessage("§cUsa a cápsula num spawner registado.");
            return;
        }
        if (kills > 100000L) {
            player.sendMessage("§cA quantidade de abates desta cápsula é inválida.");
            return;
        }
        TWSpawnersPlugin spawners = TWSpawnersPlugin.getInstance();
        if (spawners == null || spawners.getSpawnerManager() == null) {
            player.sendMessage("§cO módulo de spawners não está disponível.");
            return;
        }
        br.com.tw.spawners.managers.SpawnerManager manager = spawners.getSpawnerManager();
        PlacedSpawner spawner = manager.getSpawnerAt(location);
        if (spawner == null) {
            player.sendMessage("§cEste spawner não está registado.");
            return;
        }
        if (!spawner.getOwnerUuid().equals(player.getUniqueId()) && !player.hasPermission("tw.admin")) {
            player.sendMessage("§cSó o dono do spawner pode usar esta cápsula.");
            return;
        }
        SpawnerType type = manager.getSpawnerTypes().get(spawner.getTypeId());
        if (type == null) {
            player.sendMessage("§cO tipo deste spawner não está disponível.");
            return;
        }

        double booster = spawner.hasActiveBooster() ? spawner.getBoosterMultiplier() : 1.0;
        double lootMultiplier = 1.0 + ((spawner.getLootLevel() - 1) * 0.10);
        double money = type.getValuePerKill() * kills * lootMultiplier * booster;
        double xpValue = kills * 2.0 * booster;
        if (!Double.isFinite(money) || !Double.isFinite(xpValue) || xpValue > Long.MAX_VALUE) {
            player.sendMessage("§cA recompensa da cápsula excede o limite permitido.");
            return;
        }

        long tokens = 0L;
        long souls = 0L;
        double tokenChance = Math.min(1.0, type.getTokenChance()
                * (1.0 + ((spawner.getLootLevel() - 1) * 0.05)) * booster);
        double soulChance = Math.min(1.0, 0.001 * booster);
        for (long i = 0; i < kills; i++) {
            if (ThreadLocalRandom.current().nextDouble() < tokenChance) tokens++;
            if (ThreadLocalRandom.current().nextDouble() < soulChance) souls++;
        }

        try {
            double storedMoney = spawner.getStoredMoney() + money;
            if (!Double.isFinite(storedMoney)) throw new ArithmeticException("Saldo do silo excedido.");
            long storedTokens = Math.addExact(spawner.getStoredTokens(), tokens);
            long storedXp = Math.addExact(spawner.getStoredXp(), (long) xpValue);
            long storedSouls = Math.addExact(spawner.getStoredSouls(), souls);
            Math.addExact(spawner.getTotalKills(), kills);
            spawner.setStoredMoney(storedMoney);
            spawner.setStoredTokens(storedTokens);
            spawner.setStoredXp(storedXp);
            spawner.setStoredSouls(storedSouls);
            spawner.addTotalKills(kills);
            manager.saveSpawnerAsync(spawner);
            consumeHeldItem(player);
            player.sendMessage("§cA cápsula simulou §e" + kills + " abates§c; recompensas guardadas no silo.");
        } catch (ArithmeticException e) {
            player.sendMessage("§cO silo atingiu o limite numérico; a cápsula não foi consumida.");
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onFreePrestigeOrbUse(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        ItemStack item = event.getItem();
        if (!"free_prestige".equals(TWItemTags.getString(item, "tw_crate_effect"))) return;
        event.setCancelled(true);

        Player player = event.getPlayer();
        if (getCharges(item) < 1L) {
            player.sendMessage("§cEste orbe já não tem cargas.");
            return;
        }
        TWUser user = TWCorePlugin.getInstance().getUserManager().getUser(player.getUniqueId());
        TWRanksPlugin ranks = TWRanksPlugin.getInstance();
        if (user == null || ranks == null) {
            player.sendMessage("§cOs dados de rank ainda não estão disponíveis.");
            return;
        }
        if (!"MAX".equalsIgnoreCase(user.getRankId())) {
            player.sendMessage("§cPrecisas de alcançar o Rank MAX para usar o orbe.");
            return;
        }
        List<String> hierarchy = TWCorePlugin.getInstance().getPrestigeHierarchy();
        int currentIndex = -1;
        for (int index = 0; index < hierarchy.size(); index++) {
            if (hierarchy.get(index).equalsIgnoreCase(user.getPrestigeId())) {
                currentIndex = index;
                break;
            }
        }
        if (currentIndex < 0 || currentIndex >= hierarchy.size() - 1 || hierarchy.isEmpty()) {
            player.sendMessage("§cJá atingiste o prestígio máximo ou a progressão não está configurada.");
            return;
        }

        String nextPrestige = hierarchy.get(currentIndex + 1);
        user.setPrestigeId(nextPrestige);
        user.setRankId("CARVAO");
        TWCorePlugin.getInstance().getUserManager().saveUserAsync(user);
        Bukkit.dispatchCommand(Bukkit.getConsoleSender(),
                "lp user " + player.getName() + " parent add " + TWLuckPermsGroups.prestige(nextPrestige));
        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "lp user " + player.getName() + " parent set "
                + TWLuckPermsGroups.rank("CARVAO"));
        consumeHeldItem(player);
        player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_ENDER_DRAGON_GROWL, 1.0f, 0.8f);
        player.sendMessage("§dO orbe avançou-te gratuitamente para o prestígio §5" + nextPrestige + "§d.");
        player.sendMessage("§eO teu rank foi resetado para CARVÃO.");
    }

    private void consumeHeldItem(Player player) {
        ItemStack item = player.getInventory().getItemInMainHand();
        if (item == null) return;
        if (item.getAmount() <= 1) player.getInventory().setItemInMainHand(null);
        else {
            item.setAmount(item.getAmount() - 1);
            player.getInventory().setItemInMainHand(item);
        }
        player.updateInventory();
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onActivateMemoryItem(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        ItemStack item = event.getItem();
        CrateEffect effect = CrateEffect.fromId(TWItemTags.getString(item, "tw_crate_effect"));
        if (effect == null) return;
        event.setCancelled(true);
        Player player = event.getPlayer();
        if (!effectMemory.isLoaded(player.getUniqueId())) {
            effectMemory.load(player.getUniqueId());
            player.sendMessage("§eOs teus consumíveis estão a carregar; tenta novamente em instantes.");
            return;
        }
        long charges = getCharges(item);
        if (charges < 1L) {
            player.sendMessage("§cEste item já não tem cargas.");
            return;
        }
        try {
            long total = effectMemory.addCharges(player.getUniqueId(), effect, charges);
            consumeHeldItem(player);
            player.sendMessage("§a" + effect.getDisplayName() + " ativado: §e" + charges
                    + " carga(s) §aadicionadas à tua conta. Total ativo: §e" + total + "§a.");
        } catch (ArithmeticException | IllegalArgumentException e) {
            player.sendMessage("§cNão foi possível ativar o item: o limite de cargas seria excedido.");
        } catch (IllegalStateException e) {
            player.sendMessage("§eOs teus consumíveis ainda estão a carregar; tenta novamente em instantes.");
        }
    }

    public void reloadPhysicalCrates() {
        service.refreshPhysicalCrates(this::reloadPhysicalCrates);
    }

    public void reloadPhysicalCrates(List<PhysicalCrate> crates) {
        removeHolograms();
        physicalCrates.clear();
        for (PhysicalCrate crate : crates) {
            String key = key(crate.getWorld(), crate.getX(), crate.getY(), crate.getZ());
            physicalCrates.put(key, crate.getType());
            org.bukkit.World world = Bukkit.getWorld(crate.getWorld());
            if (world == null) {
                plugin.getLogger().warning("Mundo da caixa física não está carregado: " + crate.getWorld());
                continue;
            }
            Location location = new Location(world, crate.getX() + 0.5, crate.getY() + 1.25, crate.getZ() + 0.5);
            ArmorStand hologram = world.spawn(location, ArmorStand.class);
            hologram.setVisible(false);
            hologram.setGravity(false);
            hologram.setSmall(true);
            hologram.setCustomNameVisible(true);
            hologram.setCustomName(service.getCatalog().get(crate.getType()).getDisplayName()
                    + " §7- clique para abrir");
            hologram.setMetadata(HOLOGRAM_METADATA, new FixedMetadataValue(plugin, true));
            holograms.add(hologram);
        }
    }

    public void removeHolograms() {
        for (org.bukkit.World world : Bukkit.getWorlds()) {
            for (Entity entity : world.getEntities()) {
                if (entity instanceof ArmorStand && entity.hasMetadata(HOLOGRAM_METADATA)) entity.remove();
            }
        }
        holograms.clear();
    }

    private String key(String world, int x, int y, int z) {
        return world.toLowerCase(java.util.Locale.ROOT) + ";" + x + ";" + y + ";" + z;
    }

    private String blockKey(Block block) {
        return key(block.getWorld().getName(), block.getX(), block.getY(), block.getZ());
    }

    private long getCharges(ItemStack item) {
        return TWItemTags.getLong(item, "tw_crate_charges", 0L);
    }

    private boolean multiplyPayout(TWBlockMinedEvent event, double multiplier) {
        double payout = event.getPayout() * multiplier;
        if (!Double.isFinite(payout)) return false;
        event.setPayout(payout);
        return true;
    }

    private void notifyEffectMilestone(Player player, CrateEffect effect) {
        long remaining = effectMemory.getCharges(player.getUniqueId(), effect);
        if (remaining == 0L) {
            player.sendMessage("§e" + effect.getDisplayName() + " §cficou sem cargas.");
        } else if (remaining % 1000L == 0L || remaining <= 5L) {
            player.sendMessage("§e" + effect.getDisplayName() + ": §b" + remaining + " §ecargas restantes.");
        }
    }

    private long rollDrops(long amount, double chance) {
        if (amount < 1 || chance <= 0.0) return 0;
        if (chance >= 1.0) return Math.min(amount, 100L);
        long successes = 0;
        long remaining = amount;
        double logFailure = Math.log1p(-chance);
        while (remaining > 0 && successes < 100L) {
            double random = Math.max(1.0E-12, ThreadLocalRandom.current().nextDouble());
            long gap = (long) Math.floor(Math.log(random) / logFailure);
            if (gap >= remaining) break;
            successes++;
            remaining -= gap + 1L;
        }
        return successes;
    }

    private void awardKeys(java.util.UUID playerId, CrateType type, long amount) {
        service.grantKeys(playerId, type, amount, success -> {
            if (!success) return;
            Player player = Bukkit.getPlayer(playerId);
            if (player != null) player.sendMessage("§eRecebeste §6" + amount + " chave(s) " + type.getId() + "§e!");
        });
    }

}
