import java.io.*;
import java.net.Socket;
import java.net.SocketException;

public class ClientHandler implements Runnable {
    private final Socket socket;
    private final GameServer server;
    private ObjectOutputStream out;
    private ObjectInputStream in;
    private volatile ServerPlayer player;

    public ClientHandler(Socket socket, GameServer server) {
        this.socket = socket;
        this.server = server;
    }

    private void initStreams() throws IOException {
        this.out = new ObjectOutputStream(socket.getOutputStream());
        this.out.flush();
        this.in = new ObjectInputStream(socket.getInputStream());
    }

    private void cleanup() {
        server.removeClient(this);
        try {
            if (socket != null && !socket.isClosed()) {
                socket.close();
            }
        } catch (IOException ignored) {
        }
    }

    private void processMessages() throws IOException, ClassNotFoundException {
        while (!Thread.currentThread().isInterrupted()) {
            Object input = in.readObject();
            if (input instanceof ActionMessage action && player != null) {
                server.handleAction(player.getSessionID(), action);
            }
        }
    }

    @Override
    public void run() {
        try {
            initStreams();
            if (in.readObject() instanceof String name) {
                this.player = server.registerPlayer(this, name);
            } else {
                return;
            }
            processMessages();
        } catch (EOFException | SocketException e) {
        } catch (ClassNotFoundException e) {
            System.err.println("Protocol mismatch: " + e.getMessage());
        } catch (Exception e) {
            System.err.println("Handler error [" + (player != null ? player.getName() : "unknown") + "]: " + e.getMessage());
            e.printStackTrace();
        } finally {
            cleanup();
        }
    }

    public synchronized void sendState(GameStateDTO state) {
        if (out == null)
            return;
        try {
            out.reset();
            out.writeObject(state);
            out.flush();
        } catch (IOException e) {
            cleanup();
        }
    }

    public void setPlayer(ServerPlayer player) {
        this.player = player;
    }

    public ServerPlayer getPlayer() {
        return player;
    }
}