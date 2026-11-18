package com.mycompany.agrimatch.db;

import com.mycompany.agrimatch.model.*;
import java.sql.*;
import java.util.*;

/** Read queries used by the matching engine and UI. */
public final class Repository {
    private Repository() {}

    private static Double dbl(ResultSet rs, String col) throws SQLException {
        double v = rs.getDouble(col);
        return rs.wasNull() ? null : v;
    }

    public static List<Farmer> farmers() throws SQLException {
        List<Farmer> out = new ArrayList<>();
        try (Connection c = DBConnection.get();
             ResultSet rs = c.createStatement().executeQuery("SELECT * FROM farmers ORDER BY farmer_id")) {
            while (rs.next()) out.add(new Farmer(rs.getInt("farmer_id"), rs.getString("full_name"),
                    rs.getString("contact_number"), rs.getString("address"), dbl(rs, "latitude"), dbl(rs, "longitude")));
        }
        return out;
    }

    public static List<Buyer> buyers() throws SQLException {
        List<Buyer> out = new ArrayList<>();
        try (Connection c = DBConnection.get();
             ResultSet rs = c.createStatement().executeQuery("SELECT * FROM buyers ORDER BY buyer_id")) {
            while (rs.next()) out.add(new Buyer(rs.getInt("buyer_id"), rs.getString("full_name"),
                    rs.getString("contact_number"), rs.getString("address"), dbl(rs, "latitude"), dbl(rs, "longitude")));
        }
        return out;
    }

    public static List<AvailableBatch> availableBatches() throws SQLException {
        List<AvailableBatch> out = new ArrayList<>();
        try (Connection c = DBConnection.get();
             ResultSet rs = c.createStatement().executeQuery(
                     "SELECT * FROM v_available_batches WHERE remaining_quantity > 0 AND days_left >= 0 ORDER BY days_left")) {
            while (rs.next()) out.add(new AvailableBatch(rs.getInt("batch_id"), rs.getInt("farmer_id"),
                    rs.getInt("crop_id"), rs.getString("crop_name"), rs.getDouble("price_per_kg"),
                    rs.getDate("harvest_date").toLocalDate(), rs.getDate("expiry_date").toLocalDate(),
                    rs.getInt("days_left"), rs.getDouble("remaining_quantity")));
        }
        return out;
    }

    public static List<BuyerRequest> openRequests() throws SQLException {
        List<BuyerRequest> out = new ArrayList<>();
        String sql = "SELECT r.*, c.crop_name FROM buyer_requests r JOIN crops c USING(crop_id) "
                   + "WHERE r.status IN ('OPEN','PARTIALLY_FULFILLED') ORDER BY r.request_date";
        try (Connection c = DBConnection.get(); ResultSet rs = c.createStatement().executeQuery(sql)) {
            while (rs.next()) out.add(new BuyerRequest(rs.getInt("request_id"), rs.getInt("buyer_id"),
                    rs.getInt("crop_id"), rs.getString("crop_name"), rs.getDouble("requested_quantity"),
                    dbl(rs, "max_price_per_kg"), rs.getString("status")));
        }
        return out;
    }
        /** Kg already held or sold for a request (PENDING, CONFIRMED or COMPLETED reservations). */
    public static double reservedForRequest(int requestId) throws SQLException {
        String sql = "SELECT COALESCE(SUM(reserved_quantity),0) FROM reservations "
                   + "WHERE request_id=? AND status IN ('PENDING','CONFIRMED','COMPLETED')";
        try (Connection c = DBConnection.get(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, requestId);
            try (ResultSet rs = ps.executeQuery()) { rs.next(); return rs.getDouble(1); }
        }
    }
}
