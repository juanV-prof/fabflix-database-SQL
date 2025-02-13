package utils;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class SQLConnector {
    private Connection connection;

    public SQLConnector() {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");

            String url = "jdbc:mysql://localhost:3306/moviedb?autoReconnect=true&allowPublicKeyRetrieval=true&useSSL=false&cachePrepStmts=true";
            String user = "mytestuser";
            String password = "My6$Password";

            connection = DriverManager.getConnection(url, user, password);
            System.out.println("Connected to database.");
        } catch (Exception e) {
            connection = null;
            e.printStackTrace();
        }
    }

    public Connection getConnection() {
        return connection;
    }

    public void closeConnection() {
        try {
            if (connection != null) {
                connection.close();
                System.out.println("Database connection closed.");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        SQLConnector sqlConnector = new SQLConnector();
        Connection conn = sqlConnector.getConnection();

        if (conn != null) {
            System.out.println("Database connection successful!");
        } else {
            System.out.println("Failed to connect to the database.");
        }

        sqlConnector.closeConnection();
    }
}