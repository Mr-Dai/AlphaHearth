package info.hearthsim.brazier.ui;

import info.hearthsim.brazier.*;
import info.hearthsim.brazier.cards.CardDescr;
import java.awt.GridLayout;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.WindowConstants;

import org.jtrim.event.ListenerRef;
import org.jtrim.event.ListenerRegistries;
import org.jtrim.utils.ExceptionHelper;

import static org.jtrim.property.swing.AutoDisplayState.*;

@SuppressWarnings("serial")
public class GamePlayPanel extends javax.swing.JPanel {
    private final HearthStoneDb db;
    private final TargetManager targetManager;
    private final GamePlayUiAgent uiAgent;

    private final BoardSidePanel board1;
    private final BoardSidePanel board2;

    private final List<ListenerRef> trackRefs;
    private final PlayerPanel player1;
    private final PlayerPanel player2;

    public GamePlayPanel(Game game, PlayerId startingPlayer) {
        ExceptionHelper.checkNotNullArgument(game, "game");
        ExceptionHelper.checkNotNullArgument(startingPlayer, "startingPlayer");

        this.db = game.getDb();
        this.trackRefs = new LinkedList<>();
        this.targetManager = new TargetManager(this);

        uiAgent = new GamePlayUiAgent(game, startingPlayer, targetManager);
        uiAgent.addRefreshGameAction(this::refreshGame);

        initComponents();

        jLeftControlContainer.setLayout(new SerialLayoutManager(5, false, SerialLayoutManager.Alignment.LEFT));
        jRightControlContainer.setLayout(new SerialLayoutManager(5, false, SerialLayoutManager.Alignment.RIGHT));

        board1 = new BoardSidePanel(game.getPlayer1().getPlayerId(), targetManager);
        board2 = new BoardSidePanel(game.getPlayer2().getPlayerId(), targetManager);

        player1 = new PlayerPanel(db);
        player2 = new PlayerPanel(db);

        jPlayer1BoardPanel.add(board1);
        jPlayer2BoardPanel.add(board2);
        jPlayer1PlayerPanel.add(player1);
        jPlayer2PlayerPanel.add(player2);

        setupEnableDisable();

        refreshGame();

        setUserAgent(game);
    }

    private void setUserAgent(Game game) {
        game.setUserAgent((boolean allowCancel, List<? extends CardDescr> cards) -> {
            // Doesn't actually matter which player we use.
            Player player = game.getCurrentPlayer();
            return UiUtils.getOnEdt(() -> ChooseCardPanel.selectCard(this, allowCancel, player, cards));
        });
    }

    private void setupEnableDisable() {
        addSwingStateListener(uiAgent.hasUndos(), jUndoButton::setEnabled);
    }

    public final void setGame(Game game, PlayerId startingPlayer) {
        setUserAgent(game);
        uiAgent.resetGame(game, startingPlayer);
    }

    private void refreshGame() {
        targetManager.clearRequest();

        Game game = uiAgent.getGame();
        PlayerId currentPlayerId = uiAgent.getCurrentPlayerId();

        PlayerUiAgent player1UiAgent;
        PlayerUiAgent player2UiAgent;
        if (Objects.equals(game.getPlayer1().getPlayerId(), currentPlayerId)) {
            player1UiAgent = new PlayerUiAgent(uiAgent, currentPlayerId);
            player2UiAgent = null;
        }
        else {
            player1UiAgent = null;
            player2UiAgent = new PlayerUiAgent(uiAgent, currentPlayerId);
        }

        player1.setState(player1UiAgent, game.getPlayer1());
        player2.setState(player2UiAgent, game.getPlayer2());

        board1.setBoard(player1UiAgent, game.getPlayer1().getBoard());
        board2.setBoard(player2UiAgent, game.getPlayer2().getBoard());

        setupHeroTracking(game);
    }

