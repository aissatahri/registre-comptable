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
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Operations");

            // Style pour l'en-tête
            CellStyle headerStyle = createHeaderStyle(workbook);

            // Créer l'en-tête
            Row headerRow = sheet.createRow(0);
            String[] headers = {"OP", "OV/CHEQ", "IMP", "Nature", "BUDG", "Montant",
                    "Date Entrée", "Date Visa", "Date Rejet", "Décision",
                    "Motif Rejet", "Date Réponse", "Contenu Réponse", "Mois"};

            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            // Remplir les données
            int rowNum = 1;
            for (Operation op : operations) {
                Row row = sheet.createRow(rowNum++);

                row.createCell(0).setCellValue(op.getOp() != null ? op.getOp() : "");
                // OV/CHEQ: prefer showing "TYPE number" if available, otherwise type or number
                String ovCell = "";
                if (op.getOvCheqType() != null && op.getOvCheq() != null) ovCell = op.getOvCheqType() + " " + op.getOvCheq();
                else if (op.getOvCheqType() != null) ovCell = op.getOvCheqType();
                else if (op.getOvCheq() != null) ovCell = String.valueOf(op.getOvCheq());
                row.createCell(1).setCellValue(ovCell);
                row.createCell(2).setCellValue(op.getImp() != null ? op.getImp() : "");
                row.createCell(3).setCellValue(op.getNature() != null ? op.getNature() : "");
                row.createCell(4).setCellValue(op.getBudg() != null ? op.getBudg() : "");
                row.createCell(5).setCellValue(op.getMontant());
                row.createCell(6).setCellValue(op.getDateEntree() != null ? op.getDateEntree().toString() : "");
                row.createCell(7).setCellValue(op.getDateVisa() != null ? op.getDateVisa().toString() : "");
                row.createCell(8).setCellValue(op.getDateRejet() != null ? op.getDateRejet().toString() : "");
                row.createCell(9).setCellValue(op.getDecision() != null ? op.getDecision() : "");
                row.createCell(10).setCellValue(op.getMotifRejet() != null ? op.getMotifRejet() : "");
                row.createCell(11).setCellValue(op.getDateReponse() != null ? op.getDateReponse().toString() : "");
                row.createCell(12).setCellValue(op.getContenuReponse() != null ? op.getContenuReponse() : "");
                row.createCell(13).setCellValue(op.getMois() != null ? op.getMois() : "");
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

                    operation.setOp(getCellStringValue(row.getCell(0)));
                    // Parse OV/CHEQ cell: can be "OV 12345", "OV", or numeric
                    String ovRaw = getCellStringValue(row.getCell(1));
                    if (ovRaw != null && !ovRaw.isBlank()) {
                        String t = ovRaw.trim();
                        String[] parts = t.split("\\s+", 2);
                        if (parts.length == 2) {
                            operation.setOvCheqType(parts[0]);
                            try { operation.setOvCheq(Integer.parseInt(parts[1])); } catch (NumberFormatException ex) { operation.setOvCheq(null); }
                        } else {
                            // single token: could be numeric or type
                            try { operation.setOvCheq(Integer.parseInt(parts[0])); operation.setOvCheqType(null); }
                            catch (NumberFormatException ex) { operation.setOvCheqType(parts[0]); operation.setOvCheq(null); }
                        }
                    }
                    operation.setImp(getCellStringValue(row.getCell(2)));
                    operation.setNature(getCellStringValue(row.getCell(3)));
                    operation.setBudg(getCellStringValue(row.getCell(4)));
                    operation.setMontant(getCellNumericValue(row.getCell(5)));

                    // Dates
                    operation.setDateEntree(parseDate(getCellStringValue(row.getCell(6))));
                    operation.setDateVisa(parseDate(getCellStringValue(row.getCell(7))));
                    operation.setDateRejet(parseDate(getCellStringValue(row.getCell(8))));

                    operation.setDecision(getCellStringValue(row.getCell(9)));
                    operation.setMotifRejet(getCellStringValue(row.getCell(10)));

                    operation.setDateReponse(parseDate(getCellStringValue(row.getCell(11))));
                    operation.setContenuReponse(getCellStringValue(row.getCell(12)));
                    operation.setMois(getCellStringValue(row.getCell(13)));
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
