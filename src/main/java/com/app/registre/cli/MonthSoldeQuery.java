package com.app.registre.cli;

import com.app.registre.dao.OperationDAO;

public class MonthSoldeQuery {
    public static void main(String[] args) {
        int year = args.length > 0 ? Integer.parseInt(args[0]) : 2022;
        int month = args.length > 1 ? Integer.parseInt(args[1]) : 2;
        OperationDAO dao = new OperationDAO();
        try {
            Double prev = dao.getLastMontantForMonthYear(year, month - 1);
            Double curr = dao.getLastMontantForMonthYear(year, month);
            System.out.printf("Year=%d Month=%02d -> prevMonth=%s, current=%s%n", year, month, prev == null ? "null" : String.format("%,.2f", prev), curr == null ? "null" : String.format("%,.2f", curr));
            boolean hasPrev = dao.hasOperationsForMonthYear(year, month - 1);
            boolean hasCurr = dao.hasOperationsForMonthYear(year, month);
            System.out.printf("hasPrev=%b hasCurr=%b%n", hasPrev, hasCurr);
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(2);
        }
    }
}
