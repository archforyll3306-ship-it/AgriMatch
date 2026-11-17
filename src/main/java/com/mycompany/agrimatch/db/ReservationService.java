package com.mycompany.agrimatch.db;

import java.sql.*;

/** Creates reservations safely: all steps succeed together, or none are saved. */
public final class ReservationService {

    private static final double EPS = 0.001; // tolerance for decimal kg comparisons

    private ReservationService() {}

    /** Returns the new reservation_id. Throws IllegalStateException if a rule is broken. */
    public static int reserve(int requestId, int batchId, double qty, double pricePerKg,
                              Double distanceKm, double matchScore) throws SQLException {
        if (qty <= 0) throw new IllegalStateException("Quantity must be more than 0.");

        try (Connection c = DBConnection.get()) {
            c.setAutoCommit(false);                    // start a transaction
            try {
                // 1. lock the batch row so nobody else can reserve it at the same moment
                double batchQty; int batchCrop; String batchStatus;
                try (PreparedStatement ps = c.prepareStatement(
                        "SELECT quantity, crop_id, status FROM crop_batches WHERE batch_id=? FOR UPDATE")) {
                    ps.setInt(1, batchId);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (!rs.next()) throw new IllegalStateException("Batch #" + batchId + " does not exist.");
                        batchQty = rs.getDouble("quantity");
                        batchCrop = rs.getInt("crop_id");
                        batchStatus = rs.getString("status");
                    }
                }
                if (!batchStatus.equals("LISTED"))
                    throw new IllegalStateException("Batch #" + batchId + " is " + batchStatus + ", not LISTED.");

                // 2. lock the request row
                double reqQty; int reqCrop; String reqStatus;
                try (PreparedStatement ps = c.prepareStatement(
                        "SELECT requested_quantity, crop_id, status FROM buyer_requests WHERE request_id=? FOR UPDATE")) {
                    ps.setInt(1, requestId);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (!rs.next()) throw new IllegalStateException("Request #" + requestId + " does not exist.");
                        reqQty = rs.getDouble("requested_quantity");
                        reqCrop = rs.getInt("crop_id");
                        reqStatus = rs.getString("status");
                    }
                }
                if (!reqStatus.equals("OPEN") && !reqStatus.equals("PARTIALLY_FULFILLED"))
                    throw new IllegalStateException("Request #" + requestId + " is " + reqStatus + ".");
                if (reqCrop != batchCrop)
                    throw new IllegalStateException("Batch and request are for different crops.");

                // 3. how much is already reserved on each side?
                double batchReserved = sumReserved(c, "batch_id", batchId);
                double reqReserved   = sumReserved(c, "request_id", requestId);

                double batchLeft = batchQty - batchReserved;
                double reqLeft   = reqQty - reqReserved;
                if (qty > batchLeft + EPS)
                    throw new IllegalStateException("Only " + batchLeft + " kg left in batch #" + batchId + ".");
                if (qty > reqLeft + EPS)
                    throw new IllegalStateException("Request #" + requestId + " only needs " + reqLeft + " kg more.");

                // 4. save the reservation
                int reservationId;
                try (PreparedStatement ps = c.prepareStatement(
                        "INSERT INTO reservations (request_id, batch_id, reserved_quantity, agreed_price_per_kg, "
                      + "distance_km, match_score) VALUES (?,?,?,?,?,?)", Statement.RETURN_GENERATED_KEYS)) {
                    ps.setInt(1, requestId);
                    ps.setInt(2, batchId);
                    ps.setDouble(3, qty);
                    ps.setDouble(4, pricePerKg);
                    if (distanceKm == null) ps.setNull(5, Types.DECIMAL); else ps.setDouble(5, distanceKm);
                    ps.setDouble(6, matchScore);
                    ps.executeUpdate();
                    try (ResultSet keys = ps.getGeneratedKeys()) {
                        keys.next();
                        reservationId = keys.getInt(1);
                    }
                }

                // 5. update statuses
                if (batchLeft - qty <= EPS)
                    setStatus(c, "UPDATE crop_batches SET status='RESERVED' WHERE batch_id=?", batchId);
                String newReqStatus = (reqLeft - qty <= EPS) ? "FULFILLED" : "PARTIALLY_FULFILLED";
                try (PreparedStatement ps = c.prepareStatement(
                        "UPDATE buyer_requests SET status=? WHERE request_id=?")) {
                    ps.setString(1, newReqStatus);
                    ps.setInt(2, requestId);
                    ps.executeUpdate();
                }

                c.commit();                            // save everything
                return reservationId;

            } catch (SQLException | RuntimeException e) {
                c.rollback();                          // undo everything
                throw e;
            }
        }
    }

    private static double sumReserved(Connection c, String column, int id) throws SQLException {
        String sql = "SELECT COALESCE(SUM(reserved_quantity),0) FROM reservations WHERE " + column
                   + "=? AND status IN ('PENDING','CONFIRMED','COMPLETED')";
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) { rs.next(); return rs.getDouble(1); }
        }
    }

    private static void setStatus(Connection c, String sql, int id) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(sql)) { ps.setInt(1, id); ps.executeUpdate(); }
    }
}