import java.util.ArrayList;
import java.util.List;

public class Table {
    private final List<ServerPlayer> players = new ArrayList<>();
    private Deck deck;
    private final List<Card> communityCards = new ArrayList<>();

    private int pot;
    private int currentMaxBet;
    private int dealerIndex = -1;
    private int currentPlayerIndex;
    private int aggressorIndex;
    private int actionsInRound;
    private boolean isHandInProgress;

    private boolean isShowdown = false;
    private final List<String> winnersNames = new ArrayList<>();

    private final int SMALL_BLIND = 5;
    private final int BIG_BLIND = 10;

    public synchronized void addPlayer(ServerPlayer player) {
        boolean noActivePlayers = players.stream().noneMatch(ServerPlayer::isOnline);
        if (noActivePlayers) {
            resetTableState();
        }
        if (isHandInProgress || player.getChips() <= 0) {
            player.setWaitingForNextHand(true);
        }
        players.add(player);
    }

    private void resetTableState() {
        pot = 0;
        currentMaxBet = 0;
        communityCards.clear();
        winnersNames.clear();
        isShowdown = false;
        isHandInProgress = false;
        dealerIndex = -1;
        for (ServerPlayer p : players) {
            p.setCurrentResult(null);
        }
    }

    private void moveToNextActivePlayer() {
        long canAct = players.stream()
                .filter(p -> !p.isFolded() && !p.isAllIn() && !p.isWaitingForNextHand())
                .count();

        if (canAct <= 1 && allBetsEqual()) return;

        int startIdx = currentPlayerIndex;
        do {
            currentPlayerIndex = (currentPlayerIndex + 1) % players.size();
            if (currentPlayerIndex == startIdx) break;
        } while (players.get(currentPlayerIndex).isFolded() ||
                players.get(currentPlayerIndex).isAllIn() ||
                players.get(currentPlayerIndex).isWaitingForNextHand());
    }

    private boolean allBetsEqual() {
        List<ServerPlayer> active = players.stream()
                .filter(p -> !p.isFolded() && !p.isWaitingForNextHand() && !p.isAllIn())
                .toList();
        if (active.isEmpty())
            return true;
        return active.stream().allMatch(p -> p.getCurrentBet() == currentMaxBet);
    }

    public void startNewHand() {
        for (ServerPlayer p : players) {
            if (p.getChips() <= 0) {
                p.setWaitingForNextHand(true);
                p.setFolded(true);
            } else {
                p.setWaitingForNextHand(false);
                p.reset();
            }
        }

        long activeCount = players.stream()
                .filter(p -> p.isOnline() && !p.isWaitingForNextHand())
                .count();

        if (activeCount < 2) {
            isHandInProgress = false;
            return;
        }

        deck = new Deck();
        communityCards.clear();
        winnersNames.clear();
        isShowdown = false;
        pot = 0;
        isHandInProgress = true;

        cleanupDisconnected();

        dealerIndex = (dealerIndex + 1) % players.size();
        while (players.get(dealerIndex).isWaitingForNextHand()) {
            dealerIndex = (dealerIndex + 1) % players.size();
        }

        int sbIndex = (dealerIndex + 1) % players.size();
        while (players.get(sbIndex).isWaitingForNextHand()) {
            sbIndex = (sbIndex + 1) % players.size();
        }

        int bbIndex = (sbIndex + 1) % players.size();
        while (players.get(bbIndex).isWaitingForNextHand()) {
            bbIndex = (bbIndex + 1) % players.size();
        }

        players.get(sbIndex).makeBet(SMALL_BLIND);
        players.get(bbIndex).makeBet(BIG_BLIND);

        currentMaxBet = BIG_BLIND;
        pot += players.get(sbIndex).getLastBetAmount() + players.get(bbIndex).getLastBetAmount();

        for (ServerPlayer p : players) {
            if (!p.isWaitingForNextHand() && p.isOnline()) {
                p.getCards().clear();
                p.addCard(deck.dealCard());
                p.addCard(deck.dealCard());
            }
        }

        aggressorIndex = bbIndex;
        actionsInRound = 0;
        currentPlayerIndex = (bbIndex + 1) % players.size();

        if (players.get(currentPlayerIndex).isAllIn() ||
                players.get(currentPlayerIndex).isFolded() ||
                players.get(currentPlayerIndex).isWaitingForNextHand()) {
            moveToNextActivePlayer();
        }
    }

