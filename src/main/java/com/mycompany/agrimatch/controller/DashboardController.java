package com.mycompany.agrimatch.controller;

import com.mycompany.agrimatch.db.Repository;
import com.mycompany.agrimatch.model.AvailableBatch;
import com.mycompany.agrimatch.model.Farmer;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;

public class DashboardController {

    @FXML private Label farmersCount;
    @FXML private Label buyersCount;
    @FXML private Label batchesCount;
    @FXML private Label requestsCount;
    @FXML private Label statusLabel;

    @FXML private TableView<AvailableBatch> expiringTable;
    @FXML private TableColumn<AvailableBatch, String> cropCol;
    @FXML private TableColumn<AvailableBatch, String> farmerCol;
    @FXML private TableColumn<AvailableBatch, String> qtyCol;
    @FXML private TableColumn<AvailableBatch, String> priceCol;
    @FXML private TableColumn<AvailableBatch, String> expiryCol;
    @FXML private TableColumn<AvailableBatch, Integer> daysCol;

    private final Map<Integer, String> farmerNames = new HashMap<>();

    /** JavaFX calls this automatically after the screen is loaded. */
    @FXML
    public void initialize() {
        cropCol.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().cropName()));
        farmerCol.setCellValueFactory(cd -> new SimpleStringProperty(
                farmerNames.getOrDefault(cd.getValue().farmerId(), "?")));
        qtyCol.setCellValueFactory(cd -> new SimpleStringProperty(
                String.format("%.0f kg", cd.getValue().remainingQty())));
        priceCol.setCellValueFactory(cd -> new SimpleStringProperty(
                String.format("P%.2f", cd.getValue().pricePerKg())));
        expiryCol.setCellValueFactory(cd -> new SimpleStringProperty(
                cd.getValue().expiryDate().toString()));

        // "Time left" shown as a colored badge: red <= 2 days, amber <= 5, green otherwise
        daysCol.setCellValueFactory(cd -> new SimpleObjectProperty<>(cd.getValue().daysLeft()));
        daysCol.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(Integer days, boolean empty) {
                super.updateItem(days, empty);
                if (empty || days == null) { setGraphic(null); setText(null); return; }
                Label badge = new Label(days == 0 ? "Today" : days + (days == 1 ? " day" : " days"));
                badge.getStyleClass().addAll("badge",
                        days <= 2 ? "badge-danger" : days <= 5 ? "badge-warning" : "badge-ok");
                setGraphic(badge);
                setText(null);
            }
        });

        loadData();
    }

    @FXML
    private void onRefresh() {
        loadData();
    }

    private void loadData() {
        try {
            List<Farmer> farmers = Repository.farmers();
            farmerNames.clear();
            for (Farmer f : farmers) farmerNames.put(f.id(), f.name());

            List<AvailableBatch> batches = Repository.availableBatches();   // already sorted by days_left

            farmersCount.setText(String.valueOf(farmers.size()));
            buyersCount.setText(String.valueOf(Repository.buyers().size()));
            batchesCount.setText(String.valueOf(batches.size()));
            requestsCount.setText(String.valueOf(Repository.openRequests().size()));

            expiringTable.setItems(FXCollections.observableArrayList(
                    batches.subList(0, Math.min(10, batches.size()))));

            statusLabel.getStyleClass().setAll("label", "status-label", "status-success");
            statusLabel.setText("Connected to database.");
        } catch (SQLException e) {
            statusLabel.getStyleClass().setAll("label", "status-label", "status-error");
            statusLabel.setText("Database error: " + e.getMessage());
        }
    }
}