    private ListenerRef trackForTarget(
            TargetManager targetManager,
            JComponent component,
            info.hearthsim.brazier.Character target,
            Consumer<Boolean> highlightSetter) {
        ListenerRef ref1 = PlayerTargetNeed.trackForTarget(targetManager, component, target, highlightSetter);
        ListenerRef ref2 = AttackTargetNeed.trackForTarget(targetManager, component, target, highlightSetter);
        return ListenerRegistries.combineListenerRefs(ref1, ref2);
    }

    private void setupHeroTracking(Game game) {
        trackRefs.forEach(ListenerRef::unregister);
        trackRefs.clear();

        trackRefs.add(trackForTarget(
                uiAgent.getTargetManager(),
                player1,
                game.getPlayer1().getHero(),
                player1::setHighlight));
        trackRefs.add(trackForTarget(
                uiAgent.getTargetManager(),
                player2,
                game.getPlayer2().getHero(),
                player2::setHighlight));
    }

    /**
     * This method is called from within the constructor to initialize the form. WARNING: Do NOT modify this code. The
     * content of this method is always regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jGameContainer = new javax.swing.JPanel();
        jPlayer1Panel = new javax.swing.JPanel();
        jPlayer1PlayerPanel = new javax.swing.JPanel();
        jPlayer1BoardPanel = new javax.swing.JPanel();
        jPlayer2Panel = new javax.swing.JPanel();
        jPlayer2BoardPanel = new javax.swing.JPanel();
        jPlayer2PlayerPanel = new javax.swing.JPanel();
        jPanel1 = new javax.swing.JPanel();
        jRightControlContainer = new javax.swing.JPanel();
        jEndTurnButton = new javax.swing.JButton();
        jLeftControlContainer = new javax.swing.JPanel();
        jUndoButton = new javax.swing.JButton();
        jAddCardButton = new javax.swing.JButton();
        jResetGameButton = new javax.swing.JButton();

        jPlayer1PlayerPanel.setLayout(new java.awt.GridLayout(1, 1));

        jPlayer1BoardPanel.setLayout(new java.awt.GridLayout(1, 1));

        javax.swing.GroupLayout jPlayer1PanelLayout = new javax.swing.GroupLayout(jPlayer1Panel);
        jPlayer1Panel.setLayout(jPlayer1PanelLayout);
        jPlayer1PanelLayout.setHorizontalGroup(
            jPlayer1PanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPlayer1PanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPlayer1PanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jPlayer1BoardPanel, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jPlayer1PlayerPanel, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addContainerGap())
        );
        jPlayer1PanelLayout.setVerticalGroup(
            jPlayer1PanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPlayer1PanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jPlayer1PlayerPanel, javax.swing.GroupLayout.PREFERRED_SIZE, 185, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jPlayer1BoardPanel, javax.swing.GroupLayout.DEFAULT_SIZE, 187, Short.MAX_VALUE)
                .addContainerGap())
        );

        jPlayer2BoardPanel.setLayout(new java.awt.GridLayout(1, 1));

        jPlayer2PlayerPanel.setLayout(new java.awt.GridLayout(1, 1));

        javax.swing.GroupLayout jPlayer2PanelLayout = new javax.swing.GroupLayout(jPlayer2Panel);
        jPlayer2Panel.setLayout(jPlayer2PanelLayout);
        jPlayer2PanelLayout.setHorizontalGroup(
            jPlayer2PanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPlayer2PanelLayout.createSequentialGroup()
                .addGroup(jPlayer2PanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jPlayer2PlayerPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addGroup(jPlayer2PanelLayout.createSequentialGroup()
                        .addContainerGap()
                        .addComponent(jPlayer2BoardPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)))
                .addContainerGap())
        );
        jPlayer2PanelLayout.setVerticalGroup(
            jPlayer2PanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPlayer2PanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jPlayer2BoardPanel, javax.swing.GroupLayout.DEFAULT_SIZE, 187, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jPlayer2PlayerPanel, javax.swing.GroupLayout.PREFERRED_SIZE, 185, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap())
        );

        jRightControlContainer.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.RIGHT, 0, 0));

        jEndTurnButton.setText("End Turn");
        jEndTurnButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jEndTurnButtonActionPerformed(evt);
            }
        });
        jRightControlContainer.add(jEndTurnButton);

        jLeftControlContainer.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT, 0, 0));

        jUndoButton.setText("Undo");
        jUndoButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jUndoButtonActionPerformed(evt);
            }
        });
        jLeftControlContainer.add(jUndoButton);

        jAddCardButton.setText("Card database");
        jAddCardButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jAddCardButtonActionPerformed(evt);
            }
        });
        jLeftControlContainer.add(jAddCardButton);

        jResetGameButton.setText("Reset game");
        jResetGameButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jResetGameButtonActionPerformed(evt);
            }
        });
        jLeftControlContainer.add(jResetGameButton);

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jLeftControlContainer, javax.swing.GroupLayout.DEFAULT_SIZE, 791, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(jRightControlContainer, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap())
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addGap(0, 0, 0)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jLeftControlContainer, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jRightControlContainer, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addGap(0, 0, 0))
        );

        javax.swing.GroupLayout jGameContainerLayout = new javax.swing.GroupLayout(jGameContainer);
        jGameContainer.setLayout(jGameContainerLayout);
        jGameContainerLayout.setHorizontalGroup(
            jGameContainerLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jPlayer1Panel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
            .addComponent(jPlayer2Panel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
            .addComponent(jPanel1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );
        jGameContainerLayout.setVerticalGroup(
            jGameContainerLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jGameContainerLayout.createSequentialGroup()
                .addComponent(jPlayer1Panel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(0, 0, 0)
                .addComponent(jPanel1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addGap(0, 0, 0)
                .addComponent(jPlayer2Panel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap())
        );

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jGameContainer, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jGameContainer, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
    }// </editor-fold>//GEN-END:initComponents

    private void jEndTurnButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jEndTurnButtonActionPerformed
        uiAgent.endTurn();
    }//GEN-LAST:event_jEndTurnButtonActionPerformed

    private void jUndoButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jUndoButtonActionPerformed
        uiAgent.undoLastAction();
    }//GEN-LAST:event_jUndoButtonActionPerformed

    private void jResetGameButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jResetGameButtonActionPerformed
        Game prevGame = uiAgent.getGame();
        PlayerId player1Id = prevGame.getPlayer1().getPlayerId();
        PlayerId player2Id = prevGame.getPlayer2().getPlayerId();

        Game newGame = new Game(prevGame.getDb(), player1Id, player2Id);
        setGame(newGame, newGame.getPlayer1().getPlayerId());
    }//GEN-LAST:event_jResetGameButtonActionPerformed

    private void jAddCardButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jAddCardButtonActionPerformed
        CardDatabasePanel panel = new CardDatabasePanel(db, uiAgent);

        JFrame dbFrame = new JFrame("Card database");
        dbFrame.getContentPane().setLayout(new GridLayout(1, 1));
        dbFrame.getContentPane().add(panel);

        dbFrame.pack();
        dbFrame.setLocationRelativeTo(this);
        dbFrame.setVisible(true);
        dbFrame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
    }//GEN-LAST:event_jAddCardButtonActionPerformed

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton jAddCardButton;
    private javax.swing.JButton jEndTurnButton;
    private javax.swing.JPanel jLeftControlContainer;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPlayer1BoardPanel;
    private javax.swing.JPanel jPlayer1Panel;
    private javax.swing.JPanel jPlayer1PlayerPanel;
    private javax.swing.JPanel jPlayer2BoardPanel;
    private javax.swing.JPanel jPlayer2Panel;
    private javax.swing.JPanel jPlayer2PlayerPanel;
    private javax.swing.JButton jResetGameButton;
    private javax.swing.JPanel jRightControlContainer;
    private javax.swing.JButton jUndoButton;
    private javax.swing.JPanel jGameContainer;
    // End of variables declaration//GEN-END:variables
}