    public void handleAction(PlayerAction action, int amount) {
        ServerPlayer p = players.get(currentPlayerIndex);
        actionsInRound++;

        switch (action) {
            case FOLD -> p.setFolded(true);
            case CALL -> {
                int callDiff = currentMaxBet - p.getCurrentBet();
                p.makeBet(callDiff);
                pot += p.getLastBetAmount();
            }
            case RAISE -> {
                int raiseDiff = amount - p.getCurrentBet();
                p.makeBet(raiseDiff);
                pot += p.getLastBetAmount();
                if (p.getCurrentBet() > currentMaxBet) {
                    currentMaxBet = p.getCurrentBet();
                    aggressorIndex = currentPlayerIndex;
                    actionsInRound = 1;
                }
            }
            case CHECK -> { }
        }
        checkRoundStatus();
    }

    private void checkRoundStatus() {
        long totalActive = players.stream()
                .filter(p -> !p.isFolded() && !p.isWaitingForNextHand())
                .count();

        if (totalActive <= 1) {
            endHandEarly();
            return;
        }

        long canActCount = players.stream()
                .filter(p -> !p.isFolded() && !p.isAllIn() && !p.isWaitingForNextHand())
                .count();

        boolean roundOver = (canActCount == 0) || (allBetsEqual() && actionsInRound >= canActCount);

        if (roundOver) {
            nextStage();
        } else {
            moveToNextActivePlayer();
        }
    }

    private void nextStage() {
        for (ServerPlayer p : players)
            p.clearCurrentBet();
        currentMaxBet = 0;
        actionsInRound = 0;

        if (communityCards.size() < 5) {
            int cardsToDeal = communityCards.isEmpty() ? 3 : 1;
            for (int i = 0; i < cardsToDeal; ++i)
                communityCards.add(deck.dealCard());

            currentPlayerIndex = dealerIndex;
            moveToNextActivePlayer();
            aggressorIndex = currentPlayerIndex;
        } else {
            showdown();
        }
    }

    private void showdown() {
        isShowdown = true;
        for (ServerPlayer p : players) {
            if (!p.isFolded() && !p.isWaitingForNextHand()) {
                List<Card> all = new ArrayList<>(p.getCards());
                all.addAll(communityCards);
                p.setCurrentResult(HandEvaluator.evaluate(all));
            }
        }

        List<ServerPlayer> contenders = players.stream()
                .filter(p -> !p.isFolded() && !p.isWaitingForNextHand())
                .sorted((p1, p2) -> Long.compare(p2.getCurrentResult().getScore(), p1.getCurrentResult().getScore()))
                .toList();

        if (!contenders.isEmpty()) {
            long bestScore = contenders.getFirst().getCurrentResult().getScore();
            List<ServerPlayer> winners = contenders.stream()
                    .filter(p -> p.getCurrentResult().getScore() == bestScore)
                    .toList();

            int share = pot / winners.size();
            for (ServerPlayer w : winners) {
                w.addChips(share);
                winnersNames.add(w.getName());
            }
            winners.getFirst().addChips(pot % winners.size());
        }
        pot = 0;
        isHandInProgress = false;
    }

    private void endHandEarly() {
        players.stream()
                .filter(p -> !p.isFolded() && !p.isWaitingForNextHand())
                .findFirst()
                .ifPresent(winner -> {
                    winner.addChips(pot);
                    winnersNames.add(winner.getName());
                });
        pot = 0;
        isHandInProgress = false;
    }

    public void cleanupDisconnected() {
        players.removeIf(p -> !p.isOnline());
        if (isHandInProgress && players.stream().filter(ServerPlayer::isOnline).count() < 2) {
            endHandEarly();
        }
    }

    public int getCurrentPlayerIndex() { return currentPlayerIndex; }
    public List<ServerPlayer> getPlayers() { return players; }
    public int getPot() { return pot; }
    public int getCurrentMaxBet() { return currentMaxBet; }
    public List<Card> getCommunityCards() { return communityCards; }
    public boolean isHandInProgress() { return isHandInProgress; }
    public boolean isShowdown() { return isShowdown; }
    public List<String> getWinnersNames() { return winnersNames; }
    public int getDealerIndex() { return dealerIndex; }
}