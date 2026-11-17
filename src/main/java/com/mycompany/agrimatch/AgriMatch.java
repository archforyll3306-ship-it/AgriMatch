package com.mycompany.agrimatch;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class AgriMatch extends Application {

    @Override
    public void start(Stage stage) throws Exception {
        Parent root = FXMLLoader.load(
                getClass().getResource("/com/mycompany/agrimatch/fxml/main.fxml"));

        stage.setTitle("AgriMatch");
        stage.setScene(new Scene(root, 1000, 650));
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}