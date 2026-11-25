package com.mycompany.agrimatch.controller;

import com.mycompany.agrimatch.db.RegistrationService;
import com.mycompany.agrimatch.db.Repository;
import com.mycompany.agrimatch.model.Barangay;
import com.mycompany.agrimatch.model.Buyer;
import java.sql.SQLException;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.util.StringConverter;

public class BuyersController {

    @FXML private TextField nameField;
    @FXML private TextField contactField;
    @FXML private ComboBox<Barangay> barangayBox;
    @FXML private Button addButton;
    @FXML private Label statusLabel;
    @FXML private Label countLabel;
    @FXML private TextField searchField;
    @FXML private TableView<Buyer> dataTable;
    @FXML private TableColumn<Buyer, Integer> idCol;
    @FXML private TableColumn<Buyer, String> nameCol;
    @FXML private TableColumn<Buyer, String> contactCol;
    @FXML private TableColumn<Buyer, String> addressCol;

    private final ObservableList<Buyer> data = FXCollections.observableArrayList();
    private FilteredList<Buyer> filtered;

    @FXML
    public void initialize() {
        idCol.setCellValueFactory(cd -> new SimpleObjectProperty<>(cd.getValue().id()));
        nameCol.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().name()));
        contactCol.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().contact()));
        addressCol.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().address()));

        barangayBox.setConverter(new StringConverter<Barangay>() {
            @Override public String toString(Barangay b) { return b == null ? "" : b.name(); }
            @Override public Barangay fromString(String s) { return null; }
        });

        // search box filters the table; clicking a column header still sorts it
        filtered = new FilteredList<>(data, r -> true);
        SortedList<Buyer> sorted = new SortedList<>(filtered);
        sorted.comparatorProperty().bind(dataTable.comparatorProperty());
        dataTable.setItems(sorted);
        searchField.textProperty().addListener((obs, o, text) -> {
            String q = text == null ? "" : text.trim().toLowerCase();
            filtered.setPredicate(r -> q.isEmpty()
                    || contains(r.name(), q) || contains(r.contact(), q) || contains(r.address(), q));
            updateCount();
        });

        statusLabel.visibleProperty().bind(statusLabel.textProperty().isNotEmpty());
        statusLabel.managedProperty().bind(statusLabel.visibleProperty());

        loadData();
    }

    private static boolean contains(String value, String q) {
        return value != null && value.toLowerCase().contains(q);
    }

    private void updateCount() {
        countLabel.setText(filtered.size() + " of " + data.size());
    }

    /** Reloads the barangay list and the table from the database. */
    private void loadData() {
        try {
            barangayBox.setItems(FXCollections.observableArrayList(Repository.barangays()));
            data.setAll(Repository.buyers());
            updateCount();
        } catch (SQLException e) {
            setStatus("Database error: " + e.getMessage(), true);
        }
    }

    @FXML
    private void onRefresh() {
        loadData();
        setStatus("", false);
    }

    @FXML
    private void onClear() {
        nameField.clear();
        contactField.clear();
        barangayBox.getSelectionModel().clearSelection();
        setStatus("", false);
    }

    /** Validation and duplicate checks live in RegistrationService; we just show its message. */
    @FXML
    private void onAdd() {
        Barangay b = barangayBox.getValue();
        try {
            int id = RegistrationService.addBuyer(
                    nameField.getText(), contactField.getText(), b == null ? -1 : b.id());
            onClear();
            loadData();
            setStatus("Buyer #" + id + " added.", false);
        } catch (IllegalStateException e) {
            setStatus(e.getMessage(), true);          // e.g. "Contact number must be 7 to 15 digits."
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
