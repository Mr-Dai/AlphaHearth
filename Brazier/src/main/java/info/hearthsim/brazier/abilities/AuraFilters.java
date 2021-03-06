package info.hearthsim.brazier.abilities;

import info.hearthsim.brazier.PlayerProperty;
import info.hearthsim.brazier.actions.ActionUtils;
import info.hearthsim.brazier.game.*;
import info.hearthsim.brazier.game.Character;
import info.hearthsim.brazier.game.cards.Card;
import info.hearthsim.brazier.game.minions.Minion;
import info.hearthsim.brazier.parsing.NamedArg;
import org.jtrim.utils.ExceptionHelper;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Predicate;

/**
 * Predefined {@link AuraFilter}s.
 */
public final class AuraFilters {
    /**
     * {@code AuraFilter}, which checks if the given source and target of the related aura has same owning player.
     */
    public static final AuraFilter<PlayerProperty, PlayerProperty> SAME_OWNER = (game, source, target) -> {
        return source.getOwner() == target.getOwner();
    };

    /**
     * {@code AuraFilter}, which checks if the given source and target of the related aura are different objects and
     * has same owning player.
     */
    public static final AuraFilter<PlayerProperty, PlayerProperty> SAME_OWNER_OTHERS = (game, source, target) -> {
        return source.getOwner() == target.getOwner() && source != target;
    };

    /**
     * {@code AuraFilter}, which checks if the owning player of the aura source has not played any minion in this
     * turn yet.
     */
    public static final AuraFilter<PlayerProperty, Object> NOT_PLAYED_MINION_THIS_TURN = (game, source, target) -> {
        return source.getOwner().getMinionsPlayedThisTurn() == 0;
    };

    /**
     * {@code AuraFilter}, which checks if the owning player of the aura source has weapon.
     */
    public static final AuraFilter<PlayerProperty, Object> OWNER_HAS_WEAPON = (game, source, target) -> {
        return source.getOwner().tryGetWeapon() != null;
    };

    /**
     * {@code AuraFilter}, which checks if the source and the target is not the same object.
     */
    public static final AuraFilter<PlayerProperty, PlayerProperty> NOT_SELF = (game, source, target) -> {
        return source != target;
    };

    /**
     * Returns an {@code AuraFilter}, which checks if the target has all given {@link Keyword}s.
     */
    public static <T extends LabeledEntity> AuraFilter<Object, T>
        targetHasKeyword(@NamedArg("keywords") Keyword... keywords) {

        List<Keyword> keywordsCopy = new ArrayList<>(Arrays.asList(keywords));
        ExceptionHelper.checkNotNullElements(keywordsCopy, "keywords");

        return (Game game, Object source, T target) -> {
            return target.getKeywords().containsAll(keywordsCopy);
        };
    }

    /**
     * Returns an {@code AuraFilter}, which checks if the target does not have any of the given {@link Keyword}s.
     */
    public static AuraFilter<Object, LabeledEntity> targetDoesntHaveKeyword(@NamedArg("keywords") Keyword... keywords) {
        Predicate<LabeledEntity> targetFilter = ActionUtils.excludedKeywordsFilter(keywords);

        return (Game game, Object source, LabeledEntity target) -> {
            return targetFilter.test(target);
        };
    }

    /**
     * Returns an {@code AuraFilter}, which checks if there is any minion on the same board side as the
     * aura source has all the given {@link Keyword}s.
     */
    public static AuraFilter<PlayerProperty, Object> ownBoardHas(@NamedArg("keywords") Keyword... keywords) {
        Predicate<LabeledEntity> minionFilter = ActionUtils.includedKeywordsFilter(keywords);

        return (Game game, PlayerProperty source, Object target) -> {
            BoardSide board = source.getOwner().getBoard();
            return board.findMinion(minionFilter) != null;
        };
    }

    /**
     * Returns an {@code AuraFilter}, which checks if the opponent of the aura source owner has more cards than
     * the given number in the hand.
     */
    public static AuraFilter<PlayerProperty, Object> opponentsHandLarger(@NamedArg("limit") int limit) {
        return (Game game, PlayerProperty source, Object target) -> {
            return source.getOwner().getOpponent().getHand().getCardCount() > limit;
        };
    }

