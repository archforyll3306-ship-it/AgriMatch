package com.mycompany.agrimatch.controller;

import java.util.List;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;

public class MainController {

    private static final String ACTIVE = "nav-active";

    @FXML private StackPane content;
    @FXML private Button navDashboard;
    @FXML private Button navFarmers;
    @FXML private Button navBuyers;
    @FXML private Button navMatches;

    @FXML
    public void initialize() {
        show("dashboard", navDashboard);   // first screen shown when the app opens
    }

    @FXML private void showDashboard() { show("dashboard", navDashboard); }
    @FXML private void showFarmers()   { show("farmers", navFarmers); }
    @FXML private void showBuyers()    { show("buyers", navBuyers); }
    @FXML private void showMatches()   { show("matches", navMatches); }

    /** Loads /fxml/<name>.fxml into the center area and highlights its menu button. */
    private void show(String name, Button active) {
        for (Button b : List.of(navDashboard, navFarmers, navBuyers, navMatches)) {
            b.getStyleClass().remove(ACTIVE);
        }
        active.getStyleClass().add(ACTIVE);
        try {
            Node view = FXMLLoader.load(getClass().getResource(
                    "/com/mycompany/agrimatch/fxml/" + name + ".fxml"));
            content.getChildren().setAll(view);
        } catch (Exception e) {
            Label err = new Label("Could not load " + name + ": " + e.getMessage());
            err.getStyleClass().add("status-error");
            content.getChildren().setAll(err);
        }
    }
}
