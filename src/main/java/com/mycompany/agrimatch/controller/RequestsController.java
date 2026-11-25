package com.mycompany.agrimatch.controller;

import com.mycompany.agrimatch.db.RequestService;
import com.mycompany.agrimatch.db.Repository;
import com.mycompany.agrimatch.model.Buyer;
import com.mycompany.agrimatch.model.Crop;
import com.mycompany.agrimatch.model.RequestRow;
import java.sql.SQLException;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.util.StringConverter;

public class RequestsController {

    @FXML private ComboBox<Buyer> buyerBox;
    @FXML private ComboBox<Crop> cropBox;
    @FXML private TextField qtyField;
    @FXML private TextField maxPriceField;
    @FXML private Label statusLabel;
    @FXML private Button cancelButton;
    @FXML private TableView<RequestRow> dataTable;
    @FXML private TableColumn<RequestRow, String> idCol, buyerCol, cropCol, qtyCol, priceCol, reservedCol, statusCol;

    @FXML
    public void initialize() {
        idCol.setCellValueFactory(cd -> new SimpleStringProperty("#" + cd.getValue().id()));
        buyerCol.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().buyer()));
        cropCol.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().crop()));
        qtyCol.setCellValueFactory(cd -> new SimpleStringProperty(String.format("%.0f kg", cd.getValue().qty())));
        priceCol.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().maxPrice() == null
                ? "no limit" : String.format("P%.2f", cd.getValue().maxPrice())));
        reservedCol.setCellValueFactory(cd -> new SimpleStringProperty(String.format("%.0f kg", cd.getValue().reserved())));
        statusCol.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().status()));

        buyerBox.setConverter(new StringConverter<Buyer>() {
            @Override public String toString(Buyer b) { return b == null ? "" : b.name(); }
            @Override public Buyer fromString(String s) { return null; }
        });
        cropBox.setConverter(new StringConverter<Crop>() {
            @Override public String toString(Crop c) { return c == null ? "" : c.name(); }
            @Override public Crop fromString(String s) { return null; }
        });
        cancelButton.disableProperty().bind(dataTable.getSelectionModel().selectedItemProperty().isNull());
        statusLabel.visibleProperty().bind(statusLabel.textProperty().isNotEmpty());
        statusLabel.managedProperty().bind(statusLabel.visibleProperty());
        loadData();
    }

    private void loadData() {
        try {
            buyerBox.setItems(FXCollections.observableArrayList(Repository.buyers()));
            cropBox.setItems(FXCollections.observableArrayList(Repository.crops()));
            dataTable.setItems(FXCollections.observableArrayList(Repository.requestRows()));
        } catch (SQLException e) {
            setStatus("Database error: " + e.getMessage(), true);
        }
    }

    @FXML private void onRefresh() { loadData(); setStatus("", false); }

    @FXML
    private void onClear() {
        buyerBox.getSelectionModel().clearSelection();
        cropBox.getSelectionModel().clearSelection();
        qtyField.clear();
        maxPriceField.clear();
        setStatus("", false);
    }

    @FXML
    private void onAdd() {
        Buyer b = buyerBox.getValue();
        Crop c = cropBox.getValue();
        if (b == null || c == null) { setStatus("Choose a buyer and a crop.", true); return; }
        double qty;
        Double max = null;
        try {
            qty = Double.parseDouble(qtyField.getText().trim());
            String m = maxPriceField.getText().trim();
            if (!m.isEmpty()) max = Double.parseDouble(m);
        } catch (NumberFormatException e) {
            setStatus("Quantity and max price must be numbers.", true);
            return;
        }
        try {
            int id = RequestService.addRequest(b.id(), c.id(), qty, max);
            onClear();
            loadData();
            setStatus("Request #" + id + " added.", false);
        } catch (IllegalStateException e) {
            setStatus(e.getMessage(), true);
        } catch (SQLException e) {
            setStatus("Database error: " + e.getMessage(), true);
        }
    }

    @FXML
    private void onCancel() {
        RequestRow r = dataTable.getSelectionModel().getSelectedItem();
        if (r == null) return;
        Alert a = new Alert(Alert.AlertType.CONFIRMATION, "Cancel request #" + r.id() + "?",
                ButtonType.OK, ButtonType.CANCEL);
        a.setHeaderText("Cancel request");
        if (a.showAndWait().orElse(ButtonType.CANCEL) != ButtonType.OK) return;
        try {
            RequestService.cancelRequest(r.id());
            loadData();
            setStatus("Request #" + r.id() + " cancelled.", false);
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