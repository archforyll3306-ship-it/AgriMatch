package com.mycompany.agrimatch.controller;

import com.mycompany.agrimatch.db.Repository;
import java.sql.SQLException;
import javafx.fxml.FXML;
import javafx.scene.control.Label;

public class DashboardController {

    @FXML private Label farmersCount;
    @FXML private Label buyersCount;
    @FXML private Label batchesCount;
    @FXML private Label requestsCount;
    @FXML private Label statusLabel;

    /** JavaFX calls this automatically after the screen is loaded. */
    @FXML
    public void initialize() {
        try {
            farmersCount.setText(String.valueOf(Repository.farmers().size()));
            buyersCount.setText(String.valueOf(Repository.buyers().size()));
            batchesCount.setText(String.valueOf(Repository.availableBatches().size()));
            requestsCount.setText(String.valueOf(Repository.openRequests().size()));
            statusLabel.setText("Connected to database.");
        } catch (SQLException e) {
            statusLabel.setText("Database error: " + e.getMessage());
        }
    }
}