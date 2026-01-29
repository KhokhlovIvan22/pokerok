import java.io.Serializable;
import java.util.List;
import java.util.ArrayList;

public class GameStateDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    public List<Card> communityCards = new ArrayList<>();
    public int pot;
    public int currentMaxBet;
    public int currentPlayerIndex;
    public boolean isHandInProgress;
    public boolean isShowdown;
    public int dealerIndex;
    public List<String> winners = new ArrayList<>();
    public List<PlayerDTO> players = new ArrayList<>();

    public static class PlayerDTO implements Serializable {
        private static final long serialVersionUID = 1L;

        public String name;
        public int chips;
        public int currentBet;
        public boolean isFolded;
        public boolean isAllIn;
        public List<Card> holeCards;
        public HandResult handResult;
        public boolean isWaitingForNextHand;
    }
}