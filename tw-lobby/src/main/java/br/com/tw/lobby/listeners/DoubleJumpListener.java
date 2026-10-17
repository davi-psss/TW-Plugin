package br.com.tw.lobby.listeners;

import br.com.tw.core.TWCorePlugin;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerToggleFlightEvent;

public class DoubleJumpListener implements Listener {
    @EventHandler
    public void onMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();

        // Ignora quem está no mundo das minas ou em criativo
        if (TWCorePlugin.getInstance().isMineWorld(player.getWorld().getName())) return;
        if (player.getGameMode() == GameMode.CREATIVE) return;

        // Se o jogador estiver no chão (bloco de baixo não é AR)
        if (player.getLocation().getBlock().getRelative(BlockFace.DOWN).getType() != Material.AIR) {
            player.setAllowFlight(true); // Permite o pulo
        }
    }

    @EventHandler
    public void onDoubleJump(PlayerToggleFlightEvent event) {
        Player player = event.getPlayer();

        // Ignora quem está no mundo das minas ou em criativo
        if (TWCorePlugin.getInstance().isMineWorld(player.getWorld().getName())) return;
        if (player.getGameMode() == GameMode.CREATIVE) return;

        // Cancela o voo normal
        event.setCancelled(true);
        player.setAllowFlight(false);
        player.setFlying(false);

        // Aplica a física do Pulo Duplo
        player.setVelocity(player.getLocation().getDirection().multiply(1.5).setY(1.0));
        player.playSound(player.getLocation(), Sound.BAT_TAKEOFF, 1.0f, 1.2f);
    }
}