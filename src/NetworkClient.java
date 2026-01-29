import javafx.application.Platform;
import java.io.*;
import java.net.Socket;
import java.util.function.Consumer;

public class NetworkClient {
    private Socket socket;
    private ObjectOutputStream out;
    private ObjectInputStream in;
    private volatile boolean isRunning = false;

    private Consumer<GameStateDTO> onStateReceived;

    public NetworkClient(Consumer<GameStateDTO> onStateReceived) {
        this.onStateReceived = onStateReceived;
    }

    public boolean connect(String host, int port, String playerName) {
        try {
            socket = new Socket(host, port);
            out = new ObjectOutputStream(socket.getOutputStream());
            in = new ObjectInputStream(socket.getInputStream());

            // Отправляем имя сразу после подключения (протокол сервера)
            out.writeObject(playerName);
            out.flush();

            startListening();
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    private void startListening() {
        isRunning = true;
        Thread listenerThread = new Thread(() -> {
            try {
                while (isRunning && !Thread.currentThread().isInterrupted()) {
                    Object obj = in.readObject();
                    if (obj instanceof GameStateDTO state) {
                        // Важно: перекидываем обработку в FX Thread
                        Platform.runLater(() -> onStateReceived.accept(state));
                    }
                }
            } catch (IOException | ClassNotFoundException e) {
                System.out.println("Connection lost: " + e.getMessage());
            } finally {
                close();
            }
        });
        listenerThread.setDaemon(true);
        listenerThread.start();
    }

    public void sendAction(PlayerAction action, int amount) {
        if (out == null)
            return;
        new Thread(() -> {
            try {
                out.writeObject(new ActionMessage(action, amount));
                out.flush();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }).start();
    }

    public void close() {
        isRunning = false;
        try {
            if (socket != null)
                socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
