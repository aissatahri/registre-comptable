package com.app.registre.util;

import com.app.registre.model.Operation;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.*;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class ExcelUtil {

    public static void exportOperationsToExcel(List<Operation> operations, String filePath) throws IOException {
        exportOperationsToExcel(operations, filePath, null);
    }

    public static void exportOperationsToExcel(List<Operation> operations, String filePath, Double prevSolde) throws IOException {
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Operations");

            // Style pour l'en-tête
            CellStyle headerStyle = createHeaderStyle(workbook);

            int headerRowIndex = 0;
            // If prevSolde provided, write an info row above header
            if (prevSolde != null) {
                Row info = sheet.createRow(0);
                info.createCell(0).setCellValue("solde_precedent");
                info.createCell(1).setCellValue(prevSolde);
                headerRowIndex = 1;
            }
            // Créer l'en-tête
            Row headerRow = sheet.createRow(headerRowIndex);
                        // Export only these fields in this exact order as requested by user
                        String[] headers = {"imp", "designation", "nature", "n", "budg", "exercice", "beneficiaire",
                            "date_emission", "date_visa", "op_or", "ov_cheq_type", "ov_cheq", "recette",
                            "sur_ram", "sur_eng", "depense", "solde"};

            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            // Remplir les données
            int rowNum = headerRowIndex + 1;
            for (Operation op : operations) {
                Row row = sheet.createRow(rowNum++);

                // Write only the requested fields in the exact order
                row.createCell(0).setCellValue(op.getImp() != null ? op.getImp() : "");
                row.createCell(1).setCellValue(op.getDesignation() != null ? op.getDesignation() : "");
                row.createCell(2).setCellValue(op.getNature() != null ? op.getNature() : "");
                row.createCell(3).setCellValue(op.getN() != null ? op.getN() : "");
                row.createCell(4).setCellValue(op.getBudg() != null ? op.getBudg() : "");
                row.createCell(5).setCellValue(op.getExercice() != null ? op.getExercice() : "");
                row.createCell(6).setCellValue(op.getBeneficiaire() != null ? op.getBeneficiaire() : "");
                row.createCell(7).setCellValue(op.getDateEmission() != null ? op.getDateEmission().toString() : "");
                row.createCell(8).setCellValue(op.getDateVisa() != null ? op.getDateVisa().toString() : "");
                row.createCell(9).setCellValue(op.getOpOr() != null ? op.getOpOr() : 0);
                row.createCell(10).setCellValue(op.getOvCheqType() != null ? op.getOvCheqType() : "");
                row.createCell(11).setCellValue(op.getOvCheq() != null ? op.getOvCheq() : 0);
                row.createCell(12).setCellValue(op.getRecette() != null ? op.getRecette() : 0.0);
                row.createCell(13).setCellValue(op.getSurRam() != null ? op.getSurRam() : 0.0);
                row.createCell(14).setCellValue(op.getSurEng() != null ? op.getSurEng() : 0.0);
                row.createCell(15).setCellValue(op.getDepense() != null ? op.getDepense() : 0.0);
                row.createCell(16).setCellValue(op.getSolde() != null ? op.getSolde() : 0.0);
            }

            // Ajuster automatiquement la largeur des colonnes
            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
            }

            // Écrire le fichier
            try (FileOutputStream fileOut = new FileOutputStream(filePath)) {
                workbook.write(fileOut);
            }
        }
    }

    public static List<Operation> importOperationsFromExcel(String filePath) throws IOException {
        List<Operation> operations = new java.util.ArrayList<>();

        Workbook wb = null;
        File src = new File(filePath);
        try {
            wb = WorkbookFactory.create(src);
        } catch (IOException first) {
            File tmp = File.createTempFile("import-", ".xlsx");
            try {
                try (InputStream in = new FileInputStream(src); OutputStream out = new FileOutputStream(tmp)) {
                    in.transferTo(out);
                }
                wb = WorkbookFactory.create(tmp);
            } catch (IOException second) {
                if (wb != null) {
                    try { wb.close(); } catch (Exception ignore) {}
                }
                throw new IOException("Le fichier Excel semble être ouvert par un autre programme. Fermez-le puis réessayez.", second);
            } finally {
                tmp.deleteOnExit();
            }
        }

        try (Workbook workbook = wb) {
            Sheet sheet = workbook.getSheetAt(0);

            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row != null) {
                    Operation operation = new Operation();
                    // Read according to requested export order
                    operation.setImp(getCellStringValue(row.getCell(0)));
                    operation.setDesignation(getCellStringValue(row.getCell(1)));
                    operation.setNature(getCellStringValue(row.getCell(2)));
                    operation.setN(getCellStringValue(row.getCell(3)));
                    operation.setBudg(getCellStringValue(row.getCell(4)));
                    operation.setExercice(getCellStringValue(row.getCell(5)));
                    operation.setBeneficiaire(getCellStringValue(row.getCell(6)));
                    operation.setDateEmission(parseDate(getCellStringValue(row.getCell(7))));
                    operation.setDateVisa(parseDate(getCellStringValue(row.getCell(8))));
                    // OP/OR
                    String opOrStr = getCellStringValue(row.getCell(9));
                    try { operation.setOpOr(opOrStr == null || opOrStr.isBlank() ? null : Integer.parseInt(opOrStr)); } catch (NumberFormatException ex) { operation.setOpOr(null); }
                    // OV/CHEQ type and value
                    operation.setOvCheqType(getCellStringValue(row.getCell(10)));
                    String ovVal = getCellStringValue(row.getCell(11));
                    try { operation.setOvCheq(ovVal == null || ovVal.isBlank() ? null : Integer.parseInt(ovVal)); } catch (NumberFormatException ex) { operation.setOvCheq(null); }
                    // Numeric columns: recette (12), sur_ram (13), sur_eng (14), depense (15), solde (16)
                    operation.setRecette(getCellNumericValue(row.getCell(12)));
                    operation.setSurRam(getCellNumericValue(row.getCell(13)));
                    operation.setSurEng(getCellNumericValue(row.getCell(14)));
                    operation.setDepense(getCellNumericValue(row.getCell(15)));
                    double sol = getCellNumericValue(row.getCell(16));
                    operation.setSolde(sol);
                    if ((operation.getMois() == null || operation.getMois().isBlank()) && operation.getDateEntree() != null) {
                        operation.setMois(toFrenchMonth(operation.getDateEntree()));
                    }

                    operations.add(operation);
                }
            }
        }

        return operations;
    }

    private static CellStyle createHeaderStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        font.setColor(IndexedColors.WHITE.getIndex());
        style.setFont(font);
        style.setFillForegroundColor(IndexedColors.DARK_BLUE.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        return style;
    }

    private static String getCellStringValue(Cell cell) {
        if (cell == null) return "";

        switch (cell.getCellType()) {
            case STRING:
                return cell.getStringCellValue();
            case NUMERIC:
                if (DateUtil.isCellDateFormatted(cell)) {
                    LocalDate ld = cell.getDateCellValue().toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
                    return ld.toString();
                } else {
                    return String.valueOf(cell.getNumericCellValue());
                }
            case BOOLEAN:
                return String.valueOf(cell.getBooleanCellValue());
            default:
                return "";
        }
    }

    private static double getCellNumericValue(Cell cell) {
        if (cell == null) return 0.0;

        switch (cell.getCellType()) {
            case NUMERIC:
                return cell.getNumericCellValue();
            case STRING:
                try {
                    return Double.parseDouble(cell.getStringCellValue());
                } catch (NumberFormatException e) {
                    return 0.0;
                }
            default:
                return 0.0;
        }
    }

    private static LocalDate parseDate(String dateString) {
        if (dateString == null || dateString.isEmpty()) {
            return null;
        }
        try {
            return LocalDate.parse(dateString);
        } catch (Exception e) {
            try {
                DateTimeFormatter fr = DateTimeFormatter.ofPattern("dd/MM/yyyy");
                return LocalDate.parse(dateString, fr);
            } catch (Exception ex) {
                try {
                    DateTimeFormatter fr2 = DateTimeFormatter.ofPattern("dd-MM-yyyy");
                    return LocalDate.parse(dateString, fr2);
                } catch (Exception ex2) {
                    return null;
                }
            }
        }
    }

    private static String toFrenchMonth(LocalDate date) {
        switch (date.getMonth()) {
            case JANUARY: return "JANVIER";
            case FEBRUARY: return "FEVRIER";
            case MARCH: return "MARS";
            case APRIL: return "AVRIL";
            case MAY: return "MAI";
            case JUNE: return "JUIN";
            case JULY: return "JUILLET";
            case AUGUST: return "AOUT";
            case SEPTEMBER: return "SEPTEMBRE";
            case OCTOBER: return "OCTOBRE";
            case NOVEMBER: return "NOVEMBRE";
            case DECEMBER: return "DECEMBRE";
            default: return "";
        }
    }
}
