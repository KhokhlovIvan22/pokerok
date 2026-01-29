import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Deck {
    private final List<Card> cardList = new ArrayList<>();

    public Deck () {
        for (Suit suit: Suit.values()) {
            for (Rank power : Rank.values()) {
                Card card = new Card(suit,power);
                cardList.add(card);
            }
        }
        Collections.shuffle(cardList);
    }

    public Card dealCard () {
        if (cardList.isEmpty())
            return null;
        return cardList.removeFirst();
    }
}