package info.hearthsim.brazier;

import info.hearthsim.brazier.utils.BrazierTest;
import info.hearthsim.brazier.utils.TestCards;
import org.junit.Test;

public final class KilledEventTest extends BrazierTest {
    @Test
    public void testSoulOfTheForest() {
        agent.setMana("p2", 10);
        agent.playMinionCard("p2", TestCards.BLUEGILL_WARRIOR, 0);
        agent.playMinionCard("p2", TestCards.HAUNTED_CREEPER, 1);

        agent.expectBoard("p2",
            TestCards.expectedMinion(TestCards.BLUEGILL_WARRIOR, 2, 1),
            TestCards.expectedMinion(TestCards.HAUNTED_CREEPER, 1, 2));

        agent.setMana("p2", 10);
        agent.playCard("p2", TestCards.SOUL_OF_THE_FOREST);

        agent.playMinionCard("p2", TestCards.BLUEGILL_WARRIOR, 2);

        agent.expectBoard("p2",
            TestCards.expectedMinion(TestCards.BLUEGILL_WARRIOR, 2, 1),
            TestCards.expectedMinion(TestCards.HAUNTED_CREEPER, 1, 2),
            TestCards.expectedMinion(TestCards.BLUEGILL_WARRIOR, 2, 1));

        agent.setMana("p1", 10);
        agent.playCard("p1", TestCards.FLAMESTRIKE);

        agent.expectBoard("p1");
        agent.expectBoard("p2",
            TestCards.expectedMinion(TestCards.SPECTRAL_SPIDER, 1, 1),
            TestCards.expectedMinion(TestCards.SPECTRAL_SPIDER, 1, 1),
            TestCards.expectedMinion(TestCards.TREANT, 2, 2),
            TestCards.expectedMinion(TestCards.TREANT, 2, 2));
    }

    @Test
    public void testHauntedCreeper() {
        agent.setMana("p2", 10);
        agent.playMinionCard("p2", TestCards.HAUNTED_CREEPER, 0);

        agent.expectBoard("p2",
            TestCards.expectedMinion(TestCards.HAUNTED_CREEPER, 1, 2));

        agent.setMana("p1", 10);
        agent.playNonMinionCard("p1", TestCards.MOONFIRE, "p2:0");
        agent.playNonMinionCard("p1", TestCards.MOONFIRE, "p2:0");

        agent.expectBoard("p2",
            TestCards.expectedMinion(TestCards.SPECTRAL_SPIDER, 1, 1),
            TestCards.expectedMinion(TestCards.SPECTRAL_SPIDER, 1, 1));
    }

    @Test
    public void testHauntedCreeperWithFullBoard() {
        agent.setMana("p2", 10);
        agent.playMinionCard("p2", TestCards.HAUNTED_CREEPER, 0);
        agent.playMinionCard("p2", TestCards.HAUNTED_CREEPER, 1);
        agent.playMinionCard("p2", TestCards.HAUNTED_CREEPER, 2);
        agent.playMinionCard("p2", TestCards.HAUNTED_CREEPER, 3);
        agent.setMana("p2", 10);
        agent.playMinionCard("p2", TestCards.HAUNTED_CREEPER, 4);
        agent.playMinionCard("p2", TestCards.HAUNTED_CREEPER, 5);
        agent.playMinionCard("p2", TestCards.HAUNTED_CREEPER, 6);

        agent.expectBoard("p2",
            TestCards.expectedMinion(TestCards.HAUNTED_CREEPER, 1, 2),
            TestCards.expectedMinion(TestCards.HAUNTED_CREEPER, 1, 2),
            TestCards.expectedMinion(TestCards.HAUNTED_CREEPER, 1, 2),
            TestCards.expectedMinion(TestCards.HAUNTED_CREEPER, 1, 2),
            TestCards.expectedMinion(TestCards.HAUNTED_CREEPER, 1, 2),
            TestCards.expectedMinion(TestCards.HAUNTED_CREEPER, 1, 2),
            TestCards.expectedMinion(TestCards.HAUNTED_CREEPER, 1, 2));

        agent.setMana("p1", 10);
        agent.playNonMinionCard("p1", TestCards.MOONFIRE, "p2:0");
        agent.playNonMinionCard("p1", TestCards.MOONFIRE, "p2:0");

        agent.expectBoard("p2",
            TestCards.expectedMinion(TestCards.SPECTRAL_SPIDER, 1, 1),
            TestCards.expectedMinion(TestCards.HAUNTED_CREEPER, 1, 2),
            TestCards.expectedMinion(TestCards.HAUNTED_CREEPER, 1, 2),
            TestCards.expectedMinion(TestCards.HAUNTED_CREEPER, 1, 2),
            TestCards.expectedMinion(TestCards.HAUNTED_CREEPER, 1, 2),
            TestCards.expectedMinion(TestCards.HAUNTED_CREEPER, 1, 2),
            TestCards.expectedMinion(TestCards.HAUNTED_CREEPER, 1, 2));
    }

