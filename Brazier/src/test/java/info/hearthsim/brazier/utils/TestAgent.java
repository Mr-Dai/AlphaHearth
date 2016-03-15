package info.hearthsim.brazier.utils;

import info.hearthsim.brazier.*;
import info.hearthsim.brazier.actions.PlayTargetRequest;
import info.hearthsim.brazier.actions.undo.UndoAction;
import info.hearthsim.brazier.cards.Card;
import info.hearthsim.brazier.cards.CardDescr;
import info.hearthsim.brazier.minions.Minion;
import info.hearthsim.brazier.parsing.TestDb;
import info.hearthsim.brazier.cards.CardId;
import info.hearthsim.brazier.weapons.Weapon;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Deque;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;

import org.jtrim.utils.ExceptionHelper;

import static org.junit.Assert.*;
import static info.hearthsim.brazier.utils.TestUtils.*;

public final class TestAgent {
    private final HearthStoneDb db;
    private final ScriptedRandomProvider randomProvider;
    private final ScriptedUserAgent userAgent;
    private final World world;
    private final WorldPlayAgent playAgent;

    // /** The list of actions of the script */
    // private final List<ScriptAction> script;

    public TestAgent() {
        this(true);
    }

    public TestAgent(boolean player1First) {
        this.db = TestDb.getTestDb();

        this.randomProvider = new ScriptedRandomProvider();
        this.userAgent = new ScriptedUserAgent(db);
        this.world = player1First
            ? new World(db, PLAYER1_ID, PLAYER2_ID)
            : new World(db, PLAYER2_ID, PLAYER1_ID);
        this.world.setRandomProvider(randomProvider);
        this.world.setUserAgent(userAgent);
        this.playAgent = new WorldPlayAgent(world);
    }

    /**
     * Runs a test script given by a {@link Consumer} of {@link TestAgent}
     *
     * @param scriptConfig a {@link Consumer} of {@link TestAgent}, which is used
     *                     to be designated by lambda expression
     */

    /*public static void testScript(Consumer<TestAgent> scriptConfig) {
        TestAgent script = new TestAgent();
        scriptConfig.accept(script);
        script.executeScript();
    }*/

    /**
     * Appends an action to the script.
     *
     * @param scriptAction the script action to be appended
     */
    /*private void addScriptAction(Function<ScriptAgent, void> scriptAction) {
        addScriptAction(false, scriptAction);
    }

    private void addScriptAction(boolean expectationCheck, Function<ScriptAgent, void> scriptAction) {
        script.add(new ScriptAction(expectationCheck, new Exception("Script config line"), scriptAction));
    }*/

    /**
     * Applies the given action to the player with the given name.
     */
    public void applyToPlayer(String playerName, Function<? super Player, ? extends UndoAction> action) {
        Player player = world.getPlayer(parsePlayerName(playerName));
        action.apply(player);
    }

    /**
     * Decreases the mana cost of all the cards in the hand of the player with the given name
     * with {@code 1}.
     */
    public void decreaseManaCostOfHand(String playerName) {
        PlayerId playerId = parsePlayerName(playerName);
            Hand hand = world.getPlayer(playerId).getHand();
        hand.forAllCards((card) -> card.decreaseManaCost(1));
    }

    /**
     * Uses the given {@link Consumer} of {@link Player} to check the current state of the
     * given player.
     *
     * @param playerName the name of the player to be checked.
     * @param check      the test script, usually designated by a lambda expression.
     */
    public void expectPlayer(String playerName, Consumer<? super Player> check) {
        PlayerId playerId = parsePlayerName(playerName);
        Player player = world.getPlayer(playerId);
        check.accept(player);
    }

    /**
     * Expects the {@link Minion} with the given name by using the given {@link Consumer}
     * of {@code Minion}.
     *
     * @throws AssertionError if there is no such minion.
     */
    public void expectMinion(String target, Consumer<? super Minion> check) {
        TargetableCharacter foundTarget = findTarget(target);
        assertNotNull("Minion", foundTarget);
        assertTrue("Minion", foundTarget instanceof Minion);

        check.accept((Minion) foundTarget);
    }