    /**
     * {@code AuraFilter}, which checks if the target is a minion card.
     */
    public static final AuraFilter<Object, Card> IS_MINION_CARD = AuraFilter.and(
        targetHasKeyword(Keywords.MINION),
        (game, source, target) -> target.getCardDescr().getMinion() != null
    );

    /**
     * {@code AuraFilter}, which checks if the target is a spell card.
     */
    public static final AuraFilter<Object, Card> IS_SPELL_CARD = targetHasKeyword(Keywords.SPELL);

    /**
     * {@code AuraFilter}, which checks if the target is a battle cry minion card.
     */
    public static final AuraFilter<Object, Card> IS_BATTLE_CRY_CARD = targetHasKeyword(Keywords.BATTLE_CRY);
    
    /* AuraFilters for Minions */

    /**
     * {@link AuraFilter} which checks if the target is a demon minion.
     */
    public static final AuraFilter<Object, Minion> IS_DEMON_MINION = targetHasKeyword(Keywords.RACE_DEMON);

    /**
     * {@link AuraFilter} which checks if the target is a mech minion.
     */
    public static final AuraFilter<Object, Minion> IS_MECH_MINION = targetHasKeyword(Keywords.RACE_MECH);

    /**
     * {@link AuraFilter} which checks if the target is a beast minion.
     */
    public static final AuraFilter<Object, Minion> IS_BEAST_MINION = targetHasKeyword(Keywords.RACE_BEAST);

    /**
     * {@link AuraFilter} which checks if the target is a dragon minion.
     */
    public static final AuraFilter<Object, Minion> IS_DRAGON_MINION = targetHasKeyword(Keywords.RACE_DRAGON);

    /**
     * {@link AuraFilter} which checks if the target is a pirate minion.
     */
    public static final AuraFilter<Object, Minion> IS_PIRATE_MINION = targetHasKeyword(Keywords.RACE_PIRATE);

    /**
     * {@link AuraFilter} which checks if the target is a murloc minion.
     */
    public static final AuraFilter<Object, Minion> IS_MURLOC_MINION = targetHasKeyword(Keywords.RACE_MURLOC);

    /**
     * {@link AuraFilter} which checks if there is demon minion on the same board side as the aura source.
     */
    public static final AuraFilter<PlayerProperty, Object> OWN_BOARD_HAS_DEMON = ownBoardHas(Keywords.RACE_DEMON);

    /**
     * {@link AuraFilter} which checks if there is mech minion on the same board side as the aura source.
     */
    public static final AuraFilter<PlayerProperty, Object> OWN_BOARD_HAS_MECH = ownBoardHas(Keywords.RACE_MECH);

    /**
     * {@link AuraFilter} which checks if there is beast minion on the same board side as the aura source.
     */
    public static final AuraFilter<PlayerProperty, Object> OWN_BOARD_HAS_BEAST = ownBoardHas(Keywords.RACE_BEAST);

    /**
     * {@link AuraFilter} which checks if there is dragon minion on the same board side as the aura source.
     */
    public static final AuraFilter<PlayerProperty, Object> OWN_BOARD_HAS_DRAGON = ownBoardHas(Keywords.RACE_DRAGON);

    /**
     * {@link AuraFilter} which checks if the aura source is damaged.
     */
    public static final AuraFilter<Character, Object> SELF_DAMAGED =
        (game, source, target) -> source.isDamaged();

    /**
     * {@link AuraFilter} which checks if the target minion is next to the aura source minion.
     */
    public static final AuraFilter<Minion, Minion> IS_NEXT_MINION = (game, source, target) -> {
        BoardSide board = source.getOwner().getBoard();
        int sourceIndex = board.indexOf(source.getEntityId());
        int targetIndex = board.indexOf(source.getEntityId());

        return targetIndex != -1 && Math.abs(targetIndex - sourceIndex) == 1;
    };

    /**
     * Returns an {@link AuraFilter} which checks if the target {@link Minion} has the given name.
     */
    public static AuraFilter<Object, Minion> minionTargetNameIs(@NamedArg("name") String name) {
        ExceptionHelper.checkNotNullArgument(name, "name");
        return (Game game, Object owner, Minion target) -> {
            return name.equals(target.getBaseDescr().getId().getName());
        };
    }

    private AuraFilters() {
        throw new AssertionError();
    }
}
