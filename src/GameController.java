import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.effect.DropShadow;
import javafx.scene.effect.Glow;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Ellipse;
import javafx.stage.Stage;
import java.util.List;

public class GameController {
    private final StackPane root = new StackPane();
    private final Pane tableLayer = new Pane();
    private final Pane playersLayer = new Pane();
    private final Pane uiLayer = new Pane();

    private final NetworkClient networkClient;
    private final String playerName;

    private HBox communityCardsBox;
    private Label potLabel;
    private Label combinationHintLabel;

    private VBox controlsContainer;
    private Slider raiseSlider;
    private Label raiseValueLabel;
    private Button btnFold, btnCall, btnRaise;

    private final double CENTER_X = 640;
    private final double CENTER_Y = 320;
    private final double RADIUS_X = 450;
    private final double RADIUS_Y = 210;

    private VBox winnerContainer;
    private Label winnerNameLabel;
    private Label winnerComboLabel;

    private PauseTransition cleanupTimer;

    public GameController(Stage stage, String playerName, String host, int port) {
        this.playerName = playerName;
        this.networkClient = new NetworkClient(this::updateUI);

        setupLayers();
        setupControls();

        if (!networkClient.connect(host, port, playerName)) {
            stage.close();
        }
        stage.setOnCloseRequest(e -> networkClient.close());
    }

    private void setupLayers() {
        root.setStyle("-fx-background-color: #2b2b2b;");
        root.setPrefSize(1280, 720);

        Ellipse table = new Ellipse(CENTER_X, CENTER_Y, RADIUS_X, RADIUS_Y);
        table.setFill(Color.web("#074b1a"));
        table.setStroke(Color.web("#1a1a1a"));
        table.setStrokeWidth(10);
        tableLayer.getChildren().add(table);

        communityCardsBox = new HBox(10);
        communityCardsBox.setAlignment(Pos.CENTER);

        ImageView deckView = new ImageView(ImageUtils.getCardBack());
        deckView.setFitWidth(84);
        deckView.setPreserveRatio(true);
        deckView.setTranslateY(-20);

        HBox centerArea = new HBox(30, communityCardsBox, deckView);
        centerArea.setAlignment(Pos.CENTER);

        StackPane centerWrapper = new StackPane(centerArea);
        centerWrapper.setPrefSize(1280, 720);
        centerWrapper.setPickOnBounds(false);
        tableLayer.getChildren().add(centerWrapper);

        potLabel = new Label();
        potLabel.setStyle("-fx-text-fill: #FFD700; -fx-font-size: 26; -fx-font-weight: bold;");
        potLabel.setLayoutX(1150);
        potLabel.setLayoutY(20);
        uiLayer.getChildren().add(potLabel);

        combinationHintLabel = new Label("");
        combinationHintLabel.setStyle("-fx-text-fill: white; -fx-font-size: 20; -fx-font-weight: bold; -fx-font-style: italic;");
        VBox comboBox = new VBox(combinationHintLabel);
        comboBox.setAlignment(Pos.CENTER);
        comboBox.setPrefWidth(1280);
        comboBox.setLayoutY(440);
        uiLayer.getChildren().add(comboBox);

        playersLayer.setPickOnBounds(false);
        uiLayer.setPickOnBounds(false);
        root.getChildren().addAll(tableLayer, playersLayer, uiLayer);

        winnerNameLabel = new Label("");
        winnerNameLabel.setStyle("-fx-text-fill: #FFD700; -fx-font-size: 38; -fx-font-weight: bold;");

        winnerComboLabel = new Label("");
        winnerComboLabel.setStyle("-fx-text-fill: #FFD700; -fx-font-size: 38; -fx-font-style: italic;");

        winnerContainer = new VBox(5, winnerNameLabel, winnerComboLabel);
        winnerContainer.setAlignment(Pos.CENTER);
        winnerContainer.setPrefWidth(1280);
        winnerContainer.setLayoutY(160);
        winnerContainer.setVisible(false);

        uiLayer.getChildren().add(winnerContainer);
    }

