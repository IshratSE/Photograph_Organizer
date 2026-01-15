
package com.mycompany.photographytagger;

import java.sql.*;

public class connectjava {
    
    public static Connection connectDB() {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver"); 
            Connection conn = DriverManager.getConnection(
                "jdbc:mysql://localhost:3306/first", "root", ""
            );
            System.out.println("Connection successful");
            return conn;
        } catch (Exception e) {
            System.out.println("Connection failed: " + e.getMessage());
            return null;
        }
    }

    public static void main(String[] args) {
        Connection con = connectDB();
        if (con != null) {
            System.out.println("You are connected!");
        } else {
            System.out.println("Connection error!");
        }
    }
}


    

