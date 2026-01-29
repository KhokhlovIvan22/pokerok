import java.io.Serializable;

public class Card implements Serializable {
    private final Suit suit;
    private final Rank power;

    public Card (Suit suit, Rank power) {
        this.suit=suit;
        this.power=power;
    }

    public Suit getSuit () {
        return suit;
    }

    public Rank getPower () {
        return power;
    }
}