    /**
     * Sets the current player to the player with the given name
     */
    public void setCurrentPlayer(String playerName) {
        PlayerId playerId = parsePlayerName(playerName);
        playAgent.setCurrentPlayerId(playerId);
    }

    /**
     * Ends the current turn.
     */
    public void endTurn() {
        playAgent.endTurn();
    }

    public void playMinionCard(String playerName, int cardIndex, int minionPos) {
        playMinionCard(playerName, cardIndex, minionPos, "");
    }

    public void playCard(String playerName, int cardIndex, String target) {
        playCard(playerName, cardIndex, -1, target);
    }

    public void playCard(String playerName, int cardIndex) {
        playCard(playerName, cardIndex, -1, "");
    }

    public void playMinionCard(String playerName, int cardIndex, int minionPos, String target) {
        PlayerId playerId = parsePlayerName(playerName);

        if (minionPos < 0) {
            throw new IllegalArgumentException("Need minion drop location.");
        }

        playCard(playerId, cardIndex, minionPos, target);
    }

    private void playCard(String playerName, int cardIndex, int minionPos, String target) {
        PlayerId playerId = parsePlayerName(playerName);
        playCard(playerId, cardIndex, minionPos, target);
    }

    private void playCard(PlayerId playerId, int cardIndex, int minionPos, String target) {
        expectGameContinues();
        playAgent.playCard(cardIndex, toPlayTarget(playerId, minionPos, target));
    }

    /**
     * Designates the given player to play the given minion card on the given location.
     *
     * @param playerName the name of the player
     * @param cardName   the name of the minion card to be played
     * @param minionPos  the location on which the minion is to be placed
     * @throws IllegalArgumentException if {@code minionPos < 0}.
     */
    public void playMinionCard(String playerName, String cardName, int minionPos) {
        playMinionCard(playerName, cardName, minionPos, "");
    }

    /**
     * Designates the given player to play the given non minion card, with a specific target.
     *
     * @param playerName the name of the player
     * @param cardName   the name of the card.
     * @param target     the name of the target; {@code ""} for no target.
     */
    public void playNonMinionCard(String playerName, String cardName, String target) {
        playCard(playerName, cardName, -1, target);
    }

    /**
     * Designates the given player to play the specific card
     *
     * @param playerName the name of the player
     * @param cardName   the name of the card
     */
    public void playCard(String playerName, String cardName) {
        playCard(playerName, cardName, -1, "");
    }

    /**
     * Designates the given player to play the specific minion card with a specific minion position
     * and target.
     *
     * @param playerName the name of the player.
     * @param cardName   the name of the minion card.
     * @param minionPos  the position the minion should be put.
     * @param target     the name of the target; {@code ""} (empty string) for no target.
     * @throws IllegalArgumentException if {@code minionPos < 0}.
     */
    public void playMinionCard(String playerName, String cardName, int minionPos, String target) {
        if (minionPos < 0) {
            throw new IllegalArgumentException("Need minion drop location.");
        }

        playCard(playerName, cardName, minionPos, target);
    }

    private void playCard(String playerName, String cardName, int minionPos, String target) {
        PlayerId playerId = parsePlayerName(playerName);
        playCard(playerId, cardName, minionPos, target);
    }

    /**
     * Adds the designated card to the player's hand and play it.
     */
    private void playCard(PlayerId playerId, String cardName, int minionPos, String target) {
        CardDescr cardDescr = db.getCardDb().getById(new CardId(cardName));

        expectGameContinues();
        Player player = world.getPlayer(playerId);
        Hand hand = player.getHand();
        hand.addCard(cardDescr);

        int cardIndex = hand.getCardCount() - 1;
        playAgent.playCard(cardIndex, toPlayTarget(playerId, minionPos, target));
    }

    /**
     * Expects the game does not end yet.
     */
    public void expectGameContinues() {
        GameResult gameResult = world.tryGetGameResult();
        if (gameResult != null) {
            fail("Unexpected game over: " + gameResult);
        }
    }