    private void setupControls() {
        btnFold = createButtonStyle("FOLD", "#c0392b");
        btnCall = createButtonStyle("CALL", "#c0392b");
        btnRaise = createButtonStyle("RAISE", "#c0392b");

        raiseSlider = new Slider();
        raiseSlider.setPrefWidth(300);

        raiseValueLabel = new Label("$0");
        raiseValueLabel.setTextFill(Color.WHITE);
        raiseValueLabel.setStyle("-fx-font-size: 16; -fx-font-weight: bold;");

        raiseSlider.valueProperty().addListener((obs, oldVal, newVal) ->
                raiseValueLabel.setText("$" + newVal.intValue()));

        // Выравнивание элементов внутри горизонтальных боксов по левому краю
        HBox buttonsBox = new HBox(20, btnFold, btnCall, btnRaise);
        buttonsBox.setAlignment(Pos.CENTER_LEFT);

        HBox sliderBox = new HBox(15, raiseSlider, raiseValueLabel);
        sliderBox.setAlignment(Pos.CENTER_LEFT);

        // Основной контейнер теперь не имеет жесткой ширины 1280
        controlsContainer = new VBox(15, sliderBox, buttonsBox);
        controlsContainer.setAlignment(Pos.CENTER_LEFT);

        controlsContainer.setLayoutX(100);
        controlsContainer.setLayoutY(528);
        controlsContainer.setVisible(false);

        uiLayer.getChildren().add(controlsContainer);

        btnFold.setOnAction(e -> networkClient.sendAction(PlayerAction.FOLD, 0));
        btnCall.setOnAction(e -> networkClient.sendAction(PlayerAction.CALL, 0));
        btnRaise.setOnAction(e -> networkClient.sendAction(PlayerAction.RAISE, (int)raiseSlider.getValue()));
    }

    private Button createButtonStyle(String text, String hexColor) {
        Button b = new Button(text);
        b.setStyle("-fx-background-color: " + hexColor + "; -fx-text-fill: white; " +
                "-fx-font-size: 16; -fx-font-weight: bold; -fx-background-radius: 20; -fx-padding: 10 30;");
        b.setMinWidth(120);
        return b;
    }

    private void hideResults() {
        winnerContainer.setVisible(false);
        combinationHintLabel.setVisible(false);
        winnerNameLabel.setText("");
        winnerComboLabel.setText("");
    }

    private void startCleanupTimer() {
        if (cleanupTimer != null)
            cleanupTimer.stop();
        cleanupTimer = new PauseTransition(javafx.util.Duration.seconds(60));
        cleanupTimer.setOnFinished(e -> hideResults());
        cleanupTimer.play();
    }

    private void updateUI(GameStateDTO state) {
        Platform.runLater(() -> {
            potLabel.setText("POT: $" + state.pot);

            List<Card> winningBestFive = null;
            if (!state.isHandInProgress && state.winners != null && !state.winners.isEmpty()) {
                winningBestFive = state.players.stream()
                        .filter(p -> state.winners.contains(p.name) && p.handResult != null)
                        .map(p -> p.handResult.getBestFive())
                        .findFirst().orElse(null);
            }

            communityCardsBox.getChildren().clear();
            for (Card card : state.communityCards) {
                ImageView iv = new ImageView(ImageUtils.getCardImage(
                        card.getPower().getRank(),
                        card.getSuit().ordinal()
                ));
                iv.setFitWidth(84);
                iv.setPreserveRatio(true);
                iv.setSmooth(true);

                if (winningBestFive != null && isCardInList(card, winningBestFive)) {
                    DropShadow goldGlow = new DropShadow();
                    goldGlow.setColor(Color.GOLD);
                    goldGlow.setRadius(25);
                    goldGlow.setSpread(0.5);
                    iv.setEffect(goldGlow);
                }

                communityCardsBox.getChildren().add(iv);
            }

            int myIndex = -1;
            for (int i = 0; i < state.players.size(); i++) {
                if (state.players.get(i).name.equalsIgnoreCase(this.playerName)) {
                    myIndex = i;
                    break;
                }
            }

            updatePlayers(state);
            updateActionControls(state);

            if (myIndex != -1) {
                GameStateDTO.PlayerDTO me = state.players.get(myIndex);
                if (me.handResult != null && me.handResult.getDescription() != null) {
                    String hint = me.handResult.getDescription();
                    combinationHintLabel.setText("Ваша комбинация: " + hint.toUpperCase());
                    combinationHintLabel.setVisible(true);
                    combinationHintLabel.toFront();
                } else {
                    combinationHintLabel.setVisible(false);
                }
            } else {
                combinationHintLabel.setVisible(false);
            }

            if (!state.isHandInProgress && state.winners != null && !state.winners.isEmpty()) {
                String names = String.join(" & ", state.winners).toUpperCase();
                if (state.winners.size() > 1) {
                    winnerNameLabel.setText("Победители: " + names);
                } else {
                    winnerNameLabel.setText("Победитель: " + names);
                }

                state.players.stream()
                        .filter(p -> state.winners.contains(p.name) && p.handResult != null)
                        .findFirst()
                        .ifPresent(w -> {
                            winnerComboLabel.setText("Победная комбинация: " + w.handResult.getDescription().toUpperCase());
                        });

                winnerContainer.setVisible(true);
                winnerContainer.toFront();

                startCleanupTimer();

            } else if (state.isHandInProgress) {
                if (cleanupTimer != null) {
                    cleanupTimer.stop();
                }
                hideResults();
            }
        });
    }

