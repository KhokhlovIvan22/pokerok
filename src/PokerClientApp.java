import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class PokerClientApp extends Application {

    @Override
    public void start(Stage primaryStage) {
        LoginDialog login = new LoginDialog();
        LoginDialog.Result result = login.display();

        if (result.success) {
            GameController controller = new GameController(primaryStage, result.name, result.host, result.port);
            primaryStage.setTitle("Poker App");
            primaryStage.setScene(new Scene(controller.getView()));
            primaryStage.show();
        } else {
            System.exit(0);
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}