    /**
     * Expects the game is over and players with the given names are dead.
     */
    public void expectHeroDeath(String... expectedDeadPlayerNames) {
        Set<PlayerId> expectedDeadPlayerIds = new HashSet<>();
        for (String playerName : expectedDeadPlayerNames) {
            expectedDeadPlayerIds.add(parsePlayerName(playerName));
        }

        GameResult gameResult = world.tryGetGameResult();
        if (gameResult == null) {
            throw new AssertionError("Expected game over.");
        }

        Set<PlayerId> deadPlayerIds = new HashSet<>(gameResult.getDeadPlayers());
        assertEquals("Dead players", expectedDeadPlayerIds, deadPlayerIds);
    }

    /**
     * Puts the cards with the given names to the deck of the player with the given name.
     */
    public void deck(String playerName, String... cardNames) {
        PlayerId playerId = parsePlayerName(playerName);
        List<CardDescr> newCards = parseCards(db, cardNames);

        Player player = world.getPlayer(playerId);
        Deck deck = player.getDeck();

        deck.setCards(newCards);
    }

    /**
     * Adds the cards with the given names to the hand of the player with the given name.
     */
    public void addToHand(String playerName, String... cardNames) {
        PlayerId playerId = parsePlayerName(playerName);
        List<CardDescr> newCards = parseCards(db, cardNames);

        Hand hand = world.getPlayer(playerId).getHand();
        for (CardDescr card : newCards)
            hand.addCard(card);
    }

    /**
     * Expects the deck of the player with the given name has and only has the cards with the given names.
     */
    public void expectDeck(String playerName, String... cardNames) {
        PlayerId playerId = parsePlayerName(playerName);
        List<CardDescr> expectedCards = parseCards(db, cardNames);

        Player player = world.getPlayer(playerId);
        Deck deck = player.getDeck();

        List<Card> deckCards = deck.getCards();
        List<CardDescr> deckCardDescrs = new ArrayList<>(deckCards.size());
        deckCards.forEach((card) -> deckCardDescrs.add(card.getCardDescr()));

        assertEquals("deck", expectedCards, deckCardDescrs);
    }

    /**
     * Throws an {@link AssertionError} which indicates the given actual list of {@link Secret}
     * does not conform to the given expected list of secret names.
     */
    private void unexpectedSecrets(String[] expectedNames, List<Secret> actual) {
        List<String> actualNames = new ArrayList<>(actual.size());
        actual.forEach((secret) -> actualNames.add(secret.getSecretId().getName()));
        fail("The actual secrets are different than what was expected."
            + " Expected: " + Arrays.toString(expectedNames)
            + ". Actual: " + actualNames);
    }

    /**
     * Expects the list of {@link Secret}s the player with the given name hae being exactly
     * same as the given array of secret names.
     */
    public void expectSecret(String playerName, String... secretNames) {
        PlayerId playerId = parsePlayerName(playerName);
        String[] secretNamesCopy = secretNames.clone();

        Player player = world.getPlayer(playerId);
        List<Secret> secrets = player.getSecrets().getSecrets();

        if (secrets.size() != secretNamesCopy.length) {
            unexpectedSecrets(secretNames, secrets);
        }

        for (int i = 0; i < secretNamesCopy.length; i++) {
            if (!secretNamesCopy[i].equals(secrets.get(i).getSecretId().getName())) {
                unexpectedSecrets(secretNames, secrets);
            }
        }
    }

    /**
     * Expects the hand of the player with the given name having the exact cards as the
     * given array of card names.
     */
    public void expectHand(String playerName, String... cardNames) {
        PlayerId playerId = parsePlayerName(playerName);
        List<CardDescr> expectedCards = parseCards(db, cardNames);

        Player player = world.getPlayer(playerId);
        Hand hand = player.getHand();

        List<Card> handCards = hand.getCards();
        List<CardDescr> handCardDescrs = new ArrayList<>(handCards.size());
        handCards.forEach((card) -> handCardDescrs.add(card.getCardDescr()));

        assertEquals("hand", expectedCards, handCardDescrs);
    }

