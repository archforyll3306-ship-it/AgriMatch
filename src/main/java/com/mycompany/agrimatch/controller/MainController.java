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
    @FXML private Button navBatches;
    @FXML private Button navRequests;
    @FXML private Button navReservations;

    @FXML
    public void initialize() {
        show("dashboard", navDashboard);   // first screen shown when the app opens
    }

    @FXML private void showDashboard() { show("dashboard", navDashboard); }
    @FXML private void showFarmers()   { show("farmers", navFarmers); }
    @FXML private void showBuyers()    { show("buyers", navBuyers); }
    @FXML private void showBatches()   { show("batches", navBatches); }
    @FXML private void showRequests()  { show("requests", navRequests); }
    @FXML private void showMatches()   { show("matches", navMatches); }
    @FXML private void showReservations() { show("reservations", navReservations); }

    /** Loads /fxml/<name>.fxml into the center area and highlights its menu button. */
    private void show(String name, Button active) {
        for (Button b : List.of(navDashboard, navFarmers, navBuyers, navBatches, navRequests, navMatches, navReservations)) {
            b.getStyleClass().remove(ACTIVE);
        }
        active.getStyleClass().add(ACTIVE);
        try {
            Node view = FXMLLoader.load(getClass().getResource(
                    "/com/mycompany/agrimatch/fxml/" + name + ".fxml"));
            content.getChildren().setAll(view);
        } catch (Exception e) {
            e.printStackTrace();
            Throwable root = e;
            while (root.getCause() != null) root = root.getCause();   // the real reason
            Label err = new Label("Could not load " + name + ":\n" + e.getMessage()
                    + "\n\nCause: " + root);
            err.setWrapText(true);
            err.setMaxWidth(650);
            err.getStyleClass().add("status-error");
            content.getChildren().setAll(err);
        }
    }
}