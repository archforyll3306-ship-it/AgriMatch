package com.mycompany.agrimatch.controller;

import com.mycompany.agrimatch.db.Repository;
import com.mycompany.agrimatch.db.ReservationService;
import com.mycompany.agrimatch.matching.MatchResult;
import com.mycompany.agrimatch.matching.Matcher;
import com.mycompany.agrimatch.model.Buyer;
import com.mycompany.agrimatch.model.BuyerRequest;
import com.mycompany.agrimatch.model.Farmer;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.util.StringConverter;

public class MatchesController {

    @FXML private ComboBox<BuyerRequest> requestBox;
    @FXML private Label requestInfo;
    @FXML private TableView<MatchResult> matchTable;
    @FXML private TableColumn<MatchResult, String> rankCol;
    @FXML private TableColumn<MatchResult, String> farmerCol;
    @FXML private TableColumn<MatchResult, String> batchCol;
    @FXML private TableColumn<MatchResult, String> priceCol;
    @FXML private TableColumn<MatchResult, String> distanceCol;
    @FXML private TableColumn<MatchResult, String> daysCol;
    @FXML private TableColumn<MatchResult, String> scoreCol;
    @FXML private TableColumn<MatchResult, String> qtyCol;
    @FXML private Button reserveButton;
    @FXML private Label statusLabel;

    private final Map<Integer, Buyer> buyers = new HashMap<>();
    private final Map<Integer, Farmer> farmers = new HashMap<>();
    private BuyerRequest currentRequest;   // the chosen request, with quantity = kg still needed

    @FXML
    public void initialize() {
        // tell each column what text to show for a row
        rankCol.setCellValueFactory(cd -> new SimpleStringProperty(
                String.valueOf(matchTable.getItems().indexOf(cd.getValue()) + 1)));
        farmerCol.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().farmer().name()));
        batchCol.setCellValueFactory(cd -> new SimpleStringProperty("#" + cd.getValue().batch().batchId()));
        priceCol.setCellValueFactory(cd -> new SimpleStringProperty(
                String.format("P%.2f", cd.getValue().batch().pricePerKg())));
        distanceCol.setCellValueFactory(cd -> new SimpleStringProperty(
                cd.getValue().distanceKm() == null ? "?" : String.format("%.1f km", cd.getValue().distanceKm())));
        daysCol.setCellValueFactory(cd -> new SimpleStringProperty(
                String.valueOf(cd.getValue().batch().daysLeft())));
        scoreCol.setCellValueFactory(cd -> new SimpleStringProperty(
                String.format("%.3f", cd.getValue().total())));
        qtyCol.setCellValueFactory(cd -> new SimpleStringProperty(
                String.format("%.0f kg", cd.getValue().suggestedQty())));

        // how a request is written in the drop-down
        requestBox.setConverter(new StringConverter<BuyerRequest>() {
            @Override public String toString(BuyerRequest r) {
                if (r == null) return "";
                Buyer b = buyers.get(r.buyerId());
                return "Request #" + r.id() + " - " + (b == null ? "?" : b.name())
                        + " - " + String.format("%.0f", r.quantity()) + " kg " + r.cropName();
            }
            @Override public BuyerRequest fromString(String s) { return null; }
        });

        requestBox.setOnAction(e -> showMatches());
        matchTable.getSelectionModel().selectedItemProperty().addListener(
                (obs, oldItem, newItem) -> reserveButton.setDisable(newItem == null));

        loadData();
    }

    /** Reads farmers, buyers and open requests from the database. */
    private void loadData() {
        try {
            farmers.clear();
            buyers.clear();
            for (Farmer f : Repository.farmers()) farmers.put(f.id(), f);
            for (Buyer b : Repository.buyers()) buyers.put(b.id(), b);

            requestBox.setItems(FXCollections.observableArrayList(Repository.openRequests()));
            requestBox.getSelectionModel().clearSelection();
            matchTable.getItems().clear();
            requestInfo.setText("");
        } catch (SQLException e) {
            statusLabel.setText("Database error: " + e.getMessage());
        }
    }

    /** Fills the table with ranked batches for the chosen request. */
    private void showMatches() {
        BuyerRequest req = requestBox.getValue();
        matchTable.getItems().clear();
        currentRequest = null;
        if (req == null) {
            requestInfo.setText("");
            return;
        }
        try {
            double needed = req.quantity() - Repository.reservedForRequest(req.id());
            currentRequest = new BuyerRequest(req.id(), req.buyerId(), req.cropId(), req.cropName(),
                    needed, req.maxPricePerKg(), req.status());

            Buyer buyer = buyers.get(req.buyerId());
            List<MatchResult> results = Matcher.rank(currentRequest, buyer,
                    Repository.availableBatches(), farmers);
            matchTable.setItems(FXCollections.observableArrayList(results));

            String limit = req.maxPricePerKg() == null
                    ? "no price limit" : String.format("max P%.2f/kg", req.maxPricePerKg());
            requestInfo.setText(String.format("%s still needs %.0f kg of %s (%s). %d matching batch(es).",
                    buyer.name(), needed, req.cropName(), limit, results.size()));
            statusLabel.setText("");
        } catch (SQLException e) {
            statusLabel.setText("Database error: " + e.getMessage());
        }
    }

    /** Called when the Reserve button is clicked. */
    @FXML
    private void onReserve() {
        MatchResult m = matchTable.getSelectionModel().getSelectedItem();
        if (m == null || currentRequest == null) return;

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                String.format("Reserve %.0f kg from %s (batch #%d) at P%.2f/kg for request #%d?",
                        m.suggestedQty(), m.farmer().name(), m.batch().batchId(),
                        m.batch().pricePerKg(), currentRequest.id()),
                ButtonType.OK, ButtonType.CANCEL);
        confirm.setHeaderText("Confirm reservation");
        Optional<ButtonType> answer = confirm.showAndWait();
        if (answer.isEmpty() || answer.get() != ButtonType.OK) return;

        try {
            int id = ReservationService.reserve(currentRequest.id(), m.batch().batchId(),
                    m.suggestedQty(), m.batch().pricePerKg(), m.distanceKm(), m.total());
            loadData();                                   // refresh: the request may now be fulfilled
            statusLabel.setText("Reservation #" + id + " created.");
        } catch (IllegalStateException e) {
            statusLabel.setText("Cannot reserve: " + e.getMessage());
        } catch (SQLException e) {
            statusLabel.setText("Database error: " + e.getMessage());
        }
    }
}