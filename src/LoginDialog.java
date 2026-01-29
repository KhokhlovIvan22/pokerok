import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

public class LoginDialog {
    public static class Result {
        String name;
        String host;
        int port;
        boolean success;
    }

    public Result display() {
        Stage stage = new Stage();
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.initStyle(StageStyle.UTILITY);
        stage.setTitle("Poker initialization");

        VBox layout = new VBox(10);
        layout.setAlignment(Pos.CENTER);
        layout.setPadding(new Insets(20));
        layout.setStyle("-fx-background-color: #2c3e50;");

        TextField nameField = new TextField();
        nameField.setPromptText("Enter name");
        TextField hostField = new TextField();
        hostField.setPromptText("Enter host");
        TextField portField = new TextField();
        portField.setPromptText("Enter port");

        Button btnConnect = new Button("CONNECT");
        btnConnect.setStyle("-fx-background-color: #27ae60; -fx-text-fill: white; -fx-font-weight: bold;");
        btnConnect.setPrefWidth(200);

        Result result = new Result();

        btnConnect.setOnAction(e -> {
            if (!nameField.getText().isEmpty() && !hostField.getText().isEmpty()) {
                try {
                    result.name = nameField.getText();
                    result.host = hostField.getText();
                    result.port = Integer.parseInt(portField.getText());
                    result.success = true;
                    stage.close();
                } catch (NumberFormatException ex) {
                    portField.setStyle("-fx-border-color: red;");
                }
            }
        });

        layout.getChildren().addAll(nameField, hostField, portField, btnConnect);

        Scene scene = new Scene(layout, 300, 200);
        stage.setScene(scene);
        stage.showAndWait();

        return result;
    }
}