    /**
     * Sets the health and armor point of the player with the given name to the given values.
     */
    public void setHeroHp(String playerName, int hp, int armor) {
        PlayerId playerId = parsePlayerName(playerName);
        Player player = world.getPlayer(playerId);
        Hero hero = player.getHero();

        hero.setCurrentArmor(armor);
        hero.setCurrentHp(hp);
    }

    /**
     * Sets the current mana of the player with the given name.
     */
    public void setMana(String playerName, int mana) {
        PlayerId playerId = parsePlayerName(playerName);
        Player player = world.getPlayer(playerId);

        player.setMana(mana);
    }

    /**
     * Expects the player with the given name having the given amount of mana left.
     */
    public void expectMana(String playerName, int expectedMana) {
        PlayerId playerId = parsePlayerName(playerName);
        Player player = world.getPlayer(playerId);
        assertEquals(expectedMana, player.getMana());
    }

    /**
     * Expects the player with the given name having the given amount of health and aromr point.
     */
    public void expectHeroHp(String playerName, int expectedHp, int expectedArmor) {
        PlayerId playerId = parsePlayerName(playerName);
        Hero hero = world.getPlayer(playerId).getHero();
        assertEquals("hp", expectedHp, hero.getCurrentHp());
        assertEquals("armor", expectedArmor, hero.getCurrentArmor());
    }

    /**
     * Expects the player with the given name having given amount of attack and no weapon.
     */
    public void expectNoWeapon(String playerName, int attack) {
        PlayerId playerId = parsePlayerName(playerName);
        Player player = world.getPlayer(playerId);
        Hero hero = player.getHero();
        assertNull("weapon", player.tryGetWeapon());
        assertEquals("attack", attack, hero.getAttackTool().getAttack());
    }

    /**
     * Expects the player with the given name having a weapon with the given amount of attack and durability.
     */
    public void expectWeapon(String playerName, int expectedAttack, int expectedDurability) {
        PlayerId playerId = parsePlayerName(playerName);
        Player player = world.getPlayer(playerId);
        Hero hero = player.getHero();
        Weapon weapon = player.tryGetWeapon();
        assertNotNull("weapon", weapon);

        assertEquals("attack", expectedAttack, hero.getAttackTool().getAttack());
        assertEquals("charges", expectedDurability, weapon.getDurability());
    }

    /**
     * Refreshes the attack of characters of both players.
     */
    public void refreshAttacks() {
        refreshAttack("p1");
        refreshAttack("p2");
    }

    /**
     * Refreshes the attack of characters of the player with the given name.
     */
    public void refreshAttack(String playerName) {
        PlayerId playerId = parsePlayerName(playerName);

        Player player = world.getPlayer(playerId);
        player.getHero().refresh();
        player.getBoard().refreshStartOfTurn();
    }

    public void addRoll(int possibilityCount, int rollResult) {
        randomProvider.addRoll(possibilityCount, rollResult);
    }

    public void addCardChoice(int choiceIndex, String... cardNames) {
        ExceptionHelper.checkArgumentInRange(choiceIndex, 0, cardNames.length - 1, "choiceIndex");

        String[] cardNamesCopy = cardNames.clone();
        ExceptionHelper.checkNotNullElements(cardNamesCopy, "cardNames");

        userAgent.addChoice(choiceIndex, cardNamesCopy);
    }

    /**
     * Expects the minions of the player with the given name satisfying the given array of
     * {@link MinionExpectations}.
     */
    public void expectBoard(String playerName, MinionExpectations... minionDescrs) {
        PlayerId playerId = parsePlayerName(playerName);

        MinionExpectations[] minionDescrsCopy = minionDescrs.clone();
        ExceptionHelper.checkNotNullElements(minionDescrsCopy, "minionDescrsCopy");

        BoardSide board = world.getPlayer(playerId).getBoard();
        List<Minion> minions = board.getAllMinions();

        if (minions.size() != minionDescrsCopy.length) {
            fail("The size of the board is different than expected."
                + " Expected board: " + Arrays.toString(minionDescrsCopy) + ". Actual board: " + minions);
        }

        int index = 0;
        for (Minion minion : minions) {
            int minionIndex = index;
            minionDescrsCopy[index].verifyExpectations(minion, () -> "Index: " + minionIndex + ".");
            index++;
        }
    }

