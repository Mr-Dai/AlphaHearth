package info.hearthsim.brazier.minions;

import info.hearthsim.brazier.Keyword;
import info.hearthsim.brazier.Damage;
import info.hearthsim.brazier.DestroyableEntity;
import info.hearthsim.brazier.Hero;
import info.hearthsim.brazier.Player;
import info.hearthsim.brazier.PreparedResult;
import info.hearthsim.brazier.Silencable;
import info.hearthsim.brazier.TargetId;
import info.hearthsim.brazier.TargetableCharacter;
import info.hearthsim.brazier.TargeterDef;
import info.hearthsim.brazier.actions.undo.UndoableIntResult;
import info.hearthsim.brazier.actions.undo.UndoableResult;
import info.hearthsim.brazier.abilities.ActivatableAbility;
import info.hearthsim.brazier.abilities.AuraAwareIntProperty;
import info.hearthsim.brazier.actions.CardRef;
import info.hearthsim.brazier.actions.undo.UndoAction;
import info.hearthsim.brazier.cards.Card;
import info.hearthsim.brazier.events.SimpleEventType;
import info.hearthsim.brazier.events.WorldEventAction;
import info.hearthsim.brazier.events.WorldEvents;
import info.hearthsim.brazier.weapons.AttackTool;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import org.jtrim.utils.ExceptionHelper;

public final class Minion implements TargetableCharacter, DestroyableEntity, Silencable, CardRef {
    private TargetId minionId;
    private Player owner;
    private MinionProperties properties;

    private final long birthDate;

    private final AtomicBoolean scheduledToDestroy;
    private final AtomicBoolean destroyed;

    /**
     * Creates a {@code Minion} with the given {@code MinionDescr} and the given {@code Player} as its owner.
     *
     * @param owner the given {@code Player}.
     * @param baseDescr the given {@code MinionDescr}.
     */
    public Minion(Player owner, MinionDescr baseDescr) {
        ExceptionHelper.checkNotNullArgument(owner, "owner");
        ExceptionHelper.checkNotNullArgument(baseDescr, "baseDescr");

        this.owner = owner;
        this.minionId = new TargetId();
        this.properties = new MinionProperties(this, baseDescr);
        this.birthDate = owner.getWorld().getCurrentTime();
        this.destroyed = new AtomicBoolean(false);
        this.scheduledToDestroy = new AtomicBoolean(false);
    }

    @Override
    public UndoAction scheduleToDestroy() {
        if (!scheduledToDestroy.compareAndSet(false, true)) {
            return UndoAction.DO_NOTHING;
        }

        UndoAction deactivateUndo = getProperties().deactivateAllAbilities();
        getOwner().getBoard().scheduleToDestroy(minionId);
        return () -> {
            deactivateUndo.undo();
            scheduledToDestroy.set(false);
        };
    }

    @Override
    public Card getCard() {
        return new Card(owner, getBaseDescr().getBaseCard());
    }

    public boolean notScheduledToDestroy() {
        return !isScheduledToDestroy();
    }

    @Override
    public boolean isScheduledToDestroy() {
        return scheduledToDestroy.get();
    }

    /**
     * Destroys (or kills) the minion. A {@link SimpleEventType#MINION_KILLED MINION_KILLED} event
     * and the minion's death-rattle effect should be triggered.
     */
    @Override
    public UndoAction destroy() {
        UndoAction.Builder builder = new UndoAction.Builder(2);

        // By now, the `scheduleToDestroy` method has already been invoked,
        // and the `needSpace` of the dead minion has been set to `false`.
        builder.addUndo(completeKillAndDeactivate(true));
        builder.addUndo(owner.getBoard().removeFromBoard(minionId));

        return builder;
    }

    /**
     * Transforms this {@code Minion} to another minion described by the given {@link MinionDescr}.
     * All previously-added buffs and abilities on this minion will be removed and a brand-new version
     * of the given minion will spawn on its original location.
     */
    public UndoAction transformTo(MinionDescr newDescr) {
        ExceptionHelper.checkNotNullArgument(newDescr, "newDescr");

        UndoAction.Builder builder = new UndoAction.Builder();

        builder.addUndo(properties.deactivateAllAbilities());

        MinionProperties prevProperties = properties;
        properties = new MinionProperties(this, newDescr);
        builder.addUndo(() -> properties = prevProperties);

        builder.addUndo(properties.activatePassiveAbilities());

        return builder;
    }

