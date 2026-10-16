package br.com.tw.ranks.commands;

import br.com.tw.core.TWCorePlugin;
import br.com.tw.core.data.TWUser;
import br.com.tw.ranks.TWRanksPlugin;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.util.Arrays;
import java.util.List;

public class PrestigioCommand implements CommandExecutor {
    private final TWRanksPlugin plugin;
    private final FileConfiguration ranksConfig;
    // Ordem baseada nos dados padrão do seu banco e do ranks.yml
    private final List<String> prestigeOrder = Arrays.asList("NENHUM", "GRINCH", "YETI", "KRAMPUS");

    public PrestigioCommand(TWRanksPlugin plugin) {
        this.plugin = plugin;
        File file = new File(plugin.getDataFolder(), "ranks.yml");
        this.ranksConfig = YamlConfiguration.loadConfiguration(file);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) return true;
        Player player = (Player) sender;

        TWUser user = TWCorePlugin.getInstance().getUserManager().getUser(player.getUniqueId());
        if (user == null) {
            player.sendMessage(ChatColor.RED + "Os teus dados ainda estão a ser carregados. Tenta novamente!");
            return true;
        }

        // Verifica se o jogador chegou no Rank MAX
        if (!user.getRankId().equalsIgnoreCase("MAX")) {
            player.sendMessage(ChatColor.RED + "Precisas de alcançar o Rank MAX para dar Prestígio!");
            return true;
        }

        String currentPrestige = user.getPrestigeId();
        int currentIndex = prestigeOrder.indexOf(currentPrestige.toUpperCase());

        if (currentIndex == -1 || currentIndex >= prestigeOrder.size() - 1) {
            player.sendMessage(ChatColor.AQUA + "Já atingiste o Prestígio Máximo do servidor!");
            return true;
        }

        String nextPrestige = prestigeOrder.get(currentIndex + 1);
        double cost = ranksConfig.getDouble("prestiges." + nextPrestige + ".cost");

        Economy econ = Bukkit.getServer().getServicesManager().getRegistration(Economy.class).getProvider();
        double balance = econ.getBalance(player);

        if (balance >= cost) {
            if (econ.withdrawPlayer(player, cost).transactionSuccess()) {
                // Atualiza o prestígio e reseta o rank
                user.setPrestigeId(nextPrestige);
                user.setRankId("CARVAO");

                // Salva os dados imediatamente (ver Passo 2 abaixo)
                TWCorePlugin.getInstance().getUserManager().saveUserAsync(user);

                // Atualiza o LuckPerms
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "lp user " + player.getName() + " parent add prestigio_" + nextPrestige.toLowerCase());
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "lp user " + player.getName() + " parent set rank_carvao");

                player.sendMessage(ChatColor.GREEN + "Evoluíste para o Prestígio " + ChatColor.GOLD + ChatColor.BOLD + nextPrestige + ChatColor.GREEN + "!");
                player.sendMessage(ChatColor.YELLOW + "O teu Rank foi resetado para CARVÃO.");
                player.playSound(player.getLocation(), Sound.ENDERDRAGON_GROWL, 1.0f, 0.8f);
            } else {
                player.sendMessage(ChatColor.RED + "Erro na transação. Contacta um Administrador.");
            }
        } else {
            double missing = cost - balance;
            player.sendMessage(ChatColor.RED + "Faltam $" + String.format("%,.2f", missing) + " para evoluir para o Prestígio " + nextPrestige + ".");
        }
        return true;
    }
}