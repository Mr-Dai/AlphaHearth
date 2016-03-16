package info.hearthsim.brazier.actions;

import info.hearthsim.brazier.Character;
import info.hearthsim.brazier.actions.undo.UndoableUnregisterAction;
import info.hearthsim.brazier.minions.Minion;
import info.hearthsim.brazier.minions.MinionDescr;
import info.hearthsim.brazier.actions.undo.UndoableIntResult;
import info.hearthsim.brazier.actions.undo.UndoableRegistry;
import info.hearthsim.brazier.actions.undo.UndoableResult;
import info.hearthsim.brazier.abilities.Ability;
import info.hearthsim.brazier.abilities.HpProperty;
import info.hearthsim.brazier.cards.Card;
import info.hearthsim.brazier.cards.CardDescr;
import info.hearthsim.brazier.events.GameActionEvents;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import java.util.function.Predicate;

import info.hearthsim.brazier.actions.undo.UndoAction;
import info.hearthsim.brazier.actions.undo.UndoableAction;
import info.hearthsim.brazier.*;
import org.jtrim.utils.ExceptionHelper;

public final class ActionUtils {

    /**
     * Tries to convert the given {@code Object} to a respective {@link Minion},
     * and returns {@code null} if failed to convert.
     */
    public static Minion tryGetMinion(Object obj) {
        if (obj instanceof Minion) {
            return (Minion) obj;
        }
        if (obj instanceof CardRef) {
            CardRef cardRef = (CardRef) obj;
            return cardRef.getCard().getMinion();
        }
        return null;
    }

    /**
     * Adjusts the {@link HpProperty} of the given {@link Character} with the given {@link Function}.
     */
    public static UndoAction adjustHp(Character character, Function<HpProperty, UndoAction> action) {
        HpProperty hp = tryGetHp(character);
        if (hp == null) {
            return UndoAction.DO_NOTHING;
        }
        return action.apply(hp);
    }

    /**
     * Tries to get the {@link HpProperty} of the given {@link Character}.
     * Returns {@code null} if failed to get.
     */
    public static HpProperty tryGetHp(Character character) {
        if (character instanceof Hero) {
            return ((Hero) character).getHp();
        } else if (character instanceof Minion) {
            return ((Minion) character).getBody().getHp();
        }
        return null;
    }

    /**
     * Returns a {@link Function} of {@link MinionDescr} which stores every minion with all of the given
     * {@link Keyword}s from the {@link HearthStoneEntityDatabase} of the given {@link Game}, and uses the
     * {@link RandomProvider} from the {@code Game} to randomly select a minion from it. {@code null} will be
     * returned by the result {@code Function} if no such minion can be found.
     */
    public static Function<Game, MinionDescr> randomMinionProvider(Keyword[] keywords) {
        Function<Game, List<CardDescr>> cardProvider = minionCardProvider(keywords);
        return (game) -> {
            List<CardDescr> cards = cardProvider.apply(game);
            CardDescr card = ActionUtils.pickRandom(game, cards);
            return card != null ? card.getMinion() : null;
        };
    }

    private static Function<Game, List<CardDescr>> minionCardProvider(Keyword[] keywords) {
        ExceptionHelper.checkNotNullElements(keywords, "keywords");

        Keyword[] cardKeywords = new Keyword[keywords.length + 1];
        cardKeywords[0] = Keywords.MINION;
        System.arraycopy(keywords, 0, cardKeywords, 1, keywords.length);

        AtomicReference<List<CardDescr>> cache = new AtomicReference<>(null);
        return (game) -> {
            List<CardDescr> result = cache.get();
            if (result == null) {
                result = game.getDb().getCardDb().getByKeywords(cardKeywords);
                if (!cache.compareAndSet(null, result)) {
                    result = cache.get();
                }
            }
            return result;
        };
    }

    /**
     * Attacks the given target with the given player's hero and the given amount of damage.
     *
     * @param player the given {@link Player}.
     * @param spell if the damage comes from a spell (should the damage be affected by spell power).
     * @param damage the amount of damage.
     * @param target the given {@link Character target}.
     */
    public static UndoAction attackWithHero(Player player, boolean spell, int damage, Character target) {
        return attackWithHero(player.getHero(), spell, damage, target);
    }

