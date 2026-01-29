import java.io.Serializable;
import java.util.List;
import java.util.ArrayList;

public class HandResult implements Comparable<HandResult>, Serializable {
    private final HandRank rank;
    private final long score;
    private final String description;
    private final List<Card> bestFive;

    public HandResult(HandRank rank, long score, String description, List<Card> bestFive) {
        this.rank = rank;
        this.score = score;
        this.description = description;
        this.bestFive = new ArrayList<>(bestFive);
    }

    public HandRank getRank() {
        return rank;
    }

    public long getScore() {
        return score;
    }

    public String getDescription() {
        return description;
    }

    public List<Card> getBestFive() {
        return bestFive;
    }

    @Override
    public int compareTo (HandResult other)
    {
        return Long.compare(this.score,other.score);
    }
}