    @Test
    public void testHauntedCreeperWithFullBoardBoardClear() {
        agent.setMana("p2", 10);
        agent.playMinionCard("p2", TestCards.HAUNTED_CREEPER, 0);
        agent.playMinionCard("p2", TestCards.HAUNTED_CREEPER, 1);
        agent.playMinionCard("p2", TestCards.HAUNTED_CREEPER, 2);
        agent.playMinionCard("p2", TestCards.HAUNTED_CREEPER, 3);
        agent.setMana("p2", 10);
        agent.playMinionCard("p2", TestCards.HAUNTED_CREEPER, 4);
        agent.playMinionCard("p2", TestCards.HAUNTED_CREEPER, 5);
        agent.playMinionCard("p2", TestCards.HAUNTED_CREEPER, 6);

        agent.expectBoard("p2",
            TestCards.expectedMinion(TestCards.HAUNTED_CREEPER, 1, 2),
            TestCards.expectedMinion(TestCards.HAUNTED_CREEPER, 1, 2),
            TestCards.expectedMinion(TestCards.HAUNTED_CREEPER, 1, 2),
            TestCards.expectedMinion(TestCards.HAUNTED_CREEPER, 1, 2),
            TestCards.expectedMinion(TestCards.HAUNTED_CREEPER, 1, 2),
            TestCards.expectedMinion(TestCards.HAUNTED_CREEPER, 1, 2),
            TestCards.expectedMinion(TestCards.HAUNTED_CREEPER, 1, 2));

        agent.setMana("p1", 10);
        agent.playCard("p1", TestCards.FLAMESTRIKE);

        agent.expectBoard("p2",
            TestCards.expectedMinion(TestCards.SPECTRAL_SPIDER, 1, 1),
            TestCards.expectedMinion(TestCards.SPECTRAL_SPIDER, 1, 1),
            TestCards.expectedMinion(TestCards.SPECTRAL_SPIDER, 1, 1),
            TestCards.expectedMinion(TestCards.SPECTRAL_SPIDER, 1, 1),
            TestCards.expectedMinion(TestCards.SPECTRAL_SPIDER, 1, 1),
            TestCards.expectedMinion(TestCards.SPECTRAL_SPIDER, 1, 1),
            TestCards.expectedMinion(TestCards.SPECTRAL_SPIDER, 1, 1));
    }

    @Test
    public void testSludgeBelcherDeathRattleWorksWithFullBoard() {
        agent.setMana("p2", 10);
        agent.playMinionCard("p2", TestCards.SLUDGE_BELCHER, 0);
        agent.playMinionCard("p2", TestCards.SLUDGE_BELCHER, 1);
        agent.setMana("p2", 10);
        agent.playMinionCard("p2", TestCards.SLUDGE_BELCHER, 2);
        agent.playMinionCard("p2", TestCards.SLUDGE_BELCHER, 3);
        agent.setMana("p2", 10);
        agent.playMinionCard("p2", TestCards.SLUDGE_BELCHER, 4);
        agent.playMinionCard("p2", TestCards.SLUDGE_BELCHER, 5);
        agent.setMana("p2", 10);
        agent.playMinionCard("p2", TestCards.SLUDGE_BELCHER, 6);

        agent.expectBoard("p2",
            TestCards.expectedMinion(TestCards.SLUDGE_BELCHER, 3, 5),
            TestCards.expectedMinion(TestCards.SLUDGE_BELCHER, 3, 5),
            TestCards.expectedMinion(TestCards.SLUDGE_BELCHER, 3, 5),
            TestCards.expectedMinion(TestCards.SLUDGE_BELCHER, 3, 5),
            TestCards.expectedMinion(TestCards.SLUDGE_BELCHER, 3, 5),
            TestCards.expectedMinion(TestCards.SLUDGE_BELCHER, 3, 5),
            TestCards.expectedMinion(TestCards.SLUDGE_BELCHER, 3, 5));

        agent.setMana("p1", 10);
        agent.playNonMinionCard("p1", TestCards.MOONFIRE, "p2:0");
        agent.playNonMinionCard("p1", TestCards.MOONFIRE, "p2:0");
        agent.playNonMinionCard("p1", TestCards.MOONFIRE, "p2:0");
        agent.playNonMinionCard("p1", TestCards.MOONFIRE, "p2:0");
        agent.playNonMinionCard("p1", TestCards.MOONFIRE, "p2:0");

        agent.expectBoard("p2",
            TestCards.expectedMinion(TestCards.SLIME, 1, 2),
            TestCards.expectedMinion(TestCards.SLUDGE_BELCHER, 3, 5),
            TestCards.expectedMinion(TestCards.SLUDGE_BELCHER, 3, 5),
            TestCards.expectedMinion(TestCards.SLUDGE_BELCHER, 3, 5),
            TestCards.expectedMinion(TestCards.SLUDGE_BELCHER, 3, 5),
            TestCards.expectedMinion(TestCards.SLUDGE_BELCHER, 3, 5),
            TestCards.expectedMinion(TestCards.SLUDGE_BELCHER, 3, 5));
    }

