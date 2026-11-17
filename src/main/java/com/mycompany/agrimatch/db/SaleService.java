package com.mycompany.agrimatch.db;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.*;

/** Moves a reservation through CONFIRMED and COMPLETED, and records the sale. */
public final class SaleService {

    private SaleService() {}

    /** PENDING -> CONFIRMED. */
    public static void confirm(int reservationId) throws SQLException {
        try (Connection c = DBConnection.get();
             PreparedStatement ps = c.prepareStatement(
                     "UPDATE reservations SET status='CONFIRMED' WHERE reservation_id=? AND status='PENDING'")) {
            ps.setInt(1, reservationId);
            if (ps.executeUpdate() == 0)
                throw new IllegalStateException("Reservation #" + reservationId + " is not PENDING (or does not exist).");
        }
    }

    /** CONFIRMED -> COMPLETED and writes the transaction. Returns the new transaction_id. */
    public static int complete(int reservationId, double finalPricePerKg) throws SQLException {
        if (finalPricePerKg < 0) throw new IllegalStateException("Price cannot be negative.");

        try (Connection c = DBConnection.get()) {
            c.setAutoCommit(false);
            try {
                // 1. lock the reservation and read it
                BigDecimal qty; int batchId; String status;
                try (PreparedStatement ps = c.prepareStatement(
                        "SELECT batch_id, reserved_quantity, status FROM reservations WHERE reservation_id=? FOR UPDATE")) {
                    ps.setInt(1, reservationId);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (!rs.next()) throw new IllegalStateException("Reservation #" + reservationId + " does not exist.");
                        batchId = rs.getInt("batch_id");
                        qty = rs.getBigDecimal("reserved_quantity");
                        status = rs.getString("status");
                    }
                }
                if (!status.equals("CONFIRMED"))
                    throw new IllegalStateException("Reservation #" + reservationId + " is " + status + ", not CONFIRMED.");

                // 2. money uses BigDecimal so cents are exact
                BigDecimal price = BigDecimal.valueOf(finalPricePerKg).setScale(2, RoundingMode.HALF_UP);
                BigDecimal total = qty.multiply(price).setScale(2, RoundingMode.HALF_UP);

                // 3. write the transaction
                int transactionId;
                try (PreparedStatement ps = c.prepareStatement(
                        "INSERT INTO transactions (reservation_id, quantity_sold, final_price_per_kg, total_amount) "
                      + "VALUES (?,?,?,?)", Statement.RETURN_GENERATED_KEYS)) {
                    ps.setInt(1, reservationId);
                    ps.setBigDecimal(2, qty);
                    ps.setBigDecimal(3, price);
                    ps.setBigDecimal(4, total);
                    ps.executeUpdate();
                    try (ResultSet keys = ps.getGeneratedKeys()) { keys.next(); transactionId = keys.getInt(1); }
                }

                // 4. mark the reservation completed
                try (PreparedStatement ps = c.prepareStatement(
                        "UPDATE reservations SET status='COMPLETED' WHERE reservation_id=?")) {
                    ps.setInt(1, reservationId);
                    ps.executeUpdate();
                }

                // 5. if the whole batch has now been sold, mark it SOLD
                BigDecimal batchQty;
                try (PreparedStatement ps = c.prepareStatement(
                        "SELECT quantity FROM crop_batches WHERE batch_id=? FOR UPDATE")) {
                    ps.setInt(1, batchId);
                    try (ResultSet rs = ps.executeQuery()) { rs.next(); batchQty = rs.getBigDecimal(1); }
                }
                BigDecimal sold;
                try (PreparedStatement ps = c.prepareStatement(
                        "SELECT COALESCE(SUM(reserved_quantity),0) FROM reservations "
                      + "WHERE batch_id=? AND status='COMPLETED'")) {
                    ps.setInt(1, batchId);
                    try (ResultSet rs = ps.executeQuery()) { rs.next(); sold = rs.getBigDecimal(1); }
                }
                if (sold.compareTo(batchQty) >= 0) {
                    try (PreparedStatement ps = c.prepareStatement(
                            "UPDATE crop_batches SET status='SOLD' WHERE batch_id=?")) {
                        ps.setInt(1, batchId);
                        ps.executeUpdate();
                    }
                }

                c.commit();
                return transactionId;

            } catch (SQLException | RuntimeException e) {
                c.rollback();
                throw e;
            }
        }
    }
}