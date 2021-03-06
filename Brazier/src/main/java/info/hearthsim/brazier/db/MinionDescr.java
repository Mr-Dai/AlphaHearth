package info.hearthsim.brazier.db;

import info.hearthsim.brazier.abilities.Ability;
import info.hearthsim.brazier.abilities.LivingEntitiesAbilities;
import info.hearthsim.brazier.abilities.OwnedIntPropertyBuff;
import info.hearthsim.brazier.actions.PlayActionDef;
import info.hearthsim.brazier.actions.PlayArg;
import info.hearthsim.brazier.events.EventAction;
import info.hearthsim.brazier.events.TriggeringAbility;
import info.hearthsim.brazier.game.Keyword;
import info.hearthsim.brazier.game.Player;
import info.hearthsim.brazier.game.cards.PlayAction;
import info.hearthsim.brazier.game.minions.Minion;
import info.hearthsim.brazier.game.minions.MinionName;
import org.jtrim.collections.CollectionsEx;
import org.jtrim.utils.ExceptionHelper;

import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

/**
 * A description of a minion, which describes a kind of minions in hearthstone.
 * <p>
 * Unlike {@link Minion} who stands for in-game minion and different {@code Minion} instances may have the same name,
 * {@code MinionDescr} stands for the game-irrelevant properties of a kind of minion, by which different
 * {@code MinionDescr} must have different names.
 */
public final class MinionDescr implements HearthStoneEntity {
    private final MinionName minionId;
    private final String displayName;
    private final Supplier<CardDescr> baseCardRef;
    private final int attack;
    private final int hp;
    private final Set<Keyword> keywords;
    private final List<PlayActionDef<Minion>> battleCries;
    private final LivingEntitiesAbilities<Minion> abilities;
    private final boolean taunt;
    private final boolean divineShield;
    private final boolean charge;
    private final boolean canAttack;
    private final int maxAttackCount;
    private final boolean targetable;
    private final boolean stealth;
    private final boolean attackLeft;
    private final boolean attackRight;
    private final OwnedIntPropertyBuff<? super Minion> attackFinalizer;

    private MinionDescr(Builder builder) {
        this.minionId = builder.minionId;
        this.displayName = builder.displayName;
        this.baseCardRef = new CachedSupplier<>(builder.baseCardRef);
        this.attack = builder.attack;
        this.hp = builder.hp;
        this.keywords = Collections.unmodifiableSet(new HashSet<>(builder.keywords));
        this.battleCries = CollectionsEx.readOnlyCopy(builder.battleCries);
        this.abilities = builder.abilities;
        this.taunt = builder.taunt;
        this.divineShield = builder.divineShield;
        this.charge = builder.charge;
        this.canAttack = builder.canAttack;
        this.attackFinalizer = builder.attackFinalizer;
        this.maxAttackCount = builder.maxAttackCount;
        this.targetable = builder.targetable;
        this.stealth = builder.stealth;
        this.attackLeft = builder.attackLeft;
        this.attackRight = builder.attackRight;
    }

    public Ability<? super Minion> tryGetAbility() {
        return abilities.tryGetAbility();
    }

    public EventAction<? super Minion, ? super Minion> tryGetDeathRattle() {
        return abilities.tryGetDeathRattle();
    }

    public boolean isAttackLeft() {
        return attackLeft;
    }

    public boolean isAttackRight() {
        return attackRight;
    }

    public boolean isStealth() {
        return stealth;
    }

    public boolean isTargetable() {
        return targetable;
    }

    public boolean isCanAttack() {
        return canAttack;
    }

    public String getDisplayName() {
        return displayName;
    }

    public OwnedIntPropertyBuff<? super Minion> getAttackFinalizer() {
        return attackFinalizer;
    }

    @Override
    public MinionName getId() {
        return minionId;
    }

    @Override
    public Set<Keyword> getKeywords() {
        return keywords;
    }

    public CardDescr getBaseCard() {
        CardDescr baseCard = baseCardRef.get();
        if (baseCard == null) {
            throw new IllegalStateException("Base card is not available for minion: " + minionId);
        }
        return baseCard;
    }

    public int getAttack() {
        return attack;
    }

    public int getMaxAttackCount() {
        return maxAttackCount;
    }

    public int getHp() {
        return hp;
    }

    public boolean isTaunt() {
        return taunt;
    }

    public boolean isDivineShield() {
        return divineShield;
    }

    public boolean isCharge() {
        return charge;
    }

    public List<PlayActionDef<Minion>> getBattleCries() {
        return battleCries;
    }