    /** Attacks the target with the given name with the attacker with the given name. */
    public void attack(String attacker, String target) {
        TargetId attackerId = findTargetId(attacker);
        TargetId targetId = findTargetId(target);

        playAgent.attack(attackerId, targetId);
    }

    /*private void executeScript() {
        for (boolean changePlayers : new boolean[] { false, true }) {
            executeScript(changePlayers);
        }
    }

    private void executeScript(boolean changePlayers) {
        ScriptAgent scriptAgent = new ScriptAgent(changePlayers, db);

        executeAllScriptActions(scriptAgent);

        if (!scriptAgent.randomProvider.rolls.isEmpty()) {
            throw new AssertionError("There were unnecessary rolls defined: " + scriptAgent.randomProvider.rolls);
        }

        if (!scriptAgent.userAgent.choices.isEmpty()) {
            throw new AssertionError("There were unnecessary card choices defined: " + scriptAgent.userAgent.choices);
        }

        int scriptSize = script.size();
        for (int index1 = 0; index1 < scriptSize - 1; index1++) {
            if (script.get(index1).expectationCheck) {
                continue;
            }

            for (int index2 = index1 + 1; index2 <= scriptSize; index2++) {
                if (script.get(index2 - 1).expectationCheck) {
                    continue;
                }

                try {
                    executeScriptWithUndoTest(changePlayers, index1, index2);
                } catch (Throwable ex) {
                    ex.addSuppressed(new Exception("Undoing after", script.get(index1).stackTrace));
                    ex.addSuppressed(new Exception("Undoing before", script.get(index2 - 1).stackTrace));
                    throw ex;
                }
            }
        }
    }

    private void executeScriptWithUndoTest(boolean changePlayers, int index1, int index2) {
        ScriptAgent scriptAgent = new ScriptAgent(changePlayers, db);

        List<ScriptAction> currentScript = new ArrayList<>(script);
        int scriptLength = currentScript.size();

        ExceptionHelper.checkArgumentInRange(index1, 0, index2 - 1, "index1");
        ExceptionHelper.checkArgumentInRange(index2, index1, scriptLength, "index2");

        List<UndoableResult<Throwable>> undos = new LinkedList<>();
        for (int i = 0; i < index2; i++) {
            ScriptAction action = currentScript.get(i);
            void undo;
            try {
                undo = action.doAction(scriptAgent);
            } catch (Throwable ex) {
                ex.addSuppressed(action.stackTrace);
                throw ex;
            }

            if (i >= index1) {
                undos.add(0, new UndoableResult<>(action.stackTrace, undo));
            }
        }

        for (UndoableResult<Throwable> undo : undos) {
            try {
                undo.undo();
            } catch (Throwable ex) {
                AssertionError undoError = new AssertionError("Undoing action failed.", ex);
                undoError.addSuppressed(undo.getResult());
                throw undoError;
            }
        }

        for (int i = index1; i < scriptLength; i++) {
            ScriptAction action = currentScript.get(i);
            try {
                action.doAction(scriptAgent);
            } catch (Throwable ex) {
                ex.addSuppressed(action.stackTrace);
                throw ex;
            }
        }
    }

    private void executeAllScriptActions(ScriptAgent scriptAgent) {
        List<ScriptAction> currentScript = new ArrayList<>(script);

        for (ScriptAction action : currentScript) {
            try {
                action.doAction(scriptAgent);
            } catch (Throwable ex) {
                ex.addSuppressed(action.stackTrace);
                throw ex;
            }
        }
    }*/

