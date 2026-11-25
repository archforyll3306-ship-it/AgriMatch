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

    public static List<Barangay> barangays() throws SQLException {
        List<Barangay> out = new ArrayList<>();
        try (Connection c = DBConnection.get();
             ResultSet rs = c.createStatement().executeQuery("SELECT * FROM barangays ORDER BY name")) {
            while (rs.next()) out.add(new Barangay(rs.getInt("barangay_id"), rs.getString("name"),
                    rs.getDouble("latitude"), rs.getDouble("longitude")));
        }
        return out;
    }

    public static List<Crop> crops() throws SQLException {
        List<Crop> out = new ArrayList<>();
        try (Connection c = DBConnection.get();
             ResultSet rs = c.createStatement().executeQuery("SELECT * FROM crops ORDER BY crop_name")) {
            while (rs.next()) out.add(new Crop(rs.getInt("crop_id"), rs.getString("crop_name"),
                    rs.getInt("shelf_life_days")));
        }
        return out;
    }

    // ---- list views for the Batches / Requests / Reservations screens ----

    public static List<BatchRow> batchRows() throws SQLException {
        List<BatchRow> out = new ArrayList<>();
        String sql = "SELECT b.batch_id, f.full_name, c.crop_name, b.quantity, b.price_per_kg, b.harvest_date, b.status "
                   + "FROM crop_batches b JOIN farmers f USING(farmer_id) JOIN crops c USING(crop_id) "
                   + "ORDER BY b.batch_id DESC";
        try (Connection c = DBConnection.get(); ResultSet rs = c.createStatement().executeQuery(sql)) {
            while (rs.next()) out.add(new BatchRow(rs.getInt(1), rs.getString(2), rs.getString(3),
                    rs.getDouble(4), rs.getDouble(5), rs.getDate(6).toLocalDate(), rs.getString(7)));
        }
        return out;
    }

    public static List<RequestRow> requestRows() throws SQLException {
        List<RequestRow> out = new ArrayList<>();
        String sql = "SELECT r.request_id, b.full_name, c.crop_name, r.requested_quantity, r.max_price_per_kg, r.status, "
                   + "COALESCE((SELECT SUM(x.reserved_quantity) FROM reservations x WHERE x.request_id=r.request_id "
                   + "AND x.status IN ('PENDING','CONFIRMED','COMPLETED')),0) AS reserved "
                   + "FROM buyer_requests r JOIN buyers b USING(buyer_id) JOIN crops c USING(crop_id) "
                   + "ORDER BY r.request_id DESC";
        try (Connection c = DBConnection.get(); ResultSet rs = c.createStatement().executeQuery(sql)) {
            while (rs.next()) out.add(new RequestRow(rs.getInt(1), rs.getString(2), rs.getString(3),
                    rs.getDouble(4), dbl(rs, "max_price_per_kg"), rs.getString(6), rs.getDouble(7)));
        }
        return out;
    }

    public static List<ReservationRow> reservationRows() throws SQLException {
        List<ReservationRow> out = new ArrayList<>();
        String sql = "SELECT v.reservation_id, v.request_id, v.batch_id, bu.full_name AS buyer, f.full_name AS farmer, "
                   + "c.crop_name, v.reserved_quantity, v.agreed_price_per_kg, v.distance_km, v.status "
                   + "FROM reservations v JOIN buyer_requests r USING(request_id) JOIN buyers bu USING(buyer_id) "
                   + "JOIN crop_batches b ON b.batch_id=v.batch_id JOIN farmers f ON f.farmer_id=b.farmer_id "
                   + "JOIN crops c ON c.crop_id=b.crop_id ORDER BY v.reservation_id DESC";
        try (Connection c = DBConnection.get(); ResultSet rs = c.createStatement().executeQuery(sql)) {
            while (rs.next()) out.add(new ReservationRow(rs.getInt(1), rs.getInt(2), rs.getInt(3),
                    rs.getString(4), rs.getString(5), rs.getString(6), rs.getDouble(7), rs.getDouble(8),
                    dbl(rs, "distance_km"), rs.getString(10)));
        }
        return out;
    }
}