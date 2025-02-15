package de.hssfds.rd.javafx2mqtt;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class Main extends Application {

    private MainController controller;

    @Override
    public void start(Stage stage) throws Exception {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("Main.fxml"));
        Parent root = loader.load();
        // Store the controller for cleanup purposes
        controller = loader.getController();
        Scene scene = new Scene(root, 400, 250);
        stage.setScene(scene);
        stage.setTitle("OPC UA and MQTT GUI");
        stage.show();
    }

    @Override
    public void stop() throws Exception {
        // When closing the application, ensure we clean up the resources.
        if (controller != null) {
            controller.cleanup();
        }
        super.stop();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
