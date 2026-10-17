package br.com.tw.caixas.managers;

import br.com.tw.caixas.model.CrateDefinition;
import br.com.tw.caixas.model.CrateReward;
import br.com.tw.caixas.model.RewardGrant;
import br.com.tw.caixas.model.RewardKind;
import org.bukkit.Material;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class CrateOpeningsTest {

    @Test
    public void bulkOpeningRollsExactlyOneRewardPerCrate() {
        CrateReward common = new CrateReward(99.0, CrateReward.Rarity.COMMON, RewardKind.ITEM,
                1L, 1L, Material.PAPER, "Common", null, 0);
        CrateReward legendary = new CrateReward(1.0, CrateReward.Rarity.LEGENDARY, RewardKind.ITEM,
                1L, 1L, Material.NETHER_STAR, "Legendary", null, 0);
        CrateDefinition definition = new CrateDefinition("Crate", Material.CHEST,
                Arrays.asList(common, legendary));

        List<RewardGrant> grants = CrateOpenings.rollBulkRewards(
                definition, 3, Arrays.asList(false, true, false), false);

        long totalRewards = grants.stream().mapToLong(RewardGrant::getAmount).sum();
        assertEquals(3L, totalRewards);
        assertTrue(grants.stream().anyMatch(grant ->
                grant.getReward().getRarity() == CrateReward.Rarity.LEGENDARY));
    }

    @Test
    public void divineSealGuaranteesOneLegendaryDuringBulkOpening() {
        CrateReward common = new CrateReward(99.0, CrateReward.Rarity.COMMON, RewardKind.ITEM,
                1L, 1L, Material.PAPER, "Common", null, 0);
        CrateReward legendary = new CrateReward(1.0, CrateReward.Rarity.LEGENDARY, RewardKind.ITEM,
                1L, 1L, Material.NETHER_STAR, "Legendary", null, 0);
        CrateDefinition definition = new CrateDefinition("Crate", Material.CHEST,
                Arrays.asList(common, legendary));

        List<RewardGrant> grants = CrateOpenings.rollBulkRewards(
                definition, 1, Arrays.asList(false), true);

        assertEquals(1L, grants.stream().mapToLong(RewardGrant::getAmount).sum());
        assertEquals(CrateReward.Rarity.LEGENDARY, grants.get(0).getReward().getRarity());
    }
}
