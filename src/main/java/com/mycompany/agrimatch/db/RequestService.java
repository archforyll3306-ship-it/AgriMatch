package com.mycompany.agrimatch.db;

import java.math.BigDecimal;
import java.sql.*;

public class RequestService {

    /** Adds an OPEN buyer request. maxPricePerKg may be null (no price limit). Returns request_id. */
    public static int addRequest(int buyerId, int cropId, double quantity,
                                 Double maxPricePerKg) throws SQLException {
        if (quantity <= 0)
            throw new IllegalStateException("Quantity must be more than 0.");
        if (maxPricePerKg != null && maxPricePerKg < 0)
            throw new IllegalStateException("Max price cannot be negative.");

        try (Connection c = DBConnection.get()) {
            requireExists(c, "SELECT 1 FROM buyers WHERE buyer_id = ?", buyerId, "Buyer not found.");
            requireExists(c, "SELECT 1 FROM crops WHERE crop_id = ?", cropId, "Crop not found.");

            String sql = "INSERT INTO buyer_requests (buyer_id, crop_id, requested_quantity, "
                       + "max_price_per_kg, status) VALUES (?,?,?,?, 'OPEN')";
            try (PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                ps.setInt(1, buyerId);
                ps.setInt(2, cropId);
                ps.setBigDecimal(3, BigDecimal.valueOf(quantity));
                if (maxPricePerKg == null) ps.setNull(4, Types.DECIMAL);
                else ps.setBigDecimal(4, BigDecimal.valueOf(maxPricePerKg));
                ps.executeUpdate();
                try (ResultSet k = ps.getGeneratedKeys()) {
                    k.next();
                    return k.getInt(1);
                }
            }
        }
    }

    /** Cancels a request that has no active reservations (PENDING or CONFIRMED). */
    public static void cancelRequest(int requestId) throws SQLException {
        try (Connection c = DBConnection.get()) {
            c.setAutoCommit(false);
            try {
                String status;
                try (PreparedStatement ps = c.prepareStatement(
                        "SELECT status FROM buyer_requests WHERE request_id = ? FOR UPDATE")) {
                    ps.setInt(1, requestId);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (!rs.next()) throw new IllegalStateException("Request not found.");
                        status = rs.getString(1);
                    }
                }
                if (status.equals("CANCELLED")) throw new IllegalStateException("Request is already cancelled.");
                if (status.equals("FULFILLED"))  throw new IllegalStateException("A fulfilled request cannot be cancelled.");

                try (PreparedStatement ps = c.prepareStatement(
                        "SELECT COUNT(*) FROM reservations WHERE request_id = ? "
                      + "AND status IN ('PENDING','CONFIRMED')")) {
                    ps.setInt(1, requestId);
                    try (ResultSet rs = ps.executeQuery()) {
                        rs.next();
                        if (rs.getInt(1) > 0)
                            throw new IllegalStateException(
                                "Cancel or complete this request's active reservations first.");
                    }
                }
                try (PreparedStatement ps = c.prepareStatement(
                        "UPDATE buyer_requests SET status = 'CANCELLED' WHERE request_id = ?")) {
                    ps.setInt(1, requestId);
                    ps.executeUpdate();
                }
                c.commit();
            } catch (SQLException | RuntimeException e) {
                c.rollback();
                throw e;
            }
        }
    }

    private static void requireExists(Connection c, String sql, int id, String message) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) throw new IllegalStateException(message);
            }
        }
    }
}