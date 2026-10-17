package br.com.tw.lobby.listeners;

import br.com.tw.core.TWCorePlugin;
import br.com.tw.lobby.TWLobbyPlugin;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.weather.WeatherChangeEvent;

public class ProtecaoListener implements Listener {
    // 1. Proteção Geral do Mundo (Apenas permite alterar coisas se for Admin no Criativo)
    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        Player p = event.getPlayer();
        if (p.hasPermission("tw.admin") && p.getGameMode() == GameMode.CREATIVE) return;
        String worldName = p.getWorld().getName();
        // Bloqueia quebra no Lobby; nas Minas o tw-minas trata; nos Terrenos o PlotSquared/tw-maquinas/tw-spawners tratam
        if (worldName.equalsIgnoreCase(TWLobbyPlugin.getInstance().getLobbyWorldName())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        Player p = event.getPlayer();
        if (p.hasPermission("tw.admin") && p.getGameMode() == GameMode.CREATIVE) return;
        String worldName = p.getWorld().getName();
        // Bloqueia colocar blocos na Mina e no Lobby, mas permite no mundo de Terrenos
        if (TWCorePlugin.getInstance().isMineWorld(worldName)
                || worldName.equalsIgnoreCase(TWLobbyPlugin.getInstance().getLobbyWorldName())) {
            event.setCancelled(true);
        }
    }

    // 2. Bloqueia Danos e Fome
    @EventHandler
    public void onDamage(EntityDamageEvent event) {
        if (event.getEntity() instanceof Player) {
            // Se o dano for do Vazio (Void), teleporta em vez de matar
            if (event.getCause() == EntityDamageEvent.DamageCause.VOID) {
                event.setCancelled(true);
                event.getEntity().teleport(TWLobbyPlugin.getInstance().getLobbyLocation());
                return;
            }
            event.setCancelled(true); // Bloqueia todos os outros danos (queda, monstros, etc)
        }
    }

    @EventHandler
    public void onFood(FoodLevelChangeEvent event) {
        event.setCancelled(true); // Jogador nunca perde fome
    }

    // 3. Mantém sempre sol no servidor
    @EventHandler
    public void onWeather(WeatherChangeEvent event) {
        if (event.toWeatherState()) { // Se for chover
            event.setCancelled(true);
        }
    }

    // 4. Bloqueia Dropar e Mexer na Picareta da Mina
    @EventHandler
    public void onDrop(PlayerDropItemEvent event) {
        if (TWCorePlugin.getInstance().isMineWorld(event.getPlayer().getWorld().getName())) {
            if (event.getItemDrop().getItemStack().getType() == Material.DIAMOND_PICKAXE) {
                event.setCancelled(true);
                event.getPlayer().sendMessage(ChatColor.RED + "Não podes dropar a picareta!");
            }
        }
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (TWCorePlugin.getInstance().isMineWorld(event.getWhoClicked().getWorld().getName())) {
            if (event.getCurrentItem() != null && event.getCurrentItem().getType() == Material.DIAMOND_PICKAXE) {
                event.setCancelled(true);
                ((Player) event.getWhoClicked()).sendMessage(ChatColor.RED + "Não podes mover a picareta!");
            }
        }
    }
}