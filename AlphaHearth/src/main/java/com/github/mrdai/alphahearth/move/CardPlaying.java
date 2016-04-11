package com.github.mrdai.alphahearth.move;

import com.github.mrdai.alphahearth.Board;
import info.hearthsim.brazier.game.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CardPlaying implements SingleMove {
    private final PlayerId playerId;
    private final int cardIndex;
    private final int minionLocation;
    private final EntityId target;

    public CardPlaying(PlayerId playerId, int cardIndex) {
        this(playerId, cardIndex, -1, null);
    }

    public CardPlaying(PlayerId playerId, int cardIndex, EntityId target) {
        this(playerId, cardIndex, -1, target);
    }

    public CardPlaying(PlayerId playerId, int cardIndex, int minionLocation) {
        this(playerId, cardIndex, minionLocation, null);
    }

    public CardPlaying(PlayerId playerId, int cardIndex, int minionLocation, EntityId target) {
        this.playerId = playerId;
        this.cardIndex = cardIndex;
        this.minionLocation = minionLocation;
        this.target = target;
    }

    /**
     * Returns the index of the card to be played.
     */
    public int getCardIndex() {
        return cardIndex;
    }

    /**
     * Returns the target; returns {@code null} if there is no target.
     */
    public EntityId getTarget() {
        return target;
    }

    /**
     * Returns if there is any target for this move.
     */
    public boolean hasTarget() {
        return target == null;
    }

    public int getMinionLocation() {
        return minionLocation;
    }

    public PlayerId getPlayerId() {
        return playerId;
    }

    public String toString(Board board) {
        Game game = board.getGame();
        StringBuilder builder = new StringBuilder();
        if (minionLocation == -1) {
            builder.append(game.getPlayer(playerId).getPlayerId()).append(" plays ")
                   .append(game.getPlayer(playerId).getHand().getCard(cardIndex));
            if (target != null) {
                builder.append(" with target ");
                Entity eTarget = game.findEntity(target);
                if (eTarget instanceof Hero)
                    builder.append(eTarget.getOwner().getPlayerId().getName());
                else
                    builder.append(eTarget);
            }
        } else {
            builder.append(game.getPlayer(playerId).getPlayerId()).append(" summons ")
                   .append(game.getPlayer(playerId).getHand().getCard(cardIndex))
                   .append(" on location ").append(minionLocation);
            if (target != null) {
                builder.append(" with battle cry target ");
                Entity eTarget = game.findEntity(target);
                if (eTarget instanceof Hero)
                    builder.append(eTarget.getOwner().getPlayerId().getName());
                else
                    builder.append(eTarget.toString());
            }
        }
        return builder.toString();
    }

    public String toString() {
        return String.format("CardPlaying[PlayerId: %s, cardIndex: %d, minionLocation: %d, target: %s]",
            playerId, cardIndex, minionLocation, target);
    }
}
