package br.com.tw.ranks.commands;

import br.com.tw.core.TWCorePlugin;
import br.com.tw.core.data.TWUser;
import br.com.tw.core.utils.TWLuckPermsGroups;
import br.com.tw.ranks.TWRanksPlugin;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

import java.util.List;

public class PrestigioCommand implements CommandExecutor {
    private final FileConfiguration ranksConfig;
    // Ordem baseada nos dados padrão do seu banco e do ranks.yml
    private final List<String> prestigeOrder;

    public PrestigioCommand(TWRanksPlugin plugin) {
        this.ranksConfig = plugin.getRanksConfig();
        this.prestigeOrder = TWCorePlugin.getInstance().getPrestigeHierarchy();
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
        int currentIndex = -1;
        for (int i = 0; i < prestigeOrder.size(); i++) {
            if (prestigeOrder.get(i).equalsIgnoreCase(currentPrestige)) {
                currentIndex = i;
                break;
            }
        }

        if (currentIndex >= prestigeOrder.size() - 1) {
            player.sendMessage(ChatColor.AQUA + "Já atingiste o Prestígio Máximo do servidor!");
            return true;
        }

        String nextPrestige = prestigeOrder.get(currentIndex + 1);
        double cost = ranksConfig.getDouble("prestiges." + nextPrestige + ".cost");

        double balance = user.getBalance();

        if (balance >= cost) {
            if (user.removeBalance(cost)) {
                // Atualiza o prestígio e reseta o rank
                user.setPrestigeId(nextPrestige);
                user.setRankId("CARVAO");

                // Salva os dados imediatamente (ver Passo 2 abaixo)
                TWCorePlugin.getInstance().getUserManager().saveUserAsync(user);

                // Atualiza o LuckPerms
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "lp user " + player.getName() + " parent add "
                        + TWLuckPermsGroups.prestige(nextPrestige));
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "lp user " + player.getName() + " parent set "
                        + TWLuckPermsGroups.rank("CARVAO"));

                player.sendMessage(ChatColor.GREEN + "Evoluíste para o Prestígio " + ChatColor.GOLD + ChatColor.BOLD + nextPrestige + ChatColor.GREEN + "!");
                player.sendMessage(ChatColor.YELLOW + "O teu Rank foi resetado para CARVÃO.");
                player.playSound(player.getLocation(), Sound.ENTITY_ENDER_DRAGON_GROWL, 1.0f, 0.8f);
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