    /**
     * Transforms this {@code Minion} to a copy of the given {@code Minion}.
     * All previously-added buffs and abilities on the given minion will be copied to the transformed minion.
     * The minion will be <em>exhausted</em> when it is transformed.
     */
    public UndoAction copyOther(Minion other) {
        ExceptionHelper.checkNotNullArgument(other, "other");

        UndoAction.Builder builder = new UndoAction.Builder();

        builder.addUndo(properties.deactivateAllAbilities());

        PreparedResult<MinionProperties> copiedProperties = other.properties.copyFor(this);

        MinionProperties prevProperties = properties;
        properties = copiedProperties.getResult();
        builder.addUndo(() -> properties = prevProperties);
        builder.addUndo(copiedProperties.activate());
        builder.addUndo(properties.exhaust());

        return builder;
    }

    /**
     * Sets the minion to be exhausted.
     */
    public UndoAction exhaust() {
        return properties.exhaust();
    }

    /**
     * Returns if the minion has <b>Charge</b> effect.
     */
    public boolean isCharge() {
        return properties.isCharge();
    }

    @Override
    public long getBirthDate() {
        return birthDate;
    }

    /**
     * Returns if the minion can be targeted by the targeting attempt designated by the given {@link TargeterDef}.
     */
    @Override
    public boolean isTargetable(TargeterDef targeterDef) {
        boolean sameOwner = targeterDef.hasSameOwner(this);
        if (!sameOwner && getBody().isStealth()) {
            return false;
        }
        if (sameOwner && targeterDef.isDirectAttack()) {
            return false;
        }

        MinionBody body = properties.getBody();

        if (!body.isTargetable() && targeterDef.isHero()) {
            return false;
        }

        if (body.isTaunt()) {
            return true;
        }

        return !targeterDef.isDirectAttack() || !getOwner().getBoard().hasNonStealthTaunt();
    }

    /**
     * Adds the given {@link WorldEventAction} as a death rattle effect to this minion.
     */
    public UndoAction addDeathRattle(WorldEventAction<? super Minion, ? super Minion> deathRattle) {
        return properties.addDeathRattle(deathRattle);
    }

    /**
     * Adds the given {@link ActivatableAbility} to this minion and activates it.
     */
    public UndoAction addAndActivateAbility(ActivatableAbility<? super Minion> abilityRegisterTask) {
        return properties.addAndActivateAbility(abilityRegisterTask);
    }

    /**
     * Activates the passive abilities of this minion.
     */
    public UndoAction activatePassiveAbilities() {
        return properties.activatePassiveAbilities();
    }

    /**
     * Returns the {@link MinionProperties} of this {@code Minion}.
     */
    public MinionProperties getProperties() {
        return properties;
    }

    /**
     * Triggers a {@link SimpleEventType#MINION_KILLED} event for this minion.
     */
    private UndoAction triggerKilledEvents() {
        WorldEvents events = getOwner().getWorld().getEvents();
        return events.triggerEvent(SimpleEventType.MINION_KILLED, this);
    }

    /**
     * Returns the {@link Keyword}s of this {@code Minion}.
     */
    @Override
    public Set<Keyword> getKeywords() {
        return getBaseDescr().getBaseCard().getKeywords();
    }

    /**
     * Kills or removes the minion from board.
     *
     * @param triggerKill whether to trigger {@link SimpleEventType#MINION_KILLED MINION_KILLED} event
     *                    and the minion's deathrattle effect in this action.
     */
    public UndoAction completeKillAndDeactivate(boolean triggerKill) {
        if (destroyed.compareAndSet(false, true)) {
            UndoAction eventUndo = triggerKill
                    ? triggerKilledEvents()
                    : UndoAction.DO_NOTHING;

            UndoAction deactivateUndo = properties.deactivateAllAbilities();
            UndoAction deathRattleUndo = triggerKill
                    ? triggerDeathRattles()
                    : UndoAction.DO_NOTHING;

            return () -> {
                deathRattleUndo.undo();
                deactivateUndo.undo();
                eventUndo.undo();
                destroyed.set(false);
            };
        }
        else {
            return UndoAction.DO_NOTHING;
        }
    }

