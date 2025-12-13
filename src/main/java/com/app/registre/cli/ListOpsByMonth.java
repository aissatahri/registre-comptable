package com.app.registre.cli;

import com.app.registre.dao.Database;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class ListOpsByMonth {
    public static void main(String[] args) {
        int year = args.length > 0 ? Integer.parseInt(args[0]) : 2022;
        int month = args.length > 1 ? Integer.parseInt(args[1]) : 1;

        String dateExpr = "CASE WHEN typeof(date_emission) = 'integer' "
            + "THEN datetime(date_emission/1000, 'unixepoch', 'localtime') "
            + "ELSE date_emission END";

        String sql = "SELECT id, date_emission, solde, " + dateExpr + " as dstr "
                + "FROM operations WHERE strftime('%Y', " + dateExpr + ") = ? "
                + "AND strftime('%m', " + dateExpr + ") = printf('%02d', ?) "
                + "ORDER BY " + dateExpr + " ASC, id ASC";

        try (Connection conn = Database.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, String.valueOf(year));
            pstmt.setInt(2, month);
            try (ResultSet rs = pstmt.executeQuery()) {
                boolean any = false;
                System.out.printf("Operations for %d-%02d:\n", year, month);
                while (rs.next()) {
                    any = true;
                    int id = rs.getInt("id");
                    String de = rs.getString("date_emission");
                    String dstr = rs.getString("dstr");
                    String sol = rs.getString("solde");
                    System.out.printf("id=%d  date_emission=%s  d=%s  solde=%s%n", id, de, dstr, sol);
                }
                if (!any) System.out.println("(no operations)");
            }
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(2);
        }
    }
}