    @Test
    public void testSludgeBelcher() {
        agent.setMana("p2", 10);
        agent.playMinionCard("p2", TestCards.SLUDGE_BELCHER, 0);
        agent.setMana("p2", 10);
        agent.playMinionCard("p2", TestCards.YETI, 0);
        agent.playMinionCard("p2", TestCards.YETI, 2);

        agent.setMana("p1", 10);
        agent.playMinionCard("p1", TestCards.YETI, 0);
        agent.playMinionCard("p1", TestCards.YETI, 1);

        agent.refreshAttacks();

        agent.expectBoard("p1",
            TestCards.expectedMinion(TestCards.YETI, 4, 5),
            TestCards.expectedMinion(TestCards.YETI, 4, 5));

        agent.expectBoard("p2",
            TestCards.expectedMinion(TestCards.YETI, 4, 5),
            TestCards.expectedMinion(TestCards.SLUDGE_BELCHER, 3, 5),
            TestCards.expectedMinion(TestCards.YETI, 4, 5));

        agent.attack("p1:0", "p2:1"); // YETI 1 -> SLUDGE

        agent.expectBoard("p1",
            TestCards.expectedMinion(TestCards.YETI, 4, 2),
            TestCards.expectedMinion(TestCards.YETI, 4, 5));

        agent.expectBoard("p2",
            TestCards.expectedMinion(TestCards.YETI, 4, 5),
            TestCards.expectedMinion(TestCards.SLUDGE_BELCHER, 3, 1),
            TestCards.expectedMinion(TestCards.YETI, 4, 5));

        agent.attack("p1:1", "p2:1"); // YETI 2 -> SLUDGE

        agent.expectBoard("p1",
            TestCards.expectedMinion(TestCards.YETI, 4, 2),
            TestCards.expectedMinion(TestCards.YETI, 4, 2));

        agent.expectBoard("p2",
            TestCards.expectedMinion(TestCards.YETI, 4, 5),
            TestCards.expectedMinion(TestCards.SLIME, 1, 2),
            TestCards.expectedMinion(TestCards.YETI, 4, 5));

        agent.refreshAttacks();

        agent.attack("p1:0", "p2:1"); // YETI 1 -> SLIME

        agent.expectBoard("p1",
            TestCards.expectedMinion(TestCards.YETI, 4, 1),
            TestCards.expectedMinion(TestCards.YETI, 4, 2));

        agent.expectBoard("p2",
            TestCards.expectedMinion(TestCards.YETI, 4, 5),
            TestCards.expectedMinion(TestCards.YETI, 4, 5));
    }

    @Test
    public void testDeathsBite() {
        agent.setMana("p1", 10);
        agent.setMana("p2", 10);

        agent.playCard("p1", TestCards.DEATHS_BITE);
        agent.playMinionCard("p1", TestCards.YETI, 0);
        agent.playMinionCard("p2", TestCards.FROTHING_BERSERKER, 0);
        agent.playMinionCard("p2", TestCards.YETI, 0);

        agent.expectBoard("p1",
            TestCards.expectedMinion(TestCards.YETI, 4, 5));
        agent.expectBoard("p2",
            TestCards.expectedMinion(TestCards.YETI, 4, 5),
            TestCards.expectedMinion(TestCards.FROTHING_BERSERKER, 2, 4));

        agent.attack("p1:hero", "p2:hero");

        agent.expectBoard("p1",
            TestCards.expectedMinion(TestCards.YETI, 4, 5));
        agent.expectBoard("p2",
            TestCards.expectedMinion(TestCards.YETI, 4, 5),
            TestCards.expectedMinion(TestCards.FROTHING_BERSERKER, 2, 4));

        agent.expectHeroHp("p1", 30, 0);
        agent.expectHeroHp("p2", 26, 0);

        agent.refreshAttacks();

        agent.attack("p1:hero", "p2:0");

        agent.expectHeroHp("p1", 26, 0);
        agent.expectHeroHp("p2", 26, 0);

        agent.expectBoard("p1",
            TestCards.expectedMinion(TestCards.YETI, 4, 4));
        agent.expectBoard("p2",
            TestCards.expectedMinion(TestCards.FROTHING_BERSERKER, 6, 3));
    }

    @Test
    public void testDeathsBiteKilledByJaraxxus() {
        agent.setMana("p1", 10);

        agent.playCard("p1", TestCards.DEATHS_BITE);
        agent.playMinionCard("p1", TestCards.YETI, 0);

        agent.expectBoard("p1",
            TestCards.expectedMinion(TestCards.YETI, 4, 5));

        agent.setMana("p1", 10);
        agent.playMinionCard("p1", TestCards.JARAXXUS, 0);

        agent.expectBoard("p1",
            TestCards.expectedMinion(TestCards.YETI, 4, 4));

        agent.expectWeapon("p1", 3, 8);
    }
}
