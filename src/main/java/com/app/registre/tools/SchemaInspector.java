package com.app.registre.tools;

import com.app.registre.dao.Database;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

public class SchemaInspector {
    public static void main(String[] args) {
        try (Connection conn = Database.getInstance().getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("PRAGMA table_info('operations')")) {

            System.out.println("Columns in 'operations' table:");
            while (rs.next()) {
                String name = rs.getString("name");
                String type = rs.getString("type");
                System.out.printf("- %s (%s)%n", name, type);
            }

        } catch (Exception e) {
            e.printStackTrace();
            System.exit(2);
        }
    }
}