    /**
     * Attacks the given target with the given hero and the given amount of damage.
     *
     * @param hero the given {@link Hero}.
     * @param spell if the damage comes from a spell (should the damage be affected by spell power).
     * @param damage the amount of damage.
     * @param target the given {@link Character target}.
     */
    public static UndoAction attackWithHero(Hero hero, boolean spell, int damage, Character target) {
        if (!spell) {
            return damageCharacter(hero, damage, target);
        }

        Damage appliedDamage = hero.getOwner().getSpellDamage(damage);
        return target.damage(appliedDamage);
    }

    /**
     * Deals given amount of damage to the given {@link Character} with the given {@link DamageSource}.
     */
    public static UndoAction damageCharacter(DamageSource damageSource, int damage, Character target) {
        UndoableResult<Damage> damageRef = damageSource.createDamage(damage);
        UndoableIntResult damageUndo = target.damage(damageRef.getResult());
        return () -> {
            damageUndo.undo();
            damageRef.undo();
        };
    }

    /**
     * Randomly selects a {@link Character} from all {@code Character}s in the given {@link Game}.
     */
    public static Character rollTarget(Game game) {
        List<Character> result = new ArrayList<>(2 * (Player.MAX_BOARD_SIZE + 1));
        collectTargets(game.getPlayer1(), result);
        collectTargets(game.getPlayer2(), result);

        int roll = game.getRandomProvider().roll(result.size());
        return result.get(roll);
    }

    /**
     * Randomly selects a {@link Character} from all {@code Character}s in the given {@link Game}
     * which satisfy the given {@link Predicate}.
     */
    public static Character rollTarget(Game game, Predicate<? super Character> filter) {
        List<Character> result = new ArrayList<>(2 * (Player.MAX_BOARD_SIZE + 1));
        collectTargets(game.getPlayer1(), result, filter);
        collectTargets(game.getPlayer2(), result, filter);

        int roll = game.getRandomProvider().roll(result.size());
        return result.get(roll);
    }

    /**
     * Returns a randomly selected living {@link Character} which is on the board side
     * of the given {@link Player}.
     */
    public static Character rollAlivePlayerTarget(Game game, Player player) {
        return rollPlayerTarget(game, player, (target) -> !target.isDead());
    }

    /**
     * Returns a randomly selected {@link Character} which is on the board side
     * of the given {@link Player}.
     */
    public static Character rollPlayerTarget(Game game, Player player) {
        return rollPlayerTarget(game, player, (target) -> true);
    }

    /**
     * Returns a randomly selected {@link Character} which is on the board side of the given {@link Player}
     * and satisfies the given {@link Predicate}.
     */
    public static Character rollPlayerTarget(
        Game game,
        Player player,
        Predicate<? super Character> filter) {

        List<Character> result = new ArrayList<>(Player.MAX_BOARD_SIZE + 1);
        collectTargets(player, result, filter);

        int roll = game.getRandomProvider().roll(result.size());
        return result.get(roll);
    }

    /**
     * Collects all living {@link Character}s on the board side of the given {@link Player}
     * to the given {@link List}.
     */
    public static void collectAliveTargets(Player player, List<Character> result) {
        collectTargets(player, result, (target) -> !target.isDead());
    }

    /**
     * Collects all {@link Character}s on the board side of the given {@link Player}
     * to the given {@link List}.
     */
    public static void collectTargets(Player player, List<? super Character> result) {
        collectTargets(player, result, (target) -> true);
    }

    /**
     * Collects all living {@link Character}s on the board side of the given {@link Player} which satisfy
     * the given {@link Predicate} to the given {@link List}.
     */
    public static void collectAliveTargets(
        Player player,
        List<? super Character> result,
        Predicate<? super Character> filter) {
        collectTargets(player, result, (target) -> !target.isDead() && filter.test(target));
    }

    /**
     * Collects all {@link Character}s on the board which satisfy
     * the given {@link Predicate} to the given {@link List}.
     */
    public static void collectTargets(
        Game game,
        List<? super Character> result,
        Predicate<? super Character> filter) {
        collectTargets(game.getPlayer1(), result, filter);
        collectTargets(game.getPlayer2(), result, filter);
    }

    /**
     * Collects all {@link Character}s on the board side of the given {@link Player} which satisfy
     * the given {@link Predicate} to the given {@link List}.
     */
    public static void collectTargets(
        Player player,
        List<? super Character> result,
        Predicate<? super Character> filter) {
        ExceptionHelper.checkNotNullArgument(player, "player");
        ExceptionHelper.checkNotNullArgument(result, "result");
        ExceptionHelper.checkNotNullArgument(filter, "filter");

        if (filter.test(player.getHero())) {
            result.add(player.getHero());
        }
        player.getBoard().collectMinions(result, (minion) -> !minion.isScheduledToDestroy() && filter.test(minion));
    }

