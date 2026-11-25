package com.mycompany.agrimatch.controller;

import com.mycompany.agrimatch.db.Repository;
import com.mycompany.agrimatch.db.SaleService;
import com.mycompany.agrimatch.db.StockService;
import com.mycompany.agrimatch.model.ReservationRow;
import java.sql.SQLException;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;

public class ReservationsController {

    @FXML private Label statusLabel;
    @FXML private Button confirmButton, completeButton, cancelButton;
    @FXML private TableView<ReservationRow> dataTable;
    @FXML private TableColumn<ReservationRow, String> idCol, buyerCol, farmerCol, cropCol, qtyCol, priceCol, distCol, statusCol;

    @FXML
    public void initialize() {
        idCol.setCellValueFactory(cd -> new SimpleStringProperty("#" + cd.getValue().id()));
        buyerCol.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().buyer()));
        farmerCol.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().farmer()));
        cropCol.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().crop()));
        qtyCol.setCellValueFactory(cd -> new SimpleStringProperty(String.format("%.0f kg", cd.getValue().qty())));
        priceCol.setCellValueFactory(cd -> new SimpleStringProperty(String.format("P%.2f", cd.getValue().price())));
        distCol.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().km() == null
                ? "?" : String.format("%.1f km", cd.getValue().km())));
        statusCol.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().status()));

        // buttons only enabled when the selected reservation is in a state they apply to
        dataTable.getSelectionModel().selectedItemProperty().addListener((o, a, r) -> updateButtons(r));
        updateButtons(null);
        statusLabel.visibleProperty().bind(statusLabel.textProperty().isNotEmpty());
        statusLabel.managedProperty().bind(statusLabel.visibleProperty());
        loadData();
    }

    private void updateButtons(ReservationRow r) {
        String s = r == null ? "" : r.status();
        confirmButton.setDisable(!s.equals("PENDING"));
        completeButton.setDisable(!s.equals("CONFIRMED"));
        cancelButton.setDisable(!(s.equals("PENDING") || s.equals("CONFIRMED")));
    }

    private void loadData() {
        try {
            dataTable.setItems(FXCollections.observableArrayList(Repository.reservationRows()));
            updateButtons(null);
        } catch (SQLException e) {
            setStatus("Database error: " + e.getMessage(), true);
        }
    }

    @FXML private void onRefresh() { loadData(); setStatus("", false); }

    @FXML
    private void onConfirm() {
        ReservationRow r = dataTable.getSelectionModel().getSelectedItem();
        if (r == null) return;
        run(() -> SaleService.confirm(r.id()), "Reservation #" + r.id() + " confirmed.");
    }

    @FXML
    private void onComplete() {
        ReservationRow r = dataTable.getSelectionModel().getSelectedItem();
        if (r == null) return;
        TextInputDialog d = new TextInputDialog(String.format("%.2f", r.price()));
        d.setHeaderText("Complete sale of " + String.format("%.0f", r.qty()) + " kg " + r.crop());
        d.setContentText("Final price per kg:");
        d.showAndWait().ifPresent(text -> {
            double price;
            try { price = Double.parseDouble(text.trim()); }
            catch (NumberFormatException e) { setStatus("Price must be a number.", true); return; }
            run(() -> {
                int tx = SaleService.complete(r.id(), price);
                setStatus(String.format("Sale recorded (transaction #%d, P%.2f total).", tx, price * r.qty()), false);
            }, null);
        });
    }

    @FXML
    private void onCancel() {
        ReservationRow r = dataTable.getSelectionModel().getSelectedItem();
        if (r == null) return;
        Alert a = new Alert(Alert.AlertType.CONFIRMATION, "Cancel reservation #" + r.id() + "?",
                ButtonType.OK, ButtonType.CANCEL);
        a.setHeaderText("Cancel reservation");
        if (a.showAndWait().orElse(ButtonType.CANCEL) != ButtonType.OK) return;
        run(() -> StockService.cancel(r.id()), "Reservation #" + r.id() + " cancelled.");
    }

    @FXML
    private void onMarkSpoiled() {
        run(() -> {
            int n = StockService.markSpoiled();
            setStatus(n + " expired batch(es) marked as spoiled.", false);
        }, null);
    }

    private interface Action { void run() throws SQLException; }

    /** Runs a database action, reloads the table, and shows success or the error message. */
    private void run(Action action, String successMessage) {
        try {
            action.run();
            loadData();
            if (successMessage != null) setStatus(successMessage, false);
        } catch (IllegalStateException e) {
            setStatus(e.getMessage(), true);
        } catch (SQLException e) {
            setStatus("Database error: " + e.getMessage(), true);
        }
    }

    private void setStatus(String message, boolean error) {
        statusLabel.getStyleClass().setAll("label", "status-label");
        if (!message.isEmpty()) statusLabel.getStyleClass().add(error ? "status-error" : "status-success");
        statusLabel.setText(message);
    }
}