package common;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DB {
    // Database credentials for local XAMPP MySQL
    private static final String URL = "jdbc:mysql://localhost:3306/quizdb";
    private static final String USER = "root";
    private static final String PASSWORD = "";

    /**
     * Establishes and returns an active connection to the quizdb database.
     * Throws SQLException if connection fails.
     */
    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(URL, USER, PASSWORD);
    }

    /**
     * Test method to verify that database connection works properly.
     */
    public static void main(String[] args) {
        System.out.println("Connecting to database...");
        try (Connection conn = getConnection()) {
            if (conn != null) {
                System.out.println("SUCCESS: Connected to MySQL database quizdb!");
            }
        } catch (SQLException e) {
            System.err.println("FAILED: Could not connect to database.");
            e.printStackTrace();
        }
    }
}