    /**
     * Tries to draw a card randomly from the given {@link Player}'s deck which satisfies the given predicate.
     * Returns {@code null} if no such card exist.
     */
    public static UndoableResult<Card> pollDeckForCard(
        Player player,
        Predicate<? super Card> cardFilter) {
        return player.getDeck().tryDrawRandom(player.getGame().getRandomProvider(), cardFilter);
    }

    /**
     * Returns a {@link Predicate} of {@link LabeledEntity} which checks if the given entity has every given
     * {@link Keyword}.
     */
    public static <E extends LabeledEntity> Predicate<E> includedKeywordsFilter(Keyword... includedKeywords) {
        if (includedKeywords.length == 0) {
            return (arg) -> true;
        }

        Keyword[] includedKeywordsCopy = includedKeywords.clone();
        ExceptionHelper.checkNotNullElements(includedKeywordsCopy, "includedKeywords");

        return (entity) -> {
            for (Keyword keyword : includedKeywordsCopy) {
                if (!entity.getKeywords().contains(keyword)) {
                    return false;
                }
            }
            return true;
        };
    }

    /**
     * Returns a {@link Predicate} of {@link LabeledEntity} which checks if the given entity does not have any given
     * {@link Keyword}.
     */
    public static <E extends LabeledEntity> Predicate<E> excludedKeywordsFilter(Keyword... excludedKeywords) {
        Keyword[] excludedKeywordsCopy = excludedKeywords.clone();
        ExceptionHelper.checkNotNullElements(excludedKeywordsCopy, "excludedKeywords");

        return (entity) -> {
            Set<Keyword> minionKeywords = entity.getKeywords();
            for (Keyword keyword : excludedKeywordsCopy) {
                if (minionKeywords.contains(keyword)) {
                    return false;
                }
            }
            return true;
        };
    }

    /**
     * Returns a randomly selected living {@link Minion} which satisfies the given {@link Predicate}.
     */
    public static Minion rollAliveMinionTarget(Game game, Predicate<? super Minion> minionFilter) {
        ExceptionHelper.checkNotNullArgument(game, "game");
        ExceptionHelper.checkNotNullArgument(minionFilter, "minionFilter");

        List<Minion> result = new ArrayList<>(2 * Player.MAX_BOARD_SIZE);

        Predicate<Minion> includeMinion = (minion) -> !minion.isDead();
        includeMinion = includeMinion.and(minionFilter);

        game.getPlayer1().getBoard().collectMinions(result, includeMinion);
        game.getPlayer2().getBoard().collectMinions(result, includeMinion);

        if (result.isEmpty()) {
            return null;
        }

        int roll = game.getRandomProvider().roll(result.size());
        return result.get(roll);
    }

    /**
     * Selects a random element from the given {@link List} by using the {@link RandomProvider}
     * from the given {@link Game}.
     */
    // TODO Change the `Game` parameter to `RandomProvider`
    public static <T> T pickRandom(Game game, List<? extends T> list) {
        int size = list.size();
        if (size == 0) {
            return null;
        }

        int index = game.getRandomProvider().roll(size);
        return list.get(index);
    }

    /**
     * Selects a random element from the given array by using the {@link RandomProvider} from the given
     * {@link Game}.
     */
    public static <T> T pickRandom(Game game, T[] list) {
        if (list.length == 0) {
            return null;
        }

        int index = game.getRandomProvider().roll(list.length);
        return list[index];
    }

