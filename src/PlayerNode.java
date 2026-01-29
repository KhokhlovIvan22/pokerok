import javafx.animation.Animation;
import javafx.animation.FadeTransition;
import javafx.animation.FillTransition;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.effect.DropShadow;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.util.Duration;
import java.util.List;

public class PlayerNode extends VBox {
    private final Circle avatarCircle;
    private final Label nameLabel;
    private final Label chipsLabel;
    private final Label betLabel;
    private final Label dealerBadge;
    private final HBox cardsContainer;
    private final FillTransition allInAnimation;
    private final Label allInTextLabel;

    public PlayerNode() {
        this.setAlignment(Pos.CENTER);
        this.setSpacing(5);

        StackPane avatarContainer = new StackPane();

        avatarCircle = new Circle(30);
        avatarCircle.setFill(Color.BLACK);
        avatarCircle.setStroke(Color.WHITE);
        avatarCircle.setStrokeWidth(2);
        avatarCircle.setEffect(new DropShadow(10, Color.BLACK));

        dealerBadge = new Label("D");
        dealerBadge.setTextFill(Color.WHITE);
        dealerBadge.setFont(Font.font("Arial", FontWeight.BOLD, 12));
        dealerBadge.setStyle("-fx-background-color: red; -fx-background-radius: 10; -fx-padding: 2 5;");
        dealerBadge.setTranslateX(25);
        dealerBadge.setTranslateY(-20);
        dealerBadge.setVisible(false);

        allInTextLabel = new Label("ALL IN!!!");
        allInTextLabel.setTextFill(Color.RED);
        allInTextLabel.setFont(Font.font("Arial", FontWeight.BLACK, 16));
        allInTextLabel.setEffect(new DropShadow(5, Color.BLACK));
        allInTextLabel.setVisible(false);

        avatarContainer.getChildren().addAll(avatarCircle, dealerBadge, allInTextLabel);

        nameLabel = createLabel(14, true);
        chipsLabel = createLabel(12, false);
        chipsLabel.setTextFill(Color.LIGHTGREEN);
        betLabel = createLabel(12, false);
        betLabel.setTextFill(Color.YELLOW);

        cardsContainer = new HBox(5);
        cardsContainer.setAlignment(Pos.CENTER);
        cardsContainer.setMinHeight(40);
        cardsContainer.setTranslateY(-5);

        this.getChildren().addAll(avatarContainer, nameLabel, chipsLabel, betLabel, cardsContainer);

        allInAnimation = new FillTransition(Duration.seconds(0.4), avatarCircle);
        allInAnimation.setFromValue(Color.BLACK);
        allInAnimation.setToValue(Color.RED);
        allInAnimation.setCycleCount(Animation.INDEFINITE);
        allInAnimation.setAutoReverse(true);
    }

    private Label createLabel(int size, boolean bold) {
        Label l = new Label();
        l.setTextFill(Color.WHITE);
        l.setFont(Font.font("Segoe UI", bold ? FontWeight.BOLD : FontWeight.NORMAL, size));
        return l;
    }

    private void updateCards(GameStateDTO.PlayerDTO p, boolean isHandInProgress, boolean isShowdown, boolean isWinner) {
        int currentCardCount = cardsContainer.getChildren().size();
        cardsContainer.getChildren().clear();

        boolean shouldShow = (isHandInProgress || isShowdown) && !p.isWaitingForNextHand && !p.isFolded;

        if (shouldShow) {
            if (p.holeCards != null && !p.holeCards.isEmpty()) {
                for (Card card : p.holeCards) {
                    ImageView cardView = new ImageView(ImageUtils.getCardImage(
                            card.getPower().getRank(),
                            card.getSuit().ordinal()
                    ));
                    cardView.setFitWidth(56);
                    cardView.setPreserveRatio(true);

                    if (currentCardCount == 0) {
                        FadeTransition ft = new FadeTransition(Duration.millis(500), cardView);
                        ft.setFromValue(0);
                        ft.setToValue(1);
                        ft.play();
                    }

                    if (isShowdown && p.handResult != null && p.handResult.getBestFive() != null && isWinner) {
                        if (isCardInList(card, p.handResult.getBestFive())) {
                            DropShadow goldGlow = new DropShadow();
                            goldGlow.setColor(Color.GOLD);
                            goldGlow.setRadius(20);
                            goldGlow.setSpread(0.6);
                            cardView.setEffect(goldGlow);
                        }
                    }
                    cardsContainer.getChildren().add(cardView);
                }
            } else if (isHandInProgress) {
                for (int i = 0; i < 2; ++i) {
                    ImageView back = new ImageView(ImageUtils.getCardBack());
                    back.setFitWidth(42);
                    back.setPreserveRatio(true);
                    cardsContainer.getChildren().add(back);
                }
            }
        }
    }

    private boolean isCardInList(Card target, List<Card> list) {
        if (list == null) return false;
        return list.stream().anyMatch(c ->
                c.getPower() == target.getPower() && c.getSuit() == target.getSuit());
    }

    public void update(GameStateDTO.PlayerDTO p, boolean isActive, boolean isDealer, boolean isHandInProgress, boolean isShowdown, boolean isWinner) {
        nameLabel.setText(p.name);
        chipsLabel.setText("$" + p.chips);

        if (isWinner) {
            nameLabel.setTextFill(Color.GOLD);
            nameLabel.setScaleX(1.2);
            nameLabel.setScaleY(1.2);
        } else {
            nameLabel.setTextFill(Color.WHITE);
            nameLabel.setScaleX(1.0);
            nameLabel.setScaleY(1.0);
        }

        if (isHandInProgress && p.currentBet > 0) {
            betLabel.setText("Bet: $" + p.currentBet);
            betLabel.setVisible(true);
        } else {
            betLabel.setVisible(false);
        }

        dealerBadge.setVisible(isDealer);
        allInTextLabel.setVisible(isHandInProgress && p.isAllIn);

        if (isHandInProgress && isActive && !p.isWaitingForNextHand) {
            avatarCircle.setStroke(Color.GOLD);
            avatarCircle.setStrokeWidth(4);
        } else {
            avatarCircle.setStroke(Color.WHITE);
            avatarCircle.setStrokeWidth(2);
        }

        boolean isBrokeAndNotPlaying = (p.chips <= 0 && !p.isAllIn);

        if (p.isFolded || p.isWaitingForNextHand || isBrokeAndNotPlaying) {
            this.setOpacity(0.4);
            allInAnimation.stop();
            avatarCircle.setFill(Color.GRAY);
            avatarCircle.setStroke(Color.DARKGRAY);
        } else if (isHandInProgress && p.isAllIn) {
            this.setOpacity(1.0);
            if (allInAnimation.getStatus() != Animation.Status.RUNNING) {
                allInAnimation.play();
            }
        } else {
            this.setOpacity(1.0);
            allInAnimation.stop();
            avatarCircle.setFill(Color.BLACK);
        }

        updateCards(p, isHandInProgress, isShowdown, isWinner);
    }
}