    public void executeBattleCriesNow(Player player, PlayArg<Minion> target) {
        ExceptionHelper.checkNotNullArgument(player, "player");
        ExceptionHelper.checkNotNullArgument(target, "target");

        if (battleCries.isEmpty())
            return;

        List<PlayAction<Minion>> actions
                = new ArrayList<>(battleCries.size());
        for (PlayActionDef<Minion> action: battleCries) {
            if (action.getRequirement().meetsRequirement(player)) {
                actions.add(action.getAction());
            }
        }

        if (actions.isEmpty())
            return;

        for (PlayAction<Minion> action: actions)
            action.doPlay(target);
    }

    public TriggeringAbility<Minion> getEventActionDefs() {
        return abilities.getTriggers();
    }

    private static final class CachedSupplier<T> implements Supplier<T> {
        private final Supplier<? extends T> src;
        private final AtomicReference<T> cache;

        public CachedSupplier(Supplier<? extends T> src) {
            this.src = src;
            this.cache = new AtomicReference<>(null);
        }

        @Override
        public T get() {
            T result = cache.get();
            if (result == null) {
                result = src.get();
                if (!cache.compareAndSet(null, result)) {
                    result = cache.get();
                }
            }
            return result;
        }
    }

    public static final class Builder {
        private final MinionName minionId;
        private String displayName;
        private final int attack;
        private final int hp;
        private final Supplier<? extends CardDescr> baseCardRef;

        private final Set<Keyword> keywords;
        private final List<PlayActionDef<Minion>> battleCries;
        private boolean taunt;
        private boolean charge;
        private boolean canAttack;
        private LivingEntitiesAbilities<Minion> abilities;
        private boolean divineShield;
        private int maxAttackCount;
        private boolean targetable;
        private boolean stealth;
        private boolean attackLeft;
        private boolean attackRight;

        private OwnedIntPropertyBuff<? super Minion> attackFinalizer;

        public Builder(MinionName minionId, int attack, int hp, Supplier<? extends CardDescr> baseCardRef) {
            ExceptionHelper.checkNotNullArgument(minionId, "minionId");
            ExceptionHelper.checkNotNullArgument(baseCardRef, "baseCardRef");

            this.minionId = minionId;
            this.attack = attack;
            this.hp = hp;
            this.baseCardRef = baseCardRef;
            this.displayName = minionId.getName();
            this.keywords = new HashSet<>();
            this.battleCries = new LinkedList<>();
            this.taunt = false;
            this.divineShield = false;
            this.charge = false;
            this.targetable = true;
            this.stealth = false;
            this.maxAttackCount = 1;
            this.canAttack = true;
            this.abilities = LivingEntitiesAbilities.noAbilities();
            this.attackLeft = false;
            this.attackRight = false;
            this.attackFinalizer = OwnedIntPropertyBuff.IDENTITY;
        }

        public void setAbilities(LivingEntitiesAbilities<Minion> abilities) {
            ExceptionHelper.checkNotNullArgument(abilities, "abilities");
            this.abilities = abilities;
        }

        public void setDisplayName(String displayName) {
            ExceptionHelper.checkNotNullArgument(displayName, "displayName");
            this.displayName = displayName;
        }

        public void setCanAttack(boolean canAttack) {
            this.canAttack = canAttack;
        }

        public void setAttackFinalizer(OwnedIntPropertyBuff<? super Minion> attackFinalizer) {
            ExceptionHelper.checkNotNullArgument(attackFinalizer, "attackFinalizer");
            this.attackFinalizer = attackFinalizer;
        }

        public void setAttackLeft(boolean attackLeft) {
            this.attackLeft = attackLeft;
        }

        public void setAttackRight(boolean attackRight) {
            this.attackRight = attackRight;
        }

        public void setStealth(boolean stealth) {
            this.stealth = stealth;
        }

        public void setTargetable(boolean targetable) {
            this.targetable = targetable;
        }

        public void setMaxAttackCount(int maxAttackCount) {
            this.maxAttackCount = maxAttackCount;
        }

        public void setCharge(boolean charge) {
            this.charge = charge;
        }

        public void setDivineShield(boolean divineShield) {
            this.divineShield = divineShield;
        }

        public void setTaunt(boolean taunt) {
            this.taunt = taunt;
        }

        public void addKeyword(Keyword keyword) {
            ExceptionHelper.checkNotNullArgument(keyword, "keyword");
            keywords.add(keyword);
        }

        public void addBattleCry(PlayActionDef<Minion> battleCry) {
            ExceptionHelper.checkNotNullArgument(battleCry, "battleCry");
            battleCries.add(battleCry);
        }

        public MinionDescr create() {
            return new MinionDescr(this);
        }
    }
}
