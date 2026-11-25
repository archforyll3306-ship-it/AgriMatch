package com.mycompany.agrimatch.controller;

import com.mycompany.agrimatch.db.BatchService;
import com.mycompany.agrimatch.db.Repository;
import com.mycompany.agrimatch.model.BatchRow;
import com.mycompany.agrimatch.model.Crop;
import com.mycompany.agrimatch.model.Farmer;
import java.sql.SQLException;
import java.time.LocalDate;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.util.StringConverter;

public class BatchesController {

    @FXML private ComboBox<Farmer> farmerBox;
    @FXML private ComboBox<Crop> cropBox;
    @FXML private TextField qtyField;
    @FXML private TextField priceField;
    @FXML private DatePicker harvestPicker;
    @FXML private Label statusLabel;
    @FXML private TableView<BatchRow> dataTable;
    @FXML private TableColumn<BatchRow, String> idCol, farmerCol, cropCol, qtyCol, priceCol, harvestCol, statusCol;

    @FXML
    public void initialize() {
        idCol.setCellValueFactory(cd -> new SimpleStringProperty("#" + cd.getValue().id()));
        farmerCol.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().farmer()));
        cropCol.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().crop()));
        qtyCol.setCellValueFactory(cd -> new SimpleStringProperty(String.format("%.0f kg", cd.getValue().qty())));
        priceCol.setCellValueFactory(cd -> new SimpleStringProperty(String.format("P%.2f", cd.getValue().price())));
        harvestCol.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().harvest().toString()));
        statusCol.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().status()));

        farmerBox.setConverter(new StringConverter<Farmer>() {
            @Override public String toString(Farmer f) { return f == null ? "" : f.name(); }
            @Override public Farmer fromString(String s) { return null; }
        });
        cropBox.setConverter(new StringConverter<Crop>() {
            @Override public String toString(Crop c) { return c == null ? "" : c.name(); }
            @Override public Crop fromString(String s) { return null; }
        });
        harvestPicker.setValue(LocalDate.now());
        statusLabel.visibleProperty().bind(statusLabel.textProperty().isNotEmpty());
        statusLabel.managedProperty().bind(statusLabel.visibleProperty());
        loadData();
    }

    private void loadData() {
        try {
            farmerBox.setItems(FXCollections.observableArrayList(Repository.farmers()));
            cropBox.setItems(FXCollections.observableArrayList(Repository.crops()));
            dataTable.setItems(FXCollections.observableArrayList(Repository.batchRows()));
        } catch (SQLException e) {
            setStatus("Database error: " + e.getMessage(), true);
        }
    }

    @FXML private void onRefresh() { loadData(); setStatus("", false); }

    @FXML
    private void onClear() {
        farmerBox.getSelectionModel().clearSelection();
        cropBox.getSelectionModel().clearSelection();
        qtyField.clear();
        priceField.clear();
        harvestPicker.setValue(LocalDate.now());
        setStatus("", false);
    }

    @FXML
    private void onAdd() {
        Farmer f = farmerBox.getValue();
        Crop c = cropBox.getValue();
        if (f == null || c == null) { setStatus("Choose a farmer and a crop.", true); return; }
        double qty, price;
        try {
            qty = Double.parseDouble(qtyField.getText().trim());
            price = Double.parseDouble(priceField.getText().trim());
        } catch (NumberFormatException e) {
            setStatus("Quantity and price must be numbers.", true);
            return;
        }
        try {
            int id = BatchService.addBatch(f.id(), c.id(), qty, price, harvestPicker.getValue());
            onClear();
            loadData();
            setStatus("Batch #" + id + " listed.", false);
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