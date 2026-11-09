package com.mycompany.agrimatch;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

public class AgriMatch extends Application {

    @Override
    public void start(Stage stage) {

        Label welcomeLabel = new Label("Welcome to AgriMatch!");

        StackPane root = new StackPane(welcomeLabel);

        Scene scene = new Scene(root, 1000, 650);

        stage.setTitle("AgriMatch");
        stage.setScene(scene);
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}