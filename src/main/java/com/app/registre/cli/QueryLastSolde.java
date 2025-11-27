package com.app.registre.cli;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

public class QueryLastSolde {
    public static void main(String[] args) {
        String cutoff = args.length > 0 ? args[0] : "2022-03-01";
        String dbFile = "registre.db";
        String url = "jdbc:sqlite:" + dbFile;

        String sql = "SELECT id, COALESCE(date_emission, date_visa) AS d, solde "
                + "FROM operations "
                + "WHERE COALESCE(date_emission, date_visa) < ? "
                + "ORDER BY COALESCE(date_emission, date_visa) DESC LIMIT 1;";

        System.out.println("Querying last operation before: " + cutoff);
        try {
            try {
                Class.forName("org.sqlite.JDBC");
            } catch (ClassNotFoundException e) {
                System.err.println("SQLite JDBC driver not found on classpath: " + e.getMessage());
            }

            try (Connection conn = DriverManager.getConnection(url);
                 PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, cutoff);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    System.out.println("id\tdate\tdate(formatted)\tsolde");
                    long id = rs.getLong("id");
                    Object dObj = rs.getObject("d");
                    String rawDate = dObj == null ? null : dObj.toString();
                    String formatted = null;
                    if (rawDate != null) {
                        // try parse as epoch ms or seconds
                        try {
                            long l = Long.parseLong(rawDate);
                            Instant inst;
                            if (String.valueOf(Math.abs(l)).length() >= 13) {
                                inst = Instant.ofEpochMilli(l);
                            } else {
                                inst = Instant.ofEpochSecond(l);
                            }
                            LocalDate ld = inst.atZone(ZoneId.systemDefault()).toLocalDate();
                            formatted = ld.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
                        } catch (NumberFormatException nfe) {
                            // not a number, try ISO date
                            try {
                                LocalDate ld = LocalDate.parse(rawDate);
                                formatted = ld.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
                            } catch (Exception ex) {
                                // fallback to raw
                                formatted = rawDate;
                            }
                        }
                    }
                    String solde = rs.getString("solde");
                    System.out.printf("%d\t%s\t%s\t%s%n", id, rawDate == null ? "-" : rawDate, formatted == null ? "-" : formatted, solde == null ? "-" : solde);
                } else {
                    System.out.println("No operation found before " + cutoff);
                }
            }

            }
        } catch (SQLException e) {
            System.err.println("Database error: " + e.getMessage());
            e.printStackTrace();
            System.exit(2);
        }
    }
}
