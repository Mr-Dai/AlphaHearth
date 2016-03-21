package info.hearthsim.brazier.events;

import info.hearthsim.brazier.GameProperty;
import info.hearthsim.brazier.actions.Action;
import info.hearthsim.brazier.actions.GameActionList;
import info.hearthsim.brazier.minions.Minion;
import info.hearthsim.brazier.Player;
import info.hearthsim.brazier.Game;

import java.util.EnumMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import org.jtrim.utils.ExceptionHelper;

public final class GameEvents implements GameProperty {
    private final Game game;

    private final Map<SimpleEventType, GameActionEvents> simpleListeners;
    // private final CompletableEventContainer<Minion> summoningListeners;

    /**
     * Used in {@link #doAtomic(Action)} to collect all suspended event notifications.
     */
    private final AtomicReference<GameActionList<Game>> pauseCollectorRef;

    public GameEvents(Game game) {
        ExceptionHelper.checkNotNullArgument(game, "game");

        this.game = game;
        this.pauseCollectorRef = new AtomicReference<>(null);

        this.simpleListeners = new EnumMap<>(SimpleEventType.class);
        // this.summoningListeners = createCompletableGameActionEvents();
    }

    private GameEvents(Game game, GameEvents other) {
        ExceptionHelper.checkNotNullArgument(game, "game");
        ExceptionHelper.checkNotNullArgument(other, "other");

        this.game = game;
        GameActionList<Game> otherCollector = other.pauseCollectorRef.get();
        if (otherCollector != null)
            this.pauseCollectorRef = new AtomicReference<>(otherCollector.copy());
        else
            this.pauseCollectorRef = new AtomicReference<>(null);

        this.simpleListeners = new EnumMap<>(SimpleEventType.class);
        for (Map.Entry<SimpleEventType, GameActionEvents> entry : other.simpleListeners.entrySet())
            this.simpleListeners.put(entry.getKey(),
                createEventContainer(entry.getKey(), entry.getValue().getActionList().copy()));
        // this.summoningListeners = createCompletableGameActionEvents(other.summoningListeners.wrapped.copyFor(game));
    }

    /**
     * Returns a copy of this {@code GameEvents} for the given new {@code Game}.
     */
    public GameEvents copyFor(Game game) {
        return new GameEvents(game, this);
    }

    /**
     * Returns simple listener registered in this {@code GameEvents} which listens to the given type of event;
     * {@code null} if no such listener exists.
     */
    private <T extends GameProperty> GameActionEvents<T> tryGetSimpleListeners(SimpleEventType eventType) {
        ExceptionHelper.checkNotNullArgument(eventType, "eventType");

        @SuppressWarnings("unchecked")
        GameActionEvents<T> result = (GameActionEvents<T>) simpleListeners.get(eventType);
        return result;
    }

    /**
     * Returns the existed simple listeners with the given {@link SimpleEventType}; if there is no such listener,
     * creates a new one and returns.
     *
     * @param eventType the given {@code SimpleEventType}.
     * @return simple listeners with the given {@code SimpleEventType}.
     * @throws IllegalArgumentException if the given {@code argType} does not match the argument type of the given
     *                                  {@code SimpleEventType}.
     */
    public <T extends GameProperty> GameActionEvents<T> simpleListeners(SimpleEventType eventType) {
        GameActionEvents<T> result = tryGetSimpleListeners(eventType);
        if (result == null) {
            result = createEventContainer(eventType);
            simpleListeners.put(eventType, result);
        }

        return result;
    }

    /**
     * Triggers a given type of event with the given argument instantly.
     */
    public <T extends GameProperty> void triggerEventNow(SimpleEventType eventType, T arg) {
        triggerEvent(eventType, arg, false);
    }

    public <T extends GameProperty> void triggerEvent(SimpleEventType eventType, T arg) {
        triggerEvent(eventType, arg, true);
    }

    private <T extends GameProperty> void triggerEvent(SimpleEventType eventType, T arg, boolean delayable) {
        Class<?> expectedArgType = eventType.getArgumentType();
        if (!expectedArgType.isInstance(arg)) {
            throw new IllegalArgumentException("The requested listener has a different argument type."
                + " Requested: " + arg.getClass().getName()
                + ". Expected: " + eventType.getArgumentType());
        }

        @SuppressWarnings("unchecked")
        GameActionEvents<T> listeners = tryGetSimpleListeners(eventType);
        if (listeners != null)
            listeners.triggerEvent(delayable, arg);
    }

    public GameActionEvents<Minion> summoningListeners() {
        return simpleListeners(SimpleEventType.MINION_SUMMONED);
    }

