import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

public class GameServer {
    private final Table table = new Table();
    private final List<ClientHandler> clients = new CopyOnWriteArrayList<>();
    private final int startChips;

    public GameServer(int startChips) {
        this.startChips = startChips;
    }

    private synchronized void cleanDisconnectedPlayers() {
        table.cleanupDisconnected();
    }

    private GameStateDTO createDTO(String recipientName) {
        GameStateDTO dto = new GameStateDTO();
        dto.pot = table.getPot();
        dto.currentMaxBet = table.getCurrentMaxBet();
        dto.currentPlayerIndex = table.getCurrentPlayerIndex();
        dto.dealerIndex = table.getDealerIndex();
        dto.communityCards = new ArrayList<>(table.getCommunityCards());
        dto.isHandInProgress = table.isHandInProgress();
        dto.isShowdown = table.isShowdown();
        dto.winners = new ArrayList<>(table.getWinnersNames());

        dto.players = table.getPlayers().stream()
                .filter(ServerPlayer::isOnline)
                .map(sp -> {
                    GameStateDTO.PlayerDTO p = new GameStateDTO.PlayerDTO();
                    p.name = sp.getName();
                    p.chips = sp.getChips();
                    p.currentBet = sp.getCurrentBet();
                    p.isFolded = sp.isFolded();
                    p.isAllIn = sp.isAllIn();
                    p.isWaitingForNextHand = sp.isWaitingForNextHand();

                    if (dto.isShowdown || sp.getName().equals(recipientName)) {
                        p.holeCards = new ArrayList<>(sp.getCards());
                        p.handResult = sp.getCurrentResult();
                    } else {
                        p.holeCards = null;
                        p.handResult = null;
                    }
                    return p;
                }).toList();
        return dto;
    }

    public void broadcastState() {
        clients.forEach(c -> {
            if (c.getPlayer() != null) {
                c.sendState(createDTO(c.getPlayer().getName()));
            }
        });
    }

    private void startConsoleThread(Scanner sc) {
        Thread consoleThread = new Thread(() -> {
            while (sc.hasNext()) {
                String command = sc.next();
                if (command.equalsIgnoreCase("run")) {
                    synchronized (this) {
                        long onlineCount = table.getPlayers().stream().filter(ServerPlayer::isOnline).count();
                        if (onlineCount < 2) {
                            System.out.println("Need at least 2 online players to start!");
                            System.out.println("Type 'run' to start new hand");
                            continue;
                        }

                        cleanDisconnectedPlayers();
                        table.startNewHand();
                        System.out.println("ROUND STARTED");
                        broadcastState();
                    }
                }
            }
        });
        consoleThread.setDaemon(true);
        consoleThread.start();
    }

    public void start() {
        Scanner sc = new Scanner(System.in);
        System.out.print("Enter port: ");
        int port = sc.nextInt();

        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println("Server started on port " + port + ". Type 'run' to start new hand");
            startConsoleThread(sc);
            while (!Thread.currentThread().isInterrupted()) {
                Socket s = serverSocket.accept();
                new Thread(new ClientHandler(s, this)).start();
            }
        } catch (IOException e) {
            System.err.println("Server error: " + e.getMessage());
        }
    }

    public synchronized ServerPlayer registerPlayer(ClientHandler handler, String name) {
        table.getPlayers().removeIf(p -> p.getName().equalsIgnoreCase(name));
        ServerPlayer p = new ServerPlayer(UUID.randomUUID().toString(), name, startChips);
        table.addPlayer(p);
        handler.setPlayer(p);
        clients.add(handler);
        System.out.println("Connected: " + name);
        broadcastState();
        return p;
    }

    public synchronized void handleAction(String sid, ActionMessage msg) {
        if (!table.isHandInProgress())
            return;

        List<ServerPlayer> players = table.getPlayers();
        int currentIndex = table.getCurrentPlayerIndex();
        if (currentIndex >= 0 && currentIndex < players.size()) {
            ServerPlayer current = players.get(currentIndex);
            if (current.getSessionID().equals(sid)) {
                table.handleAction(msg.action, msg.amount);
                if (!table.isHandInProgress()) {
                    System.out.println("ROUND IS OVER. Winners: " + table.getWinnersNames());
                    System.out.println("Type 'run' to start new hand");
                }
                broadcastState();
            }
        }
    }

    public synchronized void removeClient(ClientHandler h) {
        ServerPlayer p = h.getPlayer();
        if (p != null) {
            p.setOnline(false);
            p.setFolded(true);
            System.out.println("Disconnected: " + p.getName());

            if (table.isHandInProgress()) {
                long activeCount = table.getPlayers().stream().filter(ServerPlayer::isOnline).count();
                if (activeCount < 2) {
                    System.out.println("!!! [EMERGENCY STOP] Not enough players");
                    System.out.println("Type 'run' to start new hand");
                    table.cleanupDisconnected();
                }
            } else {
                table.cleanupDisconnected();
            }
        }
        clients.remove(h);
        broadcastState();
    }

    public static void main(String[] args) {
        new GameServer(50).start();
    }
}