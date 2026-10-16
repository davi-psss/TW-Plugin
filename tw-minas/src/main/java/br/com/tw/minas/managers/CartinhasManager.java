package br.com.tw.minas.managers;

import br.com.tw.core.TWCorePlugin;
import br.com.tw.core.data.TWUser;
import br.com.tw.core.utils.TWFormat;
import org.bukkit.ChatColor;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class CartinhasManager {

    public enum MissionType {
        BLOCOS("Quebrar Blocos", "Quebra %target% blocos nas minas"),
        COMBO("Mestre do Combo", "Atinge um Combo de x%target%"),
        SKILLS("Poder dos Duendes", "Ativa habilidades da picareta %target% vezes"),
        PRESENTES("Caçador de Presentes", "Encontra %target% Caixas Surpresa na mina");

        private final String title;
        private final String descTemplate;

        MissionType(String title, String descTemplate) {
            this.title = title;
            this.descTemplate = descTemplate;
        }

        public String getTitle() { return title; }
        public String getDescription(long target) {
            return descTemplate.replace("%target%", String.valueOf(target));
        }
    }

    public static class MissionSlot {
        public int rarity; // 0 = Comum (Duende), 1 = Rara (Boneco de Neve), 2 = Lendária (Papai Noel)
        public MissionType type;
        public long progress;
        public long target;
        public long rewardTokens;
        public double rewardMoney;

        public MissionSlot(int rarity, MissionType type, long progress, long target, long rewardTokens, double rewardMoney) {
            this.rarity = rarity;
            this.type = type;
            this.progress = progress;
            this.target = target;
            this.rewardTokens = rewardTokens;
            this.rewardMoney = rewardMoney;
        }

        public boolean isCompleted() {
            return progress >= target;
        }
    }

    private final Random random = new Random();

    public List<MissionSlot> getMissions(TWUser user) {
        String raw = user.getMissoesData();
        List<MissionSlot> list = new ArrayList<>();

        if (raw != null && !raw.isEmpty()) {
            String[] parts = raw.split(";");
            if (parts.length == 3) {
                try {
                    for (int i = 0; i < 3; i++) {
                        String[] fields = parts[i].split(":");
                        MissionType type = MissionType.valueOf(fields[0]);
                        long prog = Long.parseLong(fields[1]);
                        long targ = Long.parseLong(fields[2]);
                        long rTokens = Long.parseLong(fields[3]);
                        double rMoney = Double.parseDouble(fields[4]);
                        list.add(new MissionSlot(i, type, prog, targ, rTokens, rMoney));
                    }
                    return list;
                } catch (Exception ignored) {
                    list.clear();
                }
            }
        }

        // Se não tiver missões válidas, gera as 3 iniciais
        for (int i = 0; i < 3; i++) {
            list.add(generateRandomMission(i, user.getPickaxeLevel()));
        }
        saveMissions(user, list);
        return list;
    }

    public MissionSlot generateRandomMission(int rarity, int pickaxeLevel) {
        MissionType[] types = MissionType.values();
        MissionType chosen = types[random.nextInt(types.length)];

        int mult = (rarity == 0) ? 1 : (rarity == 1) ? 3 : 8;
        int lvlScale = Math.max(1, pickaxeLevel);

        long target;
        switch (chosen) {
            case BLOCOS:
                target = (250L + random.nextInt(250)) * mult;
                break;
            case COMBO:
                target = (rarity == 0) ? 50 : (rarity == 1) ? 120 : 250;
                break;
            case SKILLS:
                target = (5L + random.nextInt(6)) * mult;
                break;
            case PRESENTES:
            default:
                target = (rarity == 0) ? 1 : (rarity == 1) ? 3 : 7;
                break;
        }

        long rewardTokens = (150L * mult) + (lvlScale * 25L * mult);
        double rewardMoney = (25000.0 * mult) * Math.pow(1.35, Math.min(30, lvlScale));

        return new MissionSlot(rarity, chosen, 0L, target, rewardTokens, rewardMoney);
    }

    public void saveMissions(TWUser user, List<MissionSlot> slots) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < slots.size(); i++) {
            MissionSlot s = slots.get(i);
            sb.append(s.type.name()).append(":")
                    .append(s.progress).append(":")
                    .append(s.target).append(":")
                    .append(s.rewardTokens).append(":")
                    .append(s.rewardMoney);
            if (i < slots.size() - 1) sb.append(";");
        }
        user.setMissoesData(sb.toString());
    }

    public void incrementProgress(Player player, TWUser user, MissionType type, long amount, boolean isAbsoluteValue) {
        List<MissionSlot> slots = getMissions(user);
        boolean changed = false;

        for (MissionSlot slot : slots) {
            if (slot.type == type && !slot.isCompleted()) {
                long old = slot.progress;
                if (isAbsoluteValue) {
                    slot.progress = Math.max(slot.progress, Math.min(slot.target, amount));
                } else {
                    slot.progress = Math.min(slot.target, slot.progress + amount);
                }

                if (slot.progress != old) {
                    changed = true;
                    if (slot.isCompleted()) {
                        player.playSound(player.getLocation(), Sound.LEVEL_UP, 1f, 1.8f);
                        player.sendMessage("");
                        player.sendMessage(ChatColor.LIGHT_PURPLE + "" + ChatColor.BOLD + "CARTINHA CONCLUÍDA! " +
                                ChatColor.YELLOW + "(" + slot.type.getTitle() + ")");
                        player.sendMessage(ChatColor.GRAY + "Abre o menu " + ChatColor.AQUA + "/mina -> Cartinhas" +
                                ChatColor.GRAY + " para resgatar a tua recompensa!");
                        player.sendMessage("");
                    }
                }
            }
        }

        if (changed) {
            saveMissions(user, slots);
        }
    }

    public boolean claimOrReroll(Player player, TWUser user, int slotIndex, boolean isRightClick) {
        List<MissionSlot> slots = getMissions(user);
        if (slotIndex < 0 || slotIndex >= slots.size()) return false;

        MissionSlot slot = slots.get(slotIndex);

        if (slot.isCompleted()) {
            user.addTokens(slot.rewardTokens);
            user.addBalance(slot.rewardMoney);

            player.playSound(player.getLocation(), Sound.ORB_PICKUP, 1f, 1.2f);
            player.sendMessage(ChatColor.GREEN + "" + ChatColor.BOLD + "RECOMPENSA RESGATADA! " +
                    ChatColor.AQUA + "+" + TWFormat.formatarDinheiro(slot.rewardTokens) + " Tokens " +
                    ChatColor.GRAY + "e " + ChatColor.GREEN + "+$" + TWFormat.formatarDinheiro(slot.rewardMoney) + "!");

            // Sorteia nova missão para aquela carta imediatamente
            slots.set(slotIndex, generateRandomMission(slotIndex, user.getPickaxeLevel()));
            saveMissions(user, slots);
            TWCorePlugin.getInstance().getUserManager().saveUserAsync(user);
            return true;
        } else if (isRightClick) {
            // Trocar missão por 50 Tokens
            long rerollCost = 50L;
            if (user.removeTokens(rerollCost)) {
                slots.set(slotIndex, generateRandomMission(slotIndex, user.getPickaxeLevel()));
                saveMissions(user, slots);
                TWCorePlugin.getInstance().getUserManager().saveUserAsync(user);
                player.playSound(player.getLocation(), Sound.BAT_TAKEOFF, 0.8f, 1.4f);
                player.sendMessage(ChatColor.YELLOW + "Sorteaste uma nova missão para esta Cartinha por 50 Tokens!");
                return true;
            } else {
                player.playSound(player.getLocation(), Sound.VILLAGER_NO, 1f, 1f);
                player.sendMessage(ChatColor.RED + "Precisas de 50 Tokens para sortear outra missão!");
                return false;
            }
        } else {
            player.playSound(player.getLocation(), Sound.VILLAGER_NO, 1f, 1f);
            player.sendMessage(ChatColor.RED + "Ainda não concluíste o objetivo desta Cartinha! (" + slot.progress + "/" + slot.target + ")");
            return false;
        }
    }
}