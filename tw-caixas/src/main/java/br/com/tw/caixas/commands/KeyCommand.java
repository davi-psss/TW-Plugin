package br.com.tw.caixas.commands;

import br.com.tw.caixas.TWCaixasPlugin;
import br.com.tw.caixas.data.CrateAccount;
import br.com.tw.caixas.managers.CrateOpenings;
import br.com.tw.caixas.managers.CrateService;
import br.com.tw.caixas.managers.PlayerEffectMemory;
import br.com.tw.caixas.menus.KeyForgeMenu;
import br.com.tw.caixas.menus.KeyPanelMenu;
import br.com.tw.caixas.model.CrateEffect;
import br.com.tw.caixas.model.CrateType;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public final class KeyCommand implements CommandExecutor {
    private final TWCaixasPlugin plugin;
    private final CrateService service;
    private final CrateOpenings openings;
    private final PlayerEffectMemory effectMemory;

    public KeyCommand(TWCaixasPlugin plugin, CrateService service, CrateOpenings openings,
                      PlayerEffectMemory effectMemory) {
        this.plugin = plugin;
        this.service = service;
        this.openings = openings;
        this.effectMemory = effectMemory;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("§cEste comando só pode ser usado no jogo.");
            return true;
        }
        Player player = (Player) sender;
        if (args.length == 0) {
            KeyPanelMenu.open(plugin, service, openings, player);
            return true;
        }
        if ("forja".equalsIgnoreCase(args[0])) {
            service.loadAccount(player.getUniqueId(), account ->
                    player.openInventory(new KeyForgeMenu(plugin, service, player, account).getInventory()),
                    () -> player.sendMessage("§cNão foi possível carregar a tua conta de chaves."));
            return true;
        }
        if ("enviar".equalsIgnoreCase(args[0])) {
            transfer(player, args);
            return true;
        }
        if ("abrir".equalsIgnoreCase(args[0])) {
            open(player, args);
            return true;
        }
        if ("correio".equalsIgnoreCase(args[0])) {
            service.claimMail(player.getUniqueId(), items -> {
                if (!player.isOnline()) {
                    service.storeMail(player.getUniqueId(), items);
                    return;
                }
                if (items.isEmpty()) {
                    player.sendMessage("§7Não tens itens guardados no correio.");
                    return;
                }
                java.util.List<org.bukkit.inventory.ItemStack> leftovers = new java.util.ArrayList<>();
                for (org.bukkit.inventory.ItemStack item : items) {
                    leftovers.addAll(player.getInventory().addItem(item).values());
                }
                player.sendMessage("§aRecolheste " + (items.size() - leftovers.size()) + " pilhas do correio.");
                if (!leftovers.isEmpty()) service.storeMail(player.getUniqueId(), leftovers);
            }, () -> player.sendMessage("§cNão foi possível carregar o correio."));
            return true;
        }
        if ("efeitos".equalsIgnoreCase(args[0])) {
            showEffects(player);
            return true;
        }
        player.sendMessage("§cUso: /key [forja|correio|efeitos|enviar <jogador> <tipo> <quantidade>|abrir <tipo> [quantidade]]");
        return true;
    }

    private void showEffects(Player player) {
        java.util.Map<CrateEffect, Long> active = effectMemory.getSnapshot(player.getUniqueId());
        if (active == null) {
            effectMemory.load(player.getUniqueId());
            player.sendMessage("§eOs teus consumíveis estão a carregar; tenta novamente em instantes.");
            return;
        }
        boolean found = false;
        for (CrateEffect effect : CrateEffect.values()) {
            long charges = active.get(effect);
            if (charges < 1L) continue;
            player.sendMessage("§e" + effect.getDisplayName() + ": §b" + charges + " carga(s)");
            found = true;
        }
        if (!found) player.sendMessage("§7Não tens consumíveis ativos guardados.");
    }

    private void transfer(Player sender, String[] args) {
        if (args.length != 4) {
            sender.sendMessage("§cUso: /key enviar <jogador> <tipo> <quantidade>");
            return;
        }
        Player target = Bukkit.getPlayerExact(args[1]);
        CrateType type = CrateType.fromId(args[2]);
        long amount = parsePositiveLong(args[3]);
        if (target == null || type == null || amount < 1) {
            sender.sendMessage("§cJogador deve estar online; tipo ou quantidade inválidos.");
            return;
        }
        if (target.getUniqueId().equals(sender.getUniqueId())) {
            sender.sendMessage("§cNão podes enviar chaves para ti mesmo.");
            return;
        }
        service.transferKeys(sender.getUniqueId(), target.getUniqueId(), type, amount, success -> {
            if (success) {
                sender.sendMessage("§aEnviaste §e" + amount + " §achaves " + type.getId() + " para " + target.getName() + ".");
                target.sendMessage("§aRecebeste §e" + amount + " §achaves " + type.getId() + " de " + sender.getName() + ".");
            } else {
                sender.sendMessage("§cTransferência falhou; confirma se tens chaves suficientes.");
            }
        });
    }

    private void open(Player player, String[] args) {
        if (args.length < 2 || args.length > 3) {
            player.sendMessage("§cUso: /key abrir <tipo> [quantidade]");
            return;
        }
        CrateType type = CrateType.fromId(args[1]);
        if (type == null) {
            player.sendMessage("§cTipos: mineira, sanguinaria, tecnologica, natureza, divina.");
            return;
        }
        if (args.length == 2) {
            openings.openManual(player, type);
            return;
        }
        long count = parsePositiveLong(args[2]);
        if (count < 1 || count > Integer.MAX_VALUE) {
            player.sendMessage("§cQuantidade inválida.");
            return;
        }
        openings.openBulk(player, type, (int) count);
    }

    private long parsePositiveLong(String raw) {
        try {
            long amount = Long.parseLong(raw);
            return amount > 0 ? amount : -1;
        } catch (NumberFormatException e) {
            return -1;
        }
    }
}
