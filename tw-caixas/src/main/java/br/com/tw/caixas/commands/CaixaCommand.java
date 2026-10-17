package br.com.tw.caixas.commands;

import br.com.tw.caixas.TWCaixasPlugin;
import br.com.tw.caixas.listeners.CrateListener;
import br.com.tw.caixas.managers.CrateService;
import br.com.tw.caixas.model.CrateEffect;
import br.com.tw.caixas.model.CrateType;
import br.com.tw.core.utils.TWItemTags;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.HashMap;
import java.util.Map;

public final class CaixaCommand implements CommandExecutor {
    private final TWCaixasPlugin plugin;
    private final CrateService service;
    private final CrateListener listener;

    public CaixaCommand(TWCaixasPlugin plugin, CrateService service, CrateListener listener) {
        this.plugin = plugin;
        this.service = service;
        this.listener = listener;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("tw.admin")) {
            sender.sendMessage("§cNão tens permissão para esse comando.");
            return true;
        }
        if (args.length == 0) {
            sender.sendMessage("§cUso: /caixa <dar|setar|remover|daritem|reload>");
            return true;
        }
        switch (args[0].toLowerCase(java.util.Locale.ROOT)) {
            case "dar": give(sender, args); break;
            case "setar": setPhysical(sender, args); break;
            case "remover": removePhysical(sender, args); break;
            case "daritem": giveItem(sender, args); break;
            case "reload": reload(sender); break;
            default: sender.sendMessage("§cUso: /caixa <dar|setar|remover|daritem|reload>");
        }
        return true;
    }

    private void give(CommandSender sender, String[] args) {
        if (args.length != 4) {
            sender.sendMessage("§cUso: /caixa dar <jogador> <tipo> <quantidade>");
            return;
        }
        OfflinePlayer target = Bukkit.getOfflinePlayer(args[1]);
        CrateType type = CrateType.fromId(args[2]);
        long amount = parseNonNegativeLong(args[3]);
        if (target.getUniqueId() == null || (!target.isOnline() && !target.hasPlayedBefore())
                || type == null || amount < 1) {
            sender.sendMessage("§cJogador, tipo ou quantidade inválidos.");
            return;
        }
        service.grantKeys(target.getUniqueId(), type, amount, success -> {
            if (!success) {
                sender.sendMessage("§cNão foi possível entregar as chaves.");
                return;
            }
            sender.sendMessage("§aEntregaste " + amount + " chaves " + type.getId() + " a " + target.getName() + ".");
            Player online = target.getPlayer();
            if (online != null) online.sendMessage("§aRecebeste §e" + amount + " §achaves " + type.getId() + ".");
        });
    }

    private void setPhysical(CommandSender sender, String[] args) {
        if (!(sender instanceof Player) || args.length != 2) {
            sender.sendMessage("§cUso: /caixa setar <tipo> (olha para o bloco desejado)");
            return;
        }
        Player player = (Player) sender;
        CrateType type = CrateType.fromId(args[1]);
        Block block = player.getTargetBlock((java.util.HashSet<Byte>) null, 6);
        if (type == null || block == null || block.getType() == Material.AIR) {
            sender.sendMessage("§cTipo inválido ou nenhum bloco alvo.");
            return;
        }
        service.addPhysicalCrate(block.getWorld().getName(), block.getX(), block.getY(), block.getZ(), type, success -> {
            if (success) {
                listener.reloadPhysicalCrates();
                player.sendMessage("§aCaixa física " + type.getId() + " configurada.");
            } else player.sendMessage("§cNão foi possível configurar a caixa.");
        });
    }

    private void removePhysical(CommandSender sender, String[] args) {
        if (!(sender instanceof Player) || args.length != 1) {
            sender.sendMessage("§cUso: /caixa remover (olha para a caixa física)");
            return;
        }
        Player player = (Player) sender;
        Block block = player.getTargetBlock((java.util.HashSet<Byte>) null, 6);
        if (block == null) {
            player.sendMessage("§cNenhum bloco alvo.");
            return;
        }
        service.removePhysicalCrate(block.getWorld().getName(), block.getX(), block.getY(), block.getZ(), success -> {
            listener.reloadPhysicalCrates();
            player.sendMessage(success ? "§aCaixa física removida." : "§cNão há caixa configurada nesse bloco.");
        });
    }

    private void giveItem(CommandSender sender, String[] args) {
        if (args.length < 3 || args.length > 4) {
            sender.sendMessage("§cUso: /caixa daritem <jogador> <efeito> [cargas]");
            return;
        }
        Player target = Bukkit.getPlayerExact(args[1]);
        long charges = args.length == 4 ? parseNonNegativeLong(args[3]) : 1;
        String effect = args[2].toLowerCase(java.util.Locale.ROOT);
        if (!isSupportedEffect(effect)) {
            sender.sendMessage("§cEfeito desconhecido ou sem uso implementado: " + effect + ".");
            return;
        }
        if (target == null || charges < 1 || target.getInventory().firstEmpty() < 0) {
            sender.sendMessage("§cJogador online, cargas positivas e espaço no inventário são necessários.");
            return;
        }
        if ("farm_growth".equals(effect) && charges != 1L) {
            sender.sendMessage("§cO booster de crescimento é um item de uso único; usa 1 carga.");
            return;
        }
        ItemStack item;
        try {
            item = createSpecialItem(effect, charges);
        } catch (RuntimeException e) {
            plugin.getLogger().log(java.util.logging.Level.SEVERE,
                    "Não foi possível criar o consumível " + effect + ".", e);
            sender.sendMessage("§cNão foi possível criar este item; verifica o log do servidor.");
            return;
        }
        Map<String, Object> tags = new HashMap<>();
        tags.put("tw_crate_effect", effect);
        tags.put("tw_crate_charges", charges);
        tags.put("tw_crate_instance", java.util.UUID.randomUUID().toString());
        target.getInventory().addItem(TWItemTags.set(item, tags));
        sender.sendMessage("§aItem entregue a " + target.getName() + ".");
    }

    private ItemStack createSpecialItem(String effect, long charges) {
        if ("farm_growth".equals(effect) || "golden_scythe".equals(effect)) {
            br.com.tw.farms.TWFarmsPlugin farms = br.com.tw.farms.TWFarmsPlugin.getInstance();
            if (farms == null || farms.getFarmManager() == null) {
                throw new IllegalStateException("tw-farms não está disponível para criar este item.");
            }
            return "golden_scythe".equals(effect)
                    ? farms.getFarmManager().createHarvesterHoe()
                    : farms.getFarmManager().createBoosterItem(2.0, 1800L);
        }
        ItemStack item = new ItemStack(Material.PAPER);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName("§6Item especial: " + effect);
        meta.setLore(java.util.Collections.singletonList("§7Cargas: " + charges));
        item.setItemMeta(meta);
        return item;
    }

    private boolean isSupportedEffect(String effect) {
        return CrateEffect.fromId(effect) != null || "kill_capsule".equals(effect)
                || "free_spawner_upgrade".equals(effect) || "machine_battery".equals(effect)
                || "machine_lubricant".equals(effect) || "machine_overclock".equals(effect)
                || "farm_growth".equals(effect) || "golden_scythe".equals(effect)
                || "mutant_seed".equals(effect) || "divine_seal".equals(effect)
                || "free_prestige".equals(effect);
    }

    private void reload(CommandSender sender) {
        try {
            plugin.reloadConfig();
            service.setCatalog(br.com.tw.caixas.config.CrateCatalog.load(plugin));
            listener.reloadPhysicalCrates();
            sender.sendMessage("§aConfiguração de caixas recarregada.");
        } catch (RuntimeException e) {
            plugin.getLogger().log(java.util.logging.Level.SEVERE, "Falha ao recarregar configuração das caixas.", e);
            sender.sendMessage("§cConfiguração inválida; verifica o log. A configuração anterior foi mantida.");
        }
    }

    private long parseNonNegativeLong(String raw) {
        try {
            long amount = Long.parseLong(raw);
            return amount >= 0 ? amount : -1;
        } catch (NumberFormatException e) {
            return -1;
        }
    }
}