    /**
     * Selects given number of random elements from the given {@link List} by using the {@link RandomProvider}
     * from the given {@link Game}.
     */
    public static <T> List<T> pickMultipleRandom(Game game, int count, List<? extends T> list) {
        int size = list.size();
        if (size == 0 || count <= 0) {
            return Collections.emptyList();
        }
        if (size == 1) {
            return Collections.singletonList(list.get(0));
        }

        RandomProvider rng = game.getRandomProvider();
        if (count == 1) {
            return Collections.singletonList(list.get(rng.roll(size)));
        }

        int[] indexes = new int[size];
        for (int i = 0; i < indexes.length; i++) {
            indexes[i] = i;
        }

        List<T> result = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            int indexOfIndex = rng.roll(indexes.length - i);
            int choice = indexes[indexOfIndex];
            indexes[indexOfIndex] = indexes[indexes.length - i - 1];

            result.add(list.get(choice));
        }
        return result;
    }

    /**
     * Executes the given {@link UndoableAction} on the end of the current turn of the given {@link Game}.
     */
    public static UndoAction doOnEndOfTurn(Game game, UndoableAction action) {
        GameActionEvents<Player> listeners = game.getEvents().turnEndsListeners();

        AtomicReference<UndoableUnregisterAction> unregisterRef = new AtomicReference<>();
        UndoableUnregisterAction unregisterAction = listeners.addAction((Game eventGame, Player player) -> {
            UndoAction unregisterUndo = unregisterRef.get().unregister();
            UndoAction actionUndo = action.doAction();

            return () -> {
                actionUndo.undo();
                unregisterUndo.undo();
            };
        });
        unregisterRef.set(unregisterAction);

        return unregisterAction;
    }

    /**
     * Converts the given {@link Ability} to a single-turn ability, which will be unregistered at
     * end of turn.
     */
    public static <Self> Ability<Self> toSingleTurnAbility(
        Game game,
        Ability<Self> ability) {
        ExceptionHelper.checkNotNullArgument(game, "game");
        ExceptionHelper.checkNotNullArgument(ability, "ability");

        return (Self self) -> {
            UndoableUnregisterAction unregisterAction = ability.activate(self);
            UndoAction unregUndo = unregisterAfterTurnEnds(game, unregisterAction);

            return new UndoableUnregisterAction() {
                @Override
                public UndoAction unregister() {
                    return unregisterAction.unregister();
                }

                @Override
                public void undo() {
                    unregUndo.undo();
                    unregisterAction.undo();
                }
            };
        };
    }

    private static UndoAction unregisterAfterTurnEnds(Game game, UndoableUnregisterAction unregisterAction) {
        GameActionEvents<Player> turnEndsListeners = game.getEvents().turnEndsListeners();
        return turnEndsListeners.addAction((Game taskGame, Player object) -> unregisterAction.unregister());
    }

    /**
     * Executes the given {@link UndoableRegistry} and unregisters by using the {@link UndoableUnregisterAction}
     * it returns at the end of the current turn of the given {@link Game}.
     */
    public static UndoAction doTemporary(Game game, UndoableRegistry registerAction) {
        UndoableUnregisterAction buffRef = registerAction.register();
        UndoAction unregUndo = unregisterAfterTurnEnds(game, buffRef);

        return () -> {
            unregUndo.undo();
            buffRef.undo();
        };
    }

    /**
     * Returns an {@link Ability}, which will be unregistered at the start of a new turn of the given
     * {@link PlayerProperty}'s owner after it is activated.
     */
    public static <Self> Ability<Self> toUntilTurnStartsAbility(
        Game game,
        PlayerProperty turnOwner,
        Ability<Self> ability) {
        ExceptionHelper.checkNotNullArgument(game, "game");
        ExceptionHelper.checkNotNullArgument(turnOwner, "turnOwner");
        ExceptionHelper.checkNotNullArgument(ability, "ability");

        return (Self self) -> {
            UndoableUnregisterAction unregisterAction = ability.activate(self);
            UndoAction unregUndo = unregisterOnNextTurn(game, turnOwner, unregisterAction);

            return new UndoableUnregisterAction() {
                @Override
                public UndoAction unregister() {
                    return unregisterAction.unregister();
                }

                @Override
                public void undo() {
                    unregUndo.undo();
                    unregisterAction.undo();
                }
            };
        };
    }

    private static UndoAction unregisterOnNextTurn(
        Game game,
        PlayerProperty turnOwner,
        UndoableUnregisterAction unregisterAction) {
        GameActionEvents<Player> turnStartsListeners = game.getEvents().turnStartsListeners();
        return turnStartsListeners.addAction((Game taskGame, Player actionPlayer) -> {
            if (actionPlayer == turnOwner.getOwner()) {
                return unregisterAction.unregister();
            } else {
                return UndoAction.DO_NOTHING;
            }
        });
    }

    /**
     * Executes the given {@link UndoableRegistry} and unregister it by using the {@link UndoableUnregisterAction}
     * it returns at the start of a new turn of the given {@link PlayerProperty}'s owner.
     */
    public static UndoAction doUntilNewTurnStart(Game game, PlayerProperty turnOwner, UndoableRegistry registerAction) {
        UndoableUnregisterAction buffRef = registerAction.register();
        UndoAction unregUndo = unregisterOnNextTurn(game, turnOwner, buffRef);

        return () -> {
            unregUndo.undo();
            buffRef.undo();
        };
    }

    private ActionUtils() {
        throw new AssertionError();
    }
}
