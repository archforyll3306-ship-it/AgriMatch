/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.mycompany.agrimatch.db;


import com.mycompany.agrimatch.model.SaleRow;
import com.mycompany.agrimatch.model.SpoiledRow;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ReportService {
    
      private ReportService() {}

    /** Every completed sale, newest first. */
    public static List<SaleRow> sales() throws SQLException {
        String sql =
            "SELECT t.transaction_id, t.transaction_date, bu.full_name AS buyer, f.full_name AS farmer, "
          + "       c.crop_name, t.quantity_sold, t.final_price_per_kg, t.total_amount "
          + "FROM transactions t "
          + "JOIN reservations v     ON v.reservation_id = t.reservation_id "
          + "JOIN buyer_requests r   ON r.request_id     = v.request_id "
          + "JOIN buyers bu          ON bu.buyer_id      = r.buyer_id "
          + "JOIN crop_batches b     ON b.batch_id       = v.batch_id "
          + "JOIN farmers f          ON f.farmer_id      = b.farmer_id "
          + "JOIN crops c            ON c.crop_id        = b.crop_id "
          + "ORDER BY t.transaction_date DESC, t.transaction_id DESC";

        List<SaleRow> out = new ArrayList<>();
        try (Connection c = DBConnection.get();
             Statement st = c.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                out.add(new SaleRow(
                        rs.getInt("transaction_id"),
                        rs.getTimestamp("transaction_date").toLocalDateTime(),
                        rs.getString("buyer"),
                        rs.getString("farmer"),
                        rs.getString("crop_name"),
                        rs.getBigDecimal("quantity_sold"),
                        rs.getBigDecimal("final_price_per_kg"),
                        rs.getBigDecimal("total_amount")));
            }
        }
        return out;
    }

    /** Every SPOILED batch, most recently expired first, with the kg that had been sold from it. */
    public static List<SpoiledRow> spoiledBatches() throws SQLException {
        // LEFT JOIN keeps batches that never had any reservation (they would vanish with a plain JOIN).
        // The CASE inside SUM counts only COMPLETED reservations as "sold".
        String sql =
            "SELECT b.batch_id, f.full_name AS farmer, c.crop_name, b.quantity, b.price_per_kg, "
          + "       DATE_ADD(b.harvest_date, INTERVAL c.shelf_life_days DAY) AS expiry_date, "
          + "       COALESCE(SUM(CASE WHEN r.status = 'COMPLETED' THEN r.reserved_quantity END), 0) AS sold_qty "
          + "FROM crop_batches b "
          + "JOIN farmers f ON f.farmer_id = b.farmer_id "
          + "JOIN crops c   ON c.crop_id   = b.crop_id "
          + "LEFT JOIN reservations r ON r.batch_id = b.batch_id "
          + "WHERE b.status = 'SPOILED' "
          + "GROUP BY b.batch_id, f.full_name, c.crop_name, b.quantity, b.price_per_kg, "
          + "         b.harvest_date, c.shelf_life_days "
          + "ORDER BY expiry_date DESC, b.batch_id DESC";

        List<SpoiledRow> out = new ArrayList<>();
        try (Connection c = DBConnection.get();
             Statement st = c.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                out.add(new SpoiledRow(
                        rs.getInt("batch_id"),
                        rs.getString("farmer"),
                        rs.getString("crop_name"),
                        rs.getBigDecimal("quantity"),
                        rs.getBigDecimal("sold_qty"),
                        rs.getBigDecimal("price_per_kg"),
                        rs.getDate("expiry_date").toLocalDate()));
            }
        }
        return out;
    }
}
