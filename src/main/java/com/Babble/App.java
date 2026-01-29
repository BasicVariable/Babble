package com.Babble;

import com.Babble.Config.Config;
import com.Babble.Config.ConfigManager;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import java.io.IOException;

public class App extends Application {
    @Override
    public void start(Stage stage) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(
            App.class.getResource("/main.fxml")
        );
        Parent root = fxmlLoader.load();

        MenuController controller = fxmlLoader.getController();
        Config config = ConfigManager.load();
        controller.setConfig(config);

        controller.setStage(stage);

        Scene scene = new Scene(root, 600, 400);
        scene.setFill(Color.TRANSPARENT);

        stage.initStyle(StageStyle.TRANSPARENT);
        stage.setScene(scene);
        stage.setAlwaysOnTop(true);
        stage.setTitle("Babble");
        stage.show();

        controller.initClickThrough();
    }

    public static void main(String[] args) {
        launch();
    }
}