    public TargetableCharacter findTarget(String targetId) {
        if (targetId.trim().isEmpty()) {
            return null;
        }

        String[] targetIdParts = targetId.split(":");
        if (targetIdParts.length < 2) {
            throw new IllegalArgumentException("Illegal target ID: " + targetId);
        }

        Player player = world.getPlayer(parsePlayerName(targetIdParts[0]));
        String targetName = targetIdParts[1].trim();
        if (targetName.equalsIgnoreCase("hero")) {
            return player.getHero();
        }

        int minionIndex = Integer.parseInt(targetName);
        Minion minion = player.getBoard().getAllMinions().get(minionIndex);
        return minion;
    }

    public TargetId findTargetId(String targetId) {
        TargetableCharacter target = findTarget(targetId);
        return target != null ? target.getTargetId() : null;
    }

    public PlayTargetRequest toPlayTarget(PlayerId player, int minionPos, String targetId) {
        return new PlayTargetRequest(player, minionPos, findTargetId(targetId));
    }

    private static final class CardChoiceDef {
        public final int choiceIndex;
        public final String[] cardNames;

        public CardChoiceDef(int choiceIndex, String[] cardNames) {
            this.choiceIndex = choiceIndex;
            this.cardNames = cardNames;
        }

        private void failExpectations(List<? extends CardDescr> cards) {
            throw new AssertionError("Unexpected card choices: " + cards
                + ". Expected: " + Arrays.toString(cardNames));
        }

        public void assertSameCards(List<? extends CardDescr> cards) {
            if (cards.size() != cardNames.length) {
                failExpectations(cards);
            }

            int index = 0;
            for (CardDescr card : cards) {
                if (!Objects.equals(cardNames[index], card.getId().getName())) {
                    failExpectations(cards);
                }
                index++;
            }
        }

        public CardDescr getChoice(HearthStoneDb db) {
            return db.getCardDb().getById(new CardId(cardNames[choiceIndex]));
        }

        @Override
        public String toString() {
            return "CardChoiceDef{" + "choice=" + choiceIndex + ", cards=" + Arrays.toString(cardNames) + '}';
        }
    }

    private static final class RollDef {
        public final int possibilityCount;
        public final int rollResult;

        public RollDef(int possibilityCount, int rollResult) {
            ExceptionHelper.checkArgumentInRange(rollResult, 0, possibilityCount - 1, "rollResult");

            this.possibilityCount = possibilityCount;
            this.rollResult = rollResult;
        }

        @Override
        public String toString() {
            return "Roll{" + "possibilityCount=" + possibilityCount + ", rollResult=" + rollResult + '}';
        }
    }

    private static final class ScriptedRandomProvider implements RandomProvider {
        private final Deque<RollDef> rolls;
        private Deque<RollDef> rollRecorder;

        public ScriptedRandomProvider() {
            this.rolls = new LinkedList<>();
        }

        public void addRoll(int possibilityCount, int rollResult) {
            rolls.addLast(new RollDef(possibilityCount, rollResult));
        }

        public void startRollRecording(Deque<RollDef> rolls) {
            if (rollRecorder != null) {
                throw new IllegalStateException("Nested roll recording.");
            }
            rollRecorder = rolls;
        }

        public void stopRollRecording() {
            rollRecorder = null;
        }

        public void addRecordedRolls(Deque<RollDef> recordedRolls) {
            for (RollDef roll : recordedRolls) {
                rolls.addFirst(roll);
            }
        }

        @Override
        public int roll(int bound) {
            RollDef roll = rolls.pollFirst();
            if (roll == null) {
                throw new AssertionError("Unexpected random roll: " + bound);
            }

            if (roll.possibilityCount != bound) {
                throw new AssertionError("Unexpected possibility count for random roll: " + bound
                    + ". Expected: " + roll.possibilityCount);
            }

            if (rollRecorder != null) {
                rollRecorder.addFirst(roll);
            }

            return roll.rollResult;
        }
    }

