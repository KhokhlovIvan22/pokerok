import javafx.scene.image.Image;
import java.util.HashMap;
import java.util.Map;

public class ImageUtils {
    private static final Map<String, Image> cardCache = new HashMap<>();
    private static Image cardBack;

    public static Image getCardImage(int rank, int suitOrdinal) {
        String suitName = switch (suitOrdinal) {
            case 0 -> "Hearts";
            case 1 -> "Diamonds";
            case 2 -> "Spades";
            case 3 -> "Clubs";
            default -> "";
        };

        String rankName = switch (rank) {
            case 11 -> "J";
            case 12 -> "Q";
            case 13 -> "K";
            case 14 -> "A";
            default -> String.valueOf(rank);
        };

        String fileName = "card" + suitName + rankName + ".png";

        if (!cardCache.containsKey(fileName)) {
            try {
                Image img = new Image(ImageUtils.class.getResourceAsStream("/" + fileName));
                cardCache.put(fileName, img);
            } catch (Exception e) {
                System.err.println("Failed to load card: " + fileName);
                return null;
            }
        }
        return cardCache.get(fileName);
    }

    public static Image getCardBack() {
        if (cardBack == null) {
            cardBack = new Image(ImageUtils.class.getResourceAsStream("/cardBack_red5.png"));
        }
        return cardBack;
    }
}