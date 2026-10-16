package br.com.tw.lobby.managers;

import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;

public class InventoryManager {
    private final File file;
    private final FileConfiguration config;

    public InventoryManager(JavaPlugin plugin) {
        file = new File(plugin.getDataFolder(), "inventories.yml");
        if (!file.exists()) {
            file.getParentFile().mkdirs();
            try { file.createNewFile(); } catch (IOException ignored) {}
        }
        config = YamlConfiguration.loadConfiguration(file);
    }

    public void saveAndClearInventory(Player player) {
        String uuid = player.getUniqueId().toString();
        config.set(uuid, null); // Limpa registo antigo

        ItemStack[] contents = player.getInventory().getContents();
        for (int i = 0; i < contents.length; i++) {
            if (contents[i] != null && contents[i].getType() != Material.AIR) {
                config.set(uuid + ".content." + i, contents[i]);
            }
        }

        ItemStack[] armor = player.getInventory().getArmorContents();
        for (int i = 0; i < armor.length; i++) {
            if (armor[i] != null && armor[i].getType() != Material.AIR) {
                config.set(uuid + ".armor." + i, armor[i]);
            }
        }

        config.set(uuid + ".saved", true);
        saveFile();

        player.getInventory().clear();
        player.getInventory().setArmorContents(null);

        // Dá a Picareta padrão da Mina com Eficiência Infinita
        ItemStack pickaxe = new ItemStack(Material.DIAMOND_PICKAXE);
        ItemMeta meta = pickaxe.getItemMeta();
        meta.setDisplayName("§b§lPicareta de Natal");
        meta.spigot().setUnbreakable(true); // Deixa a picareta inquebrável
        pickaxe.setItemMeta(meta);

        // Adiciona Eficiência 1000 para quebrar blocos como manteiga
        pickaxe.addUnsafeEnchantment(org.bukkit.enchantments.Enchantment.DIG_SPEED, 1000);

        player.getInventory().setItem(0, pickaxe);
        player.updateInventory();
    }

    public boolean restoreInventory(Player player) {
        String uuid = player.getUniqueId().toString();
        if (config.contains(uuid + ".saved")) {
            player.getInventory().clear();
            player.getInventory().setArmorContents(null);

            ItemStack[] contents = new ItemStack[36];
            ConfigurationSection contentSection = config.getConfigurationSection(uuid + ".content");
            if (contentSection != null) {
                for (String key : contentSection.getKeys(false)) {
                    try { contents[Integer.parseInt(key)] = config.getItemStack(uuid + ".content." + key); }
                    catch (NumberFormatException ignored) {}
                }
            }
            player.getInventory().setContents(contents);

            ItemStack[] armor = new ItemStack[4];
            ConfigurationSection armorSection = config.getConfigurationSection(uuid + ".armor");
            if (armorSection != null) {
                for (String key : armorSection.getKeys(false)) {
                    try { armor[Integer.parseInt(key)] = config.getItemStack(uuid + ".armor." + key); }
                    catch (NumberFormatException ignored) {}
                }
            }
            player.getInventory().setArmorContents(armor);
            player.updateInventory();

            config.set(uuid, null);
            saveFile();
            return true; // <-- Adicionamos isto
        }
        return false; // <-- E isto
    }

    private void saveFile() {
        try { config.save(file); } catch (IOException ignored) {}
    }
}