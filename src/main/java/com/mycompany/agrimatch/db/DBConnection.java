package com.mycompany.agrimatch.db;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/** Opens connections to the local MariaDB (XAMPP defaults: user root, empty password). */
public final class DBConnection {
    private static final String URL  = "jdbc:mariadb://localhost:3306/agrimatch_db";
    private static final String USER = "root";
    private static final String PASS = "";

    private DBConnection() {}

    public static Connection get() throws SQLException {
        return DriverManager.getConnection(URL, USER, PASS);
    }
}
