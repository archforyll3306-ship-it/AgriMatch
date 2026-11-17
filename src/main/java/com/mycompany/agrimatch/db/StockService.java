package com.mycompany.agrimatch.db;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/** Cancelling reservations and marking expired batches as spoiled. */
public final class StockService {

    private static final double EPS = 0.001;

    private StockService() {}

    /** Cancels a PENDING or CONFIRMED reservation and frees the kg back to the batch and request. */
    public static void cancel(int reservationId) throws SQLException {
        try (Connection c = DBConnection.get()) {
            c.setAutoCommit(false);
            try {
                int batchId = cancelInTx(c, reservationId);
                // a fully reserved batch has free kg again, so it can be listed
                try (PreparedStatement ps = c.prepareStatement(
                        "UPDATE crop_batches SET status='LISTED' WHERE batch_id=? AND status='RESERVED'")) {
                    ps.setInt(1, batchId);
                    ps.executeUpdate();
                }
                c.commit();
            } catch (SQLException | RuntimeException e) {
                c.rollback();
                throw e;
            }
        }
    }

    /** Marks every expired LISTED/RESERVED batch as SPOILED. Returns how many were marked. */
    public static int markSpoiled() throws SQLException {
        try (Connection c = DBConnection.get()) {
            c.setAutoCommit(false);
            try {
                // 1. find expired batches (same rule as days_left < 0 in the view)
                List<Integer> expired = new ArrayList<>();
                try (Statement st = c.createStatement();
                     ResultSet rs = st.executeQuery(
                         "SELECT b.batch_id FROM crop_batches b JOIN crops cr ON cr.crop_id=b.crop_id "
                       + "WHERE b.status IN ('LISTED','RESERVED') "
                       + "AND DATE_ADD(b.harvest_date, INTERVAL cr.shelf_life_days DAY) < CURDATE()")) {
                    while (rs.next()) expired.add(rs.getInt(1));
                }

                for (int batchId : expired) {
                    // 2. cancel reservations that were still waiting on this batch
                    List<Integer> open = new ArrayList<>();
                    try (PreparedStatement ps = c.prepareStatement(
                            "SELECT reservation_id FROM reservations WHERE batch_id=? "
                          + "AND status IN ('PENDING','CONFIRMED')")) {
                        ps.setInt(1, batchId);
                        try (ResultSet rs = ps.executeQuery()) { while (rs.next()) open.add(rs.getInt(1)); }
                    }
                    for (int rid : open) cancelInTx(c, rid);

                    // 3. mark the batch
                    try (PreparedStatement ps = c.prepareStatement(
                            "UPDATE crop_batches SET status='SPOILED' WHERE batch_id=?")) {
                        ps.setInt(1, batchId);
                        ps.executeUpdate();
                    }
                }
                c.commit();
                return expired.size();
            } catch (SQLException | RuntimeException e) {
                c.rollback();
                throw e;
            }
        }
    }

    // ---- shared helper: cancels one reservation and fixes the request status. Returns the batch id. ----
    private static int cancelInTx(Connection c, int reservationId) throws SQLException {
        int batchId, requestId; String status;
        try (PreparedStatement ps = c.prepareStatement(
                "SELECT batch_id, request_id, status FROM reservations WHERE reservation_id=? FOR UPDATE")) {
            ps.setInt(1, reservationId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) throw new IllegalStateException("Reservation #" + reservationId + " does not exist.");
                batchId = rs.getInt("batch_id");
                requestId = rs.getInt("request_id");
                status = rs.getString("status");
            }
        }
        if (!status.equals("PENDING") && !status.equals("CONFIRMED"))
            throw new IllegalStateException("Reservation #" + reservationId + " is " + status + " and cannot be cancelled.");

        try (PreparedStatement ps = c.prepareStatement(
                "UPDATE reservations SET status='CANCELLED' WHERE reservation_id=?")) {
            ps.setInt(1, reservationId);
            ps.executeUpdate();
        }

        // recalculate the request's status from what is still reserved
        double reqQty; String reqStatus;
        try (PreparedStatement ps = c.prepareStatement(
                "SELECT requested_quantity, status FROM buyer_requests WHERE request_id=? FOR UPDATE")) {
            ps.setInt(1, requestId);
            try (ResultSet rs = ps.executeQuery()) { rs.next(); reqQty = rs.getDouble(1); reqStatus = rs.getString(2); }
        }
        if (!reqStatus.equals("CANCELLED")) {
            double reserved;
            try (PreparedStatement ps = c.prepareStatement(
                    "SELECT COALESCE(SUM(reserved_quantity),0) FROM reservations WHERE request_id=? "
                  + "AND status IN ('PENDING','CONFIRMED','COMPLETED')")) {
                ps.setInt(1, requestId);
                try (ResultSet rs = ps.executeQuery()) { rs.next(); reserved = rs.getDouble(1); }
            }
            String newStatus = reserved <= EPS ? "OPEN"
                             : (reserved + EPS < reqQty ? "PARTIALLY_FULFILLED" : "FULFILLED");
            try (PreparedStatement ps = c.prepareStatement(
                    "UPDATE buyer_requests SET status=? WHERE request_id=?")) {
                ps.setString(1, newStatus);
                ps.setInt(2, requestId);
                ps.executeUpdate();
            }
        }
        return batchId;
    }
}