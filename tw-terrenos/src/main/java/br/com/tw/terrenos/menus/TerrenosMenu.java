package br.com.tw.terrenos.menus;

import br.com.tw.core.menus.TWMenu;
import br.com.tw.core.TWCorePlugin;
import br.com.tw.core.data.TWUser;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Arrays;

public class TerrenosMenu implements TWMenu {
    private final Inventory inventory;
    private final Player player;
    private final double PRECO_TERRENO = 50000.0; // O valor que quer cobrar

    public TerrenosMenu(Player player) {
        this.player = player;
        this.inventory = Bukkit.createInventory(this, 27, ChatColor.GREEN + "§lAgência de Terrenos");
        setupItems();
    }

    private void setupItems() {
        // Botão de Comprar Terreno
        ItemStack comprar = new ItemStack(Material.GRASS_BLOCK);
        ItemMeta comprarMeta = comprar.getItemMeta();
        comprarMeta.setDisplayName("§a§lComprar Novo Terreno");
        comprarMeta.setLore(Arrays.asList(
                "§7Adquire um espaço seguro para",
                "§7construir a tua base e máquinas.",
                "",
                "§7Preço: §2$" + br.com.tw.core.utils.TWFormat.formatarDinheiro(PRECO_TERRENO),
                "",
                "§eClica para adquirir!"
        ));
        comprar.setItemMeta(comprarMeta);
        inventory.setItem(11, comprar);

        // Botão de Ir para o Terreno
        ItemStack teleportar = new ItemStack(Material.ENDER_PEARL);
        ItemMeta teleportarMeta = teleportar.getItemMeta();
        teleportarMeta.setDisplayName("§b§lIr para o meu Terreno");
        teleportarMeta.setLore(Arrays.asList(
                "§7Teleporte instantâneo para a",
                "§7tua propriedade principal.",
                "",
                "§eClica para viajar!"
        ));
        teleportar.setItemMeta(teleportarMeta);
        inventory.setItem(15, teleportar);
    }

    @Override
    public void onClick(InventoryClickEvent event) {
        int slot = event.getSlot();

        if (slot == 11) { // Comprar
            player.closeInventory();
            TWUser user = TWCorePlugin.getInstance().getUserManager().getUser(player.getUniqueId());

            if (user != null) {
                if (user.getBalance() >= PRECO_TERRENO) {
                    // Nós não descontamos o dinheiro AQUI.
                    // Vamos dar a permissão e forçar o comando.
                    // Se o PlotSquared disser que ele já atingiu o limite, ele não perde dinheiro.

                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "lp user " + player.getName() + " permission settemp plots.auto true 3s");

                    // Pequeno delay para o LuckPerms processar a permissão
                    Bukkit.getScheduler().runTaskLater(TWCorePlugin.getInstance(), () -> {
                        player.performCommand("p auto");

                        // NOTA: Como não estamos a usar a API do PlotSquared,
                        // a cobrança será feita através de um evento no próximo passo.
                    }, 5L);

                } else {
                    player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
                    player.sendMessage(ChatColor.RED + "Não tens $" + br.com.tw.core.utils.TWFormat.formatarDinheiro(PRECO_TERRENO) + " para comprar um terreno.");
                }
            }
        }
        else if (slot == 15) { // Ir para o terreno
            player.closeInventory();
            player.performCommand("p h");
            player.playSound(player.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 1f, 1f);
        }
    }

    @Override
    public Inventory getInventory() { return inventory; }

    public void open() {
        player.openInventory(inventory);
        player.playSound(player.getLocation(), Sound.BLOCK_CHEST_OPEN, 0.5f, 1f);
    }
}