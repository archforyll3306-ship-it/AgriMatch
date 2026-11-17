package com.mycompany.agrimatch.controller;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;

public class MainController {

    @FXML private StackPane content;

    @FXML
    public void initialize() {
        show("dashboard");              // the first screen shown when the app opens
    }

    @FXML private void showDashboard() { show("dashboard"); }
    @FXML private void showMatches()   { show("matches"); }

    /** Loads /fxml/<name>.fxml into the center area. */
    private void show(String name) {
        try {
            Node view = FXMLLoader.load(getClass().getResource(
                    "/com/mycompany/agrimatch/fxml/" + name + ".fxml"));
            content.getChildren().setAll(view);
        } catch (Exception e) {
            content.getChildren().setAll(new Label("Could not load " + name + ": " + e.getMessage()));
        }
    }
}