package DBConnect;

import java.sql.Connection;
import java.sql.DriverManager;

public class DBConnect {

    public static Connection getConnection() {
        Connection con = null;

        try {
            // Load driver
            Class.forName("com.mysql.cj.jdbc.Driver");

            // Try to connect
            String url = "jdbc:mysql://localhost:3306/phpmyadmin?useSSL=false&serverTimezone=UTC";
            String user = "root";
            String pass = "";  // যদি পাসওয়ার্ড থাকে এখানে দিন

            con = DriverManager.getConnection(url, user, pass);

            // Success message (console এ দেখাবে)
            System.out.println("✔ Database connected successfully!");

        } catch (Exception e) {

            // Error হলে console এ দেখাবে
            System.out.println("✘ Database connection failed!");
            e.printStackTrace();
        }

        return con;
    }

    public static void main(String[] args) {

        // Test connection
        Connection testCon = DBConnect.getConnection();

        if (testCon != null) {
            System.out.println("✔ Test: Connection object is NOT null.");
        } else {
            System.out.println("✘ Test: Connection object is NULL.");
        }
    }
}