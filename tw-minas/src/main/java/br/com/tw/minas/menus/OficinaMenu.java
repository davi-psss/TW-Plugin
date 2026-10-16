package br.com.tw.minas.menus;

import br.com.tw.core.TWCorePlugin;
import br.com.tw.core.data.TWUser;
import br.com.tw.core.menus.TWMenu;
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
import java.util.HashMap;
import java.util.Map;

public class OficinaMenu implements TWMenu {
    private final Inventory inventory;
    private final Player player;
    private final TWUser user;
    private final Map<Integer, SkillInfo> skillsMap = new HashMap<>();

    public OficinaMenu(Player player) {
        this.player = player;
        this.user = TWCorePlugin.getInstance().getUserManager().getUser(player.getUniqueId());
        this.inventory = Bukkit.createInventory(this, 45, ChatColor.RED + "§lOficina dos Duendes");
        initSkillsMap();
        setupItems();
    }

    private void initSkillsMap() {
        skillsMap.put(11, new SkillInfo("fortuna", 100));
        skillsMap.put(12, new SkillInfo("cacador", 150));
        skillsMap.put(13, new SkillInfo("sabedoria", 200));
        skillsMap.put(14, new SkillInfo("sorteiro", 500));
        skillsMap.put(15, new SkillInfo("jackpot", 1000));
        skillsMap.put(20, new SkillInfo("explosao", 2000));
        skillsMap.put(21, new SkillInfo("broca", 2500));
        skillsMap.put(22, new SkillInfo("laser", 3000));
        skillsMap.put(23, new SkillInfo("trator", 5000));
        skillsMap.put(24, new SkillInfo("avalanche", 10000));
        skillsMap.put(25, new SkillInfo("bateria", 1500));
    }

    private void setupItems() {
        inventory.clear();
        if (user == null) return;

        // Lendo diretamente do TWUser
        int level = user.getPickaxeLevel();
        double xp = user.getPickaxeXp();

        inventory.setItem(4, createItem(Material.DIAMOND_PICKAXE, "§b§lA Tua Picareta",
                "§7Nível: §f" + level,
                "§7XP: §a" + (int)xp + " §8/ §a" + (level * 1000),
                "§7Tokens Disponíveis: §b" + br.com.tw.core.utils.TWFormat.formatarDinheiro(user.getTokens())));

        renderSkill(11, "fortuna", Material.GOLD_NUGGET, "§eFortuna", "Aumenta o dinheiro ganho.");
        renderSkill(12, "cacador", Material.SNOW_BALL, "§bCaçador de Flocos", "Aumenta o drop de Tokens.");
        renderSkill(13, "sabedoria", Material.EXP_BOTTLE, "§aSabedoria", "Aumenta o XP da Picareta.");
        renderSkill(14, "sorteiro", Material.TRIPWIRE_HOOK, "§6Sorteiro", "Chance de achar Chaves.");
        renderSkill(15, "jackpot", Material.EMERALD, "§a§lJackpot", "Dobra o dinheiro de um bloco.");
        renderSkill(20, "explosao", Material.TNT, "§cExplosão 3x3", "Quebra blocos em volta.");
        renderSkill(21, "broca", Material.PISTON_BASE, "§7Broca", "Destrói blocos para baixo.");
        renderSkill(22, "laser", Material.BEACON, "§bLaser", "Destrói blocos em linha.");
        renderSkill(23, "trator", Material.MINECART, "§8Trator", "Limpa a camada atual.");
        renderSkill(24, "avalanche", Material.NETHER_STAR, "§4§lAvalanche", "O poder mais raro.");
        renderSkill(25, "bateria", Material.REDSTONE, "§cBateria de Combo", "Aumenta o tempo do combo.");

        inventory.setItem(40, createItem(Material.WOOD_DOOR, "§cVoltar", "§7Retornar à Central."));
    }

    private void renderSkill(int slot, String id, Material mat, String name, String desc) {
        int lvl = user.getSkillLevel(id);
        int baseCost = skillsMap.get(slot).baseCost;
        int cost = baseCost * (lvl + 1);

        inventory.setItem(slot, createItem(mat, name,
                "§7" + desc, "",
                "§7Nível Atual: §f" + lvl,
                "§7Custo: §b" + br.com.tw.core.utils.TWFormat.formatarDinheiro(cost) + " Tokens",
                "§aClica para evoluir!"));
    }

    @Override
    public void onClick(InventoryClickEvent event) {
        int slot = event.getSlot();
        if (slot == 40) {
            new MinaMainMenu(player).open();
            return;
        }

        SkillInfo info = skillsMap.get(slot);
        if (info != null) {
            int currentLvl = user.getSkillLevel(info.id);
            int cost = info.baseCost * (currentLvl + 1);

            if (user.removeTokens(cost)) {
                user.upgradeSkill(info.id); // Melhora a habilidade direto no TWUser

                // Salva no banco de dados para evitar perda em caso de crash
                TWCorePlugin.getInstance().getUserManager().saveUserAsync(user);

                player.playSound(player.getLocation(), Sound.LEVEL_UP, 1f, 2f);
                player.sendMessage(ChatColor.GREEN + "Habilidade " + info.id.toUpperCase() + " evoluída para o nível " + (currentLvl + 1) + "!");
                setupItems();
            } else {
                player.playSound(player.getLocation(), Sound.VILLAGER_NO, 1f, 1f);
                long faltam = cost - user.getTokens();
                player.sendMessage(ChatColor.RED + "Não tens Tokens suficientes! Faltam " + br.com.tw.core.utils.TWFormat.formatarDinheiro(faltam) + ".");
            }
        }
    }

    @Override
    public Inventory getInventory() { return inventory; }
    public void open() { player.openInventory(inventory); }

    private ItemStack createItem(Material mat, String name, String... lore) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(name);
        meta.setLore(Arrays.asList(lore));
        item.setItemMeta(meta);
        return item;
    }

    private static class SkillInfo {
        String id;
        int baseCost;
        SkillInfo(String id, int baseCost) { this.id = id; this.baseCost = baseCost; }
    }
}