    private static final class ScriptedUserAgent implements UserAgent {
        private final HearthStoneDb db;
        private final Deque<CardChoiceDef> choices;
        private Deque<CardChoiceDef> choiceRecorder;

        public ScriptedUserAgent(HearthStoneDb db) {
            assert db != null;

            this.db = db;
            this.choices = new LinkedList<>();
        }

        public void addChoice(int choiceIndex, String[] cardNames) {
            choices.addLast(new CardChoiceDef(choiceIndex, cardNames));
        }

        public void startRollRecording(Deque<CardChoiceDef> choices) {
            if (choiceRecorder != null) {
                throw new IllegalStateException("Nested card choice recording.");
            }
            choiceRecorder = choices;
        }

        public void stopRollRecording() {
            choiceRecorder = null;
        }

        public void addRecordedChoices(Deque<CardChoiceDef> recordedChoices) {
            for (CardChoiceDef choice : recordedChoices) {
                choices.addFirst(choice);
            }
        }

        @Override
        public CardDescr selectCard(boolean allowCancel, List<? extends CardDescr> cards) {
            CardChoiceDef choice = choices.pollFirst();
            if (choice == null) {
                throw new AssertionError("Unexpected card choose request: " + cards);
            }
            choice.assertSameCards(cards);

            if (choiceRecorder != null) {
                choiceRecorder.addFirst(choice);
            }

            return choice.getChoice(db);
        }
    }
/*
    private static final class ScriptAgent {
        private final ScriptedUserAgent userAgent;
        private final ScriptedRandomProvider randomProvider;
        private final WorldPlayAgent playAgent;
        private final World world;

        public ScriptAgent(boolean changePlayers, HearthStoneDb db) {
            this.randomProvider = new ScriptedRandomProvider();
            this.userAgent = new ScriptedUserAgent(db);
            this.world = changePlayers
                ? new World(db, PLAYER2_ID, PLAYER1_ID)
                : new World(db, PLAYER1_ID, PLAYER2_ID);
            this.world.setRandomProvider(randomProvider);
            this.world.setUserAgent(userAgent);
            this.playAgent = new WorldPlayAgent(world);
        }

        public TargetableCharacter findTarget(String targetId) {
            if (targetId.trim().isEmpty()) {
                return null;
            }

            String[] targetIdParts = targetId.split(":");
            if (targetIdParts.length < 2) {
                throw new IllegalArgumentException("Illegal target ID: " + targetId);
            }

            Player player = world.getPlayer(parsePlayerName(targetIdParts[0]));
            String targetName = targetIdParts[1].trim();
            if (targetName.equalsIgnoreCase("hero")) {
                return player.getHero();
            }

            int minionIndex = Integer.parseInt(targetName);
            Minion minion = player.getBoard().getAllMinions().get(minionIndex);
            return minion;
        }

        public TargetId findTargetId(String targetId) {
            TargetableCharacter target = findTarget(targetId);
            return target != null ? target.getTargetId() : null;
        }

        public PlayTargetRequest toPlayTarget(PlayerId player, int minionPos, String targetId) {
            return new PlayTargetRequest(player, minionPos, findTargetId(targetId));
        }
    }

    *//**
     * Action unit of a script.
     *//*
    private static final class ScriptAction {
        private final Exception stackTrace;
        private final boolean expectationCheck;
        private final Function<ScriptAgent, void> action;

        public ScriptAction(boolean expectationCheck, Exception stackTrace, Function<ScriptAgent, void> action) {
            this.expectationCheck = expectationCheck;
            this.stackTrace = stackTrace;
            this.action = action;
        }

        public void doAction(ScriptAgent scriptAgent) {
            Deque<RollDef> recordedRolls = new LinkedList<>();
            Deque<CardChoiceDef> recodedChoices = new LinkedList<>();

            scriptAgent.userAgent.startRollRecording(recodedChoices);
            scriptAgent.randomProvider.startRollRecording(recordedRolls);
            action.apply(scriptAgent);
            scriptAgent.randomProvider.stopRollRecording();
            scriptAgent.userAgent.stopRollRecording();
        }
    }*/
}