package br.com.tw.economia.commands;

import br.com.tw.core.TWCorePlugin;
import br.com.tw.core.data.TWUser;
import br.com.tw.economia.TWEconomiaPlugin;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Map;

public class BaltopCommand implements CommandExecutor {
    private final TWEconomiaPlugin plugin;

    public BaltopCommand(TWEconomiaPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        sender.sendMessage(ChatColor.YELLOW + "A sincronizar e procurar os magnatas do servidor...");

        // Corre tudo fora da thread principal para ZERO lag
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {

            // 1. Sincroniza o dinheiro de todos os jogadores online com o MySQL primeiro
            for (Player p : Bukkit.getOnlinePlayers()) {
                TWUser u = TWCorePlugin.getInstance().getUserManager().getUser(p.getUniqueId());
                if (u != null) {
                    // Usa o save direto do repositório porque já estamos numa thread assíncrona
                    TWCorePlugin.getInstance().getUserManager().getRepository().saveUser(u);
                }
            }

            // 2. Agora puxa o ranking atualizado do MySQL
            Map<String, Double> top = TWCorePlugin.getInstance().getUserManager().getTopBalances(10);

            // 3. Volta à thread principal para enviar a mensagem
            Bukkit.getScheduler().runTask(plugin, () -> {
                sender.sendMessage(ChatColor.GOLD + "§m---§r " + ChatColor.YELLOW + "§l[ Top Magnatas ]" + ChatColor.GOLD + " §m---");
                int pos = 1;
                for (Map.Entry<String, Double> entry : top.entrySet()) {
                    String formatted = br.com.tw.core.utils.TWFormat.formatarDinheiro(entry.getValue());
                    sender.sendMessage(ChatColor.GOLD + "" + pos + "º " + ChatColor.WHITE + entry.getKey() + ChatColor.GREEN + " - $" + formatted);
                    pos++;
                }
                sender.sendMessage(ChatColor.GOLD + "§m------------------------");
            });
        });
        return true;
    }
}