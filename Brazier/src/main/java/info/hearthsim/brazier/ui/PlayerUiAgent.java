package info.hearthsim.brazier.ui;

import info.hearthsim.brazier.*;
import info.hearthsim.brazier.actions.PlayTargetRequest;
import info.hearthsim.brazier.actions.TargetNeed;
import info.hearthsim.brazier.actions.undo.UndoAction;
import info.hearthsim.brazier.cards.Card;
import info.hearthsim.brazier.cards.CardDescr;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;

import org.jtrim.utils.ExceptionHelper;

public final class PlayerUiAgent {
    private final GamePlayUiAgent gameAgent;
    private final PlayerId playerId;

    public PlayerUiAgent(GamePlayUiAgent gameAgent, PlayerId playerId) {
        ExceptionHelper.checkNotNullArgument(gameAgent, "gameAgent");
        ExceptionHelper.checkNotNullArgument(playerId, "playerId");

        this.gameAgent = gameAgent;
        this.playerId = playerId;
    }

    public Player getPlayer() {
        return gameAgent.getGame().getPlayer(playerId);
    }

    public void alterPlayer(Function<? super Player, ? extends UndoAction> task) {
        ExceptionHelper.checkNotNullArgument(task, "task");
        gameAgent.alterGame((game) -> {
            return task.apply(game.getPlayer(playerId));
        });
    }

    public boolean canPlayCard(Card card) {
        Player player = gameAgent.getGame().getPlayer(playerId);
        if (player.getMana() < card.getActiveManaCost()) {
            return false;
        }

        return card.getCardDescr().doesSomethingWhenPlayed(player);
    }

    public void playHeroPower() {
        Player player = gameAgent.getGame().getPlayer(playerId);
        HeroPower heroPower = player.getHero().getHeroPower();

        if (!heroPower.isPlayable()) {
            throw new IllegalStateException("Cannot play the hero power.");
        }

        TargetNeed targetNeed = heroPower.getTargetNeed();
        if (targetNeed.hasTarget()) {
            TargetManager targetManager = gameAgent.getTargetManager();
            TargeterDef targeterDef = new TargeterDef(playerId, true, false);
            PlayerTargetNeed playerTargetNeed = new PlayerTargetNeed(targeterDef, targetNeed);
            targetManager.requestTarget(playerTargetNeed, (targetId) -> {
                if (targetId instanceof TargetId) {
                    targetManager.clearRequest();
                    playHeroPowerNow((TargetId)targetId);
                }
            });
        }
        else {
            playHeroPowerNow(null);
        }
    }

    private void playHeroPowerNow(TargetId targetId) {
        PlayTargetRequest target = new PlayTargetRequest(playerId, -1, targetId);
        gameAgent.playHeroPower(target);
    }

    public void playCard(int cardIndex) {
        Player player = gameAgent.getGame().getPlayer(playerId);
        Card card = player.getHand().getCard(cardIndex);
        if (!canPlayCard(card)) {
            throw new IllegalArgumentException("Cannot play the given card.");
        }

        List<CardDescr> chooseOneActions = card.getCardDescr().getChooseOneChoices();
        CardDescr chooseOneChoice;
        if (!chooseOneActions.isEmpty()) {
            chooseOneChoice = gameAgent.getGame().getUserAgent().selectCard(true, chooseOneActions);
            if (chooseOneChoice == null) {
                return;
            }
        }
        else {
            chooseOneChoice = null;
        }

        boolean needsMinion = card.getCardDescr().getMinion() != null;
        if (needsMinion) {
            findMinionTarget(player, cardIndex, card, chooseOneChoice);
            return;
        }

        TargetNeed targetNeed = getTargetNeed(player, card, chooseOneChoice);
        if (targetNeed.hasTarget()) {
            TargeterDef targeterDef = new TargeterDef(playerId, true, false);
            PlayerTargetNeed playerTargetNeed = new PlayerTargetNeed(targeterDef, targetNeed);
            findTarget(playerTargetNeed, cardIndex, -1, chooseOneChoice);
        }
        else {
            gameAgent.playCard(cardIndex, new PlayTargetRequest(playerId, -1, null, chooseOneChoice));
        }
    }

    private static TargetNeed getTargetNeed(Player player, Card card, CardDescr chooseOneChoice) {
        TargetNeed result = card.getCardDescr().getCombinedTargetNeed(player);
        if (chooseOneChoice != null) {
            result = result.combine(chooseOneChoice.getCombinedTargetNeed(player));
        }
        return result;
    }

    private static boolean hasValidTarget(Player player, PlayerTargetNeed targetNeed) {
        if (targetNeed.isAllowedTarget(player.getHero())) {
            return true;
        }

        return player.getBoard().findMinion((minion) -> targetNeed.isAllowedTarget(minion)) != null;
    }

    private static boolean hasValidTarget(Game game, PlayerTargetNeed targetNeed) {
        return hasValidTarget(game.getPlayer1(), targetNeed)
                || hasValidTarget(game.getPlayer2(), targetNeed);
    }

    private void findMinionTarget(Player player, int cardIndex, Card card, CardDescr chooseOneChoice) {
        if (player.getBoard().isFull()) {
            return;
        }

        TargetManager targetManager = gameAgent.getTargetManager();
        UiMinionIndexNeed minionIndexNeed = new UiMinionIndexNeed(playerId);
        targetManager.requestTarget(minionIndexNeed, (minionIndex) -> {
            if (minionIndex instanceof Integer) {
                targetManager.clearRequest();
                TargetNeed targetNeed = getTargetNeed(player, card, chooseOneChoice);
                if (targetNeed.hasTarget()) {
                    TargeterDef targeterDef = new TargeterDef(playerId, false, false);
                    PlayerTargetNeed playerTargetNeed = new PlayerTargetNeed(targeterDef, targetNeed);
                    if (hasValidTarget(player.getGame(), playerTargetNeed)) {
                        findTarget(playerTargetNeed, cardIndex, (int)minionIndex, chooseOneChoice);
                        return;
                    }
                }

                gameAgent.playCard(cardIndex, new PlayTargetRequest(playerId, (int)minionIndex, null, chooseOneChoice));
            }
        });
    }

    private void findTarget(PlayerTargetNeed targetNeed, int cardIndex, int minionIndex, CardDescr chooseOneChoice) {
        TargetManager targetManager = gameAgent.getTargetManager();
        targetManager.requestTarget(targetNeed, (targetId) -> {
            if (targetId instanceof TargetId) {
                targetManager.clearRequest();
                gameAgent.playCard(cardIndex,
                        new PlayTargetRequest(playerId, minionIndex, (TargetId)targetId, chooseOneChoice));
            }
        });
    }

    public void attack(info.hearthsim.brazier.Character attacker) {
        ExceptionHelper.checkNotNullArgument(attacker, "attacker");
        if (!Objects.equals(attacker.getOwner().getPlayerId(), playerId)) {
            throw new IllegalArgumentException("Must attack with player: " + playerId.getName());
        }

        TargetManager targetManager = gameAgent.getTargetManager();
        TargeterDef targeterDef = new TargeterDef(playerId, attacker instanceof Hero, true);
        targetManager.requestTarget(new AttackTargetNeed(targeterDef), (targetId) -> {
            if (targetId instanceof TargetId) {
                gameAgent.attack(attacker.getTargetId(), (TargetId)targetId);
            }
        });
    }

    @Override
    public String toString() {
        return "UI agent of " + playerId.getName();
    }
}