    /**
     * Triggers the death rattle effects of this minion. The death rattle effects may be trigger multiple times
     * due to some specific aura effect.
     * <p>
     * See minion <em>Baron Rivendare</em>.
     */
    public UndoAction triggerDeathRattles() {
        if (!properties.isDeathRattle()) {
            return UndoAction.DO_NOTHING;
        }

        int triggerCount = getOwner().getDeathRattleTriggerCount().getValue();
        return properties.triggerDeathRattles(triggerCount);
    }

    @Override
    public UndoAction silence() {
        return properties.silence();
    }

    @Override
    public Player getOwner() {
        return owner;
    }

    @Override
    public UndoAction kill() {
        return getBody().poison();
    }

    public void setOwner(Player owner) {
        ExceptionHelper.checkNotNullArgument(owner, "owner");
        this.owner = owner;
    }

    public AuraAwareIntProperty getBuffableAttack() {
        return properties.getBuffableAttack();
    }

    @Override
    public AttackTool getAttackTool() {
        return properties.getAttackTool();
    }

    /**
     * Returns {@code true} if this minion has been completely destroyed and might no longer
     * be added to the board.
     *
     * @return {@code true} if this minion has been completely destroyed and might no longer
     *   be added to the board, {@code false} otherwise
     */
    public boolean isDestroyed() {
        return destroyed.get();
    }

    @Override
    public boolean isDead() {
        return getBody().isDead();
    }

    @Override
    public boolean isDamaged() {
        MinionBody body = getBody();
        return body.getCurrentHp() < body.getMaxHp();
    }

    @Override
    public TargetId getTargetId() {
        return minionId;
    }

    @Override
    public UndoAction setTargetId(TargetId targetId) {
        TargetId oldId = minionId;
        minionId = targetId;
        return () -> minionId = oldId;
    }

    public MinionDescr getBaseDescr() {
        return getBody().getBaseStats();
    }

    public MinionBody getBody() {
        return properties.getBody();
    }

    public UndoAction setCharge(boolean newCharge) {
        return properties.setCharge(newCharge);
    }

    @Override
    public UndoableResult<Damage> createDamage(int damage) {
        int preparedDamage = damage;
        if (damage < 0 && getOwner().getDamagingHealAura().getValue()) {
            preparedDamage = -damage;
        }
        return new UndoableResult<>(new Damage(this, preparedDamage), getBody().setStealth(false));
    }

    @Override
    public boolean isLethalDamage(int damage) {
        return getBody().isLethalDamage(damage);
    }

    @Override
    public UndoableIntResult damage(Damage damage) {
        return Hero.doPreparedDamage(damage, this, (appliedDamage) -> getBody().damage(appliedDamage));
    }

    /**
     * Refreshes the state of the minion at the start of a turn. That is,
     * sleeping minion will be awakened and the number of attacks will reset.
     */
    public UndoAction refreshStartOfTurn() {
        return properties.refreshStartOfTurn();
    }

    /**
     * Refreshes the state of the minion at the end of a turn. That is,
     * frozen minion will be unfrozen.
     */
    public UndoAction refreshEndOfTurn() {
        return properties.refreshEndOfTurn();
    }

    public UndoAction applyAuras() {
        return properties.updateAuras();
    }

    @Override
    public String toString() {
        MinionBody body = getBody();
        AttackTool attackTool = getAttackTool();
        int attack = attackTool.getAttack();

        MinionId id = body.getBaseStats().getId();
        int currentHp = body.getCurrentHp();

        StringBuilder result = new StringBuilder(64);
        result.append("Minion(");
        result.append(id);
        result.append(") ");
        result.append(attack);
        result.append("/");
        result.append(currentHp);

        if (body.isTaunt()) {
            result.append(" ");
            result.append(" TAUNT");
        }

        if (properties.isFrozen()) {
            result.append(" ");
            result.append(" FROZEN");
        }

        if (!body.isTargetable()) {
            result.append(" ");
            result.append(" UNTARGETABLE");
        }

        return result.toString();
    }
}
