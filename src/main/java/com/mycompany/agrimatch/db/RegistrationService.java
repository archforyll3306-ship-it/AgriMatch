package com.mycompany.agrimatch.db;

import java.sql.*;

/** Adds farmers and buyers. The address and coordinates come from the chosen barangay. */
public final class RegistrationService {

    private RegistrationService() {}

    public static int addFarmer(String name, String contact, int barangayId) throws SQLException {
        return add("farmers", "farmer", name, contact, barangayId);
    }

    public static int addBuyer(String name, String contact, int barangayId) throws SQLException {
        return add("buyers", "buyer", name, contact, barangayId);
    }

    // 'table' is only ever "farmers" or "buyers", passed in by the two methods above, never by a user.
    private static int add(String table, String label, String name, String contact, int barangayId)
            throws SQLException {

        // 1. check what was typed (no database needed yet)
        String cleanName = name == null ? "" : name.trim();
        String cleanContact = contact == null ? "" : contact.replaceAll("[\\s-]", "");
        if (cleanName.isEmpty())
            throw new IllegalStateException("Name is required.");
        if (cleanName.length() > 100)
            throw new IllegalStateException("Name is too long (100 characters at most).");
        if (!cleanContact.matches("\\d{7,15}"))
            throw new IllegalStateException("Contact number must be 7 to 15 digits.");

        try (Connection c = DBConnection.get()) {
            // 2. the barangay gives us the address and the coordinates
            String barangay; double lat, lon;
            try (PreparedStatement ps = c.prepareStatement(
                    "SELECT name, latitude, longitude FROM barangays WHERE barangay_id=?")) {
                ps.setInt(1, barangayId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) throw new IllegalStateException("Please choose a barangay from the list.");
                    barangay = rs.getString("name");
                    lat = rs.getDouble("latitude");
                    lon = rs.getDouble("longitude");
                }
            }

            // 3. the same phone number cannot be registered twice
            try (PreparedStatement ps = c.prepareStatement(
                    "SELECT COUNT(*) FROM " + table + " WHERE contact_number=?")) {
                ps.setString(1, cleanContact);
                try (ResultSet rs = ps.executeQuery()) {
                    rs.next();
                    if (rs.getInt(1) > 0)
                        throw new IllegalStateException("A " + label + " with the number " + cleanContact + " already exists.");
                }
            }

            // 4. save
            String sql = "INSERT INTO " + table
                       + " (full_name, contact_number, address, latitude, longitude) VALUES (?,?,?,?,?)";
            try (PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                ps.setString(1, cleanName);
                ps.setString(2, cleanContact);
                ps.setString(3, barangay + ", Davao City");
                ps.setDouble(4, lat);
                ps.setDouble(5, lon);
                ps.executeUpdate();
                try (ResultSet keys = ps.getGeneratedKeys()) {
                    keys.next();
                    return keys.getInt(1);
                }
            }
        }
    }
}
