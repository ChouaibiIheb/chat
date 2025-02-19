package com.dhasboard.chat;// Database Connection (DBConnection.java)

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DBConnection {
    private static final String URL = "jdbc:mysql://localhost:3306/chat_app";
    private static final String USER = "root";
    private static final String PASSWORD = "";
    private static Connection instance;

    private static boolean isFirstConnection = true;

    private DBConnection() {}

    public static synchronized Connection getConnection() throws SQLException {
        if (instance == null || instance.isClosed()) {
            try {
                instance = DriverManager.getConnection(URL, USER, PASSWORD);
                if (isFirstConnection) {
                    System.out.println("Database connection established");
                    isFirstConnection = false;
                }
            } catch (SQLException e) {
                System.err.println("Database connection failed: " + e.getMessage());
                throw e;
            }
        }
        return instance;
    }

    public static void closeConnection() {
        try {
            if (instance != null && !instance.isClosed()) {
                instance.close();
                System.out.println("Database connection closed");
            }
        } catch (SQLException e) {
            System.err.println("Error closing connection: " + e.getMessage());
        }
    }

    static {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            DBConnection.closeConnection();
        }));
    }
}