    public GameActionEvents<Player> turnStartsListeners() {
        return simpleListeners(SimpleEventType.TURN_STARTS);
    }

    public GameActionEvents<Player> turnEndsListeners() {
        return simpleListeners(SimpleEventType.TURN_ENDS);
    }

    /**
     * Executes the given action, ensuring that it won't be interrupted by event notifications
     * scheduled to any of the event listeners of this {@code GameEvents} object. If the
     * given action would be interrupted by an event notification, the event notification
     * is suspended until the specified action returns.
     * <p>
     * Calls to {@code doAtomic} can be nested in which case event notifications will
     * be executed once the outer most atomic action returns.
     * <p>
     * <B>Warning</B>: This method is <B>not</B> thread-safe.
     *
     * @param action the action to be executed. This argument cannot be {@code null}.
     */
    public void doAtomic(Action action) {
        ExceptionHelper.checkNotNullArgument(action, "action");

        if (pauseCollectorRef.get() != null) {
            // A caller already ensures this call to be atomic
            action.act();
        } else {
            GameActionList<Game> currentCollector = new GameActionList<>();
            try {
                pauseCollectorRef.compareAndSet(null, currentCollector);
                action.act();
            } finally {
                pauseCollectorRef.compareAndSet(currentCollector, null);
            }

            currentCollector.executeActionsNow(game, false);
        }
    }

    /**
     * Creates and returns a new {@link GameActionEvents} for the given type of event.
     * It uses an underlying {@link GameActionList} as its implementation.
     */
    private <T extends GameProperty> GameActionEvents<T> createEventContainer(SimpleEventType eventType) {
        return createEventContainer(eventType, new GameActionList<>());
    }

    /**
     * Creates and returns a new {@link GameActionEvents} for the given type of event,
     * using the given {@link GameActionList} as its underlying implementation.
     */
    private <T extends GameProperty> GameActionEvents<T> createEventContainer(SimpleEventType eventType,
                                                                              GameActionList<T> actionList) {
        boolean greedyEvent = eventType.isGreedyEvent();
        return new GameActionEvents<>(greedyEvent, actionList, pauseCollectorRef);
    }

    @Override
    public Game getGame() {
        return game;
    }
/*
    private <T> CompletableEventContainer<T> createCompletableGameActionEvents() {
        return createCompletableGameActionEvents(new DefaultCompletableGameActionEvents<>(game));
    }

    private <T> CompletableEventContainer<T>
        createCompletableGameActionEvents(DefaultCompletableGameActionEvents<T> wrapped) {
        return new CompletableEventContainer<>(wrapped);
    }

    private final class CompletableEventContainer<T> implements CompletableGameActionEvents<T> {
        private final DefaultCompletableGameActionEvents<T> wrapped;

        CompletableEventContainer(DefaultCompletableGameActionEvents<T> wrapped) {
            this.wrapped = wrapped;
        }

        @Override
        public UndoObjectAction<CompletableGameActionEventsRegistry>
            register(int priority, CompletableGameObjectAction<? super T> action) {
            return wrapped.register(priority, action);
        }

        @Override
        public UndoableResult<UndoableAction> triggerEvent(boolean delayable, T object) {
            GameActionList<Game> pauseCollector = pauseCollectorRef.get();
            if (pauseCollector != null && delayable) {
                // If the returned finalizer has been called, then
                // we have to call the finalizer immediately after triggering
                // the event. Otherwise, we set a reference after triggering the
                // event and then it is the client's responsiblity to notify
                // the finalizer.

                AtomicReference<UndoableAction> finalizerRef = new AtomicReference<>(null);

                GameAction delayedAction = (Game actionGame) -> {
                    UndoableResult<UndoableAction> triggerResult = wrapped.triggerEvent(object);

                    UndoAction finalizeUndo;
                    UndoableAction finalizer = triggerResult.getResult();
                    if (!finalizerRef.compareAndSet(null, finalizer)) {
                        finalizeUndo = finalizer.doAction();
                    } else {
                        finalizeUndo = UndoAction.DO_NOTHING;
                    }

                    return () -> {
                        finalizeUndo.undo();
                        triggerResult.undo();
                    };
                };

                UndoableAction finalizer = () -> {
                    UndoableAction currentFinalizer = finalizerRef.getAndSet(() -> UndoAction.DO_NOTHING);
                    return currentFinalizer != null
                        ? currentFinalizer.doAction()
                        : UndoAction.DO_NOTHING;
                };

                RegisterId registerId = pauseCollector.addAction(delayedAction);
                return new UndoableResult<>(finalizer, () -> pauseCollector.unregister(registerId));
            } else {
                return wrapped.triggerEvent(object);
            }
        }
    }*/
}
