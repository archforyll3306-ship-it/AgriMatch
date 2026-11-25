package com.mycompany.agrimatch.db;

import java.sql.*;
import java.time.LocalDate;

public class BatchService {

    /** Adds a harvested batch and lists it right away. Returns the new batch_id. */
    public static int addBatch(int farmerId, int cropId, double quantity,
                               double pricePerKg, LocalDate harvestDate) throws SQLException {
        if (quantity <= 0)    throw new IllegalStateException("Quantity must be more than 0.");
        if (pricePerKg < 0)   throw new IllegalStateException("Price cannot be negative.");
        if (harvestDate == null) throw new IllegalStateException("Harvest date is required.");
        if (harvestDate.isAfter(LocalDate.now()))
            throw new IllegalStateException("Harvest date cannot be in the future.");

        try (Connection c = DBConnection.get()) {
            int shelfLife = lookupShelfLife(c, cropId);          // also proves the crop exists
            requireExists(c, "SELECT 1 FROM farmers WHERE farmer_id = ?", farmerId, "Farmer not found.");

            if (harvestDate.plusDays(shelfLife).isBefore(LocalDate.now()))
                throw new IllegalStateException("This batch is already past its shelf life.");

            String sql = "INSERT INTO crop_batches (farmer_id, crop_id, quantity, price_per_kg, "
                       + "harvest_date, status) VALUES (?,?,?,?,?, 'LISTED')";
            try (PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                ps.setInt(1, farmerId);
                ps.setInt(2, cropId);
                ps.setBigDecimal(3, java.math.BigDecimal.valueOf(quantity));
                ps.setBigDecimal(4, java.math.BigDecimal.valueOf(pricePerKg));
                ps.setDate(5, Date.valueOf(harvestDate));
                ps.executeUpdate();
                try (ResultSet k = ps.getGeneratedKeys()) {
                    k.next();
                    return k.getInt(1);
                }
            }
        }
    }

    private static int lookupShelfLife(Connection c, int cropId) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(
                "SELECT shelf_life_days FROM crops WHERE crop_id = ?")) {
            ps.setInt(1, cropId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) throw new IllegalStateException("Crop not found.");
                return rs.getInt(1);
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