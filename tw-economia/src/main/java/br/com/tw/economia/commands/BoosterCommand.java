package br.com.tw.economia.commands;

import br.com.tw.economia.TWEconomiaPlugin;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class BoosterCommand implements CommandExecutor {
    private final TWEconomiaPlugin plugin;

    public BoosterCommand(TWEconomiaPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("tw.admin")) {
            sender.sendMessage(ChatColor.RED + "Sem permissão.");
            return true;
        }

        if (args.length < 1) {
            sender.sendMessage(ChatColor.RED + "Uso correto: /booster <multiplicador> (Ex: /booster 2.0)");
            sender.sendMessage(ChatColor.YELLOW + "Booster atual do servidor: " + ChatColor.AQUA + plugin.getGlobalBooster() + "x");
            return true;
        }

        try {
            double multiplier = Double.parseDouble(args[0]);
            if (!Double.isFinite(multiplier) || multiplier <= 0.0) {
                sender.sendMessage(ChatColor.RED + "O multiplicador deve ser um número finito maior que zero.");
                return true;
            }
            plugin.setGlobalBooster(multiplier);

            Bukkit.broadcastMessage("");
            Bukkit.broadcastMessage(ChatColor.AQUA + "§l[TW-ECONOMIA] §a§lEVENTO DE BOOSTER ATIVADO!");
            Bukkit.broadcastMessage(ChatColor.GREEN + "Todos os minérios quebrados agora valem §e§l" + multiplier + "x §amais!");
            Bukkit.broadcastMessage("");

            for (Player p : Bukkit.getOnlinePlayers()) {
                p.playSound(p.getLocation(), Sound.ENTITY_ENDER_DRAGON_GROWL, 0.5f, 1.2f);
            }

        } catch (NumberFormatException e) {
            sender.sendMessage(ChatColor.RED + "Número inválido. Use formato com ponto, ex: 1.5");
        }
        return true;
    }
}