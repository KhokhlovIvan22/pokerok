import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class ServerPlayer implements Serializable {
    private final String sessionID;
    private final String name;
    private int chips;
    private boolean online = true;
    private boolean waitingForNextHand;

    private boolean allIn;
    private boolean folded;
    private int currentBet = 0;
    private int lastBetAmount = 0;
    private HandResult currentResult;
    private final List<Card> cards = new ArrayList<>();

    public ServerPlayer(String id, String name, int startChips) {
        this.sessionID = id;
        this.name = name;
        this.chips = startChips;
    }

    public void reset() {
        currentBet = 0;
        lastBetAmount = 0;
        currentResult = null;
        cards.clear();
        allIn = false;
        folded = false;
    }

    public void addCard(Card card) {
        cards.add(card);
    }

    public void makeBet(int amount) {
        if (amount >= chips) {
            lastBetAmount = chips;
            currentBet += lastBetAmount;
            chips = 0;
            allIn = true;
        } else
        {
            lastBetAmount = amount;
            currentBet += amount;
            chips -= amount;
        }
    }

    public void clearCurrentBet() {
        this.currentBet = 0;
        this.lastBetAmount = 0;
    }

    public String getSessionID() {
        return sessionID;
    }

    public String getName() {
        return name;
    }

    public int getChips() {
        return chips;
    }

    public void addChips(int amount) {
        chips += amount;
    }

    public int getCurrentBet() {
        return currentBet;
    }

    public int getLastBetAmount() {
        return lastBetAmount;
    }

    public List<Card> getCards() {
        return cards;
    }

    public HandResult getCurrentResult() {
        return currentResult;
    }

    public void setCurrentResult(HandResult result) {
        currentResult = result;
    }

    public boolean isAllIn() {
        return allIn;
    }

    public boolean isFolded() {
        return folded;
    }

    public void setFolded(boolean folded) {
        this.folded = folded;
    }

    public boolean isOnline() {
        return online;
    }

    public void setOnline (boolean online) {
        this.online = online;
    }

    public boolean isWaitingForNextHand() {
        return waitingForNextHand;
    }

    public void setWaitingForNextHand(boolean waiting) {
        this.waitingForNextHand = waiting;
    }
}