    private boolean isCardInList(Card target, List<Card> list) {
        if (list == null) return false;
        return list.stream().anyMatch(c ->
                c.getPower() == target.getPower() && c.getSuit() == target.getSuit());
    }

    private void updatePlayers(GameStateDTO state) {
        playersLayer.getChildren().clear();
        int total = state.players.size();
        if (total == 0)
            return;
        int myIndex = -1;
        for (int i = 0; i < total; i++) {
            if (state.players.get(i).name.trim().equalsIgnoreCase(playerName.trim())) {
                myIndex = i;
                break;
            }
        }
        int referenceIndex = (myIndex != -1) ? myIndex : 0;
        double step = (total > 1) ? (2 * Math.PI / total) : 0;
        for (int i = 0; i < total; ++i) {
            GameStateDTO.PlayerDTO p = state.players.get(i);
            PlayerNode node = new PlayerNode();
            boolean isActive = (state.currentPlayerIndex == i && state.isHandInProgress);
            boolean isDealer = (state.dealerIndex == i);
            boolean isWinner = !state.isHandInProgress && state.winners.contains(p.name);
            node.update(p, isActive, isDealer, state.isHandInProgress, state.isShowdown, isWinner);
            playersLayer.getChildren().add(node);
            if (i == myIndex) {
                node.setLayoutX(CENTER_X - 60);
                node.setLayoutY(500);
            } else {
                if (total == 2) {
                    node.setLayoutX(CENTER_X - 60);
                    node.setLayoutY(10);
                } else {
                    double angle = Math.PI / 2 + (i - referenceIndex) * step;
                    double x = CENTER_X + (RADIUS_X + 40) * Math.cos(angle);
                    double y = CENTER_Y + (RADIUS_Y + 40) * Math.sin(angle);
                    node.setLayoutX(x - 60);
                    node.setLayoutY(y - 100);
                }
            }
        }
    }

    private void updateActionControls(GameStateDTO state) {
        boolean isMyTurn = state.isHandInProgress &&
                state.currentPlayerIndex != -1 &&
                state.players.get(state.currentPlayerIndex).name.equals(playerName);
        controlsContainer.setVisible(isMyTurn);

        if (isMyTurn) {
            GameStateDTO.PlayerDTO me = state.players.get(state.currentPlayerIndex);
            if (me.currentBet == state.currentMaxBet) {
                btnCall.setText("CHECK");
            } else {
                btnCall.setText("CALL (" + (state.currentMaxBet - me.currentBet) + ")");
            }

            int minRaise = state.currentMaxBet + 10;
            int maxPossible = me.chips + me.currentBet;

            if (maxPossible > minRaise) {
                raiseSlider.setDisable(false);
                btnRaise.setDisable(false);
                raiseSlider.setMin(minRaise);
                raiseSlider.setMax(maxPossible);
            } else {
                raiseSlider.setDisable(true);
                btnRaise.setDisable(true);
            }
        }
    }

    public StackPane getView() { return root; }
}