package br.com.tw.lobby.listeners;

import br.com.tw.lobby.TWLobbyPlugin;
import br.com.tw.lobby.managers.InventoryManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class MundoListener implements Listener {

    private final InventoryManager inventoryManager;
    private final String MINAS_WORLD = "minas";

    public MundoListener(InventoryManager inventoryManager) {
        this.inventoryManager = inventoryManager;
    }

    @EventHandler
    public void onWorldChange(PlayerChangedWorldEvent event) {
        Player player = event.getPlayer();
        String to = player.getWorld().getName();
        String from = event.getFrom().getName();

        if (to.equalsIgnoreCase(MINAS_WORLD)) {
            // Entrou na mina: Salva e entrega picareta instantaneamente
            inventoryManager.saveAndClearInventory(player);
            player.sendMessage(ChatColor.AQUA + "⚒ Os teus itens foram guardados no Lobby.");

        } else if (from.equalsIgnoreCase(MINAS_WORLD)) {
            // SAIU DA MINA: Adicionamos um delay de meio segundo (10 ticks)
            // Isto garante que o Essentials termina o teleporte ANTES de mexermos no inventário
            Bukkit.getScheduler().runTaskLater(TWLobbyPlugin.getInstance(), () -> {
                if (!player.isOnline()) return; // Prevenção extra

                // Limpeza forçada (Garante que a picareta some)
                player.getInventory().clear();
                player.getInventory().setArmorContents(null);

                // Restaura os itens e atualiza a tela
                if (inventoryManager.restoreInventory(player)) {
                    player.updateInventory(); // Corrige bugs visuais
                    player.playSound(player.getLocation(), Sound.LEVEL_UP, 1f, 1f);
                    player.sendMessage(ChatColor.GREEN + "Os teus itens do Lobby foram restaurados!");
                }
            }, 10L); // 10L = 0.5 segundos
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();

        // Delay de 20 ticks (1 segundo) para evitar conflitos com plugins de Login (AuthMe)
        Bukkit.getScheduler().runTaskLater(TWLobbyPlugin.getInstance(), () -> {
            if (!player.isOnline()) return;

            if (player.getWorld().getName().equalsIgnoreCase(MINAS_WORLD)) {
                // 1. Puxa ele para o Lobby principal
                Location spawn = TWLobbyPlugin.getInstance().getLobbyLocation();
                player.teleport(spawn);

                // 2. Limpa a picareta forçadamente
                player.getInventory().clear();
                player.getInventory().setArmorContents(null);
                player.updateInventory();

                // 3. Tenta restaurar
                if (inventoryManager.restoreInventory(player)) {
                    player.playSound(player.getLocation(), Sound.LEVEL_UP, 1f, 1f);
                    player.sendMessage(ChatColor.GREEN + "Os teus itens do Lobby foram restaurados!");
                }
            } else {
                // Se já logou no lobby, apenas tenta restaurar
                if (inventoryManager.restoreInventory(player)) {
                    player.playSound(player.getLocation(), Sound.LEVEL_UP, 1f, 1f);
                    player.sendMessage(ChatColor.GREEN + "Os teus itens do Lobby foram restaurados!");
                }
            }
        }, 20L);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        if (player.getWorld().getName().equalsIgnoreCase(MINAS_WORLD)) {
            // Remove a picareta assim que o jogador desconecta
            player.getInventory().clear();
            player.getInventory().setArmorContents(null);
        }
    }
}