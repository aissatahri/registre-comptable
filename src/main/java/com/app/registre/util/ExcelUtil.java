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
                            "sur_ram", "sur_eng", "depense", "solde", "montant", "decision", "motif_rejet", "date_reponse", "contenu_reponse", "mois"};

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
                // Use legacy `op` as first column (request uses OP identifier)
                row.createCell(0).setCellValue(op.getOp() != null ? op.getOp() : "");
                row.createCell(1).setCellValue(op.getDesignation() != null ? op.getDesignation() : "");
                row.createCell(2).setCellValue(op.getNature() != null ? op.getNature() : "");
                row.createCell(3).setCellValue(op.getN() != null ? op.getN() : "");
                row.createCell(4).setCellValue(op.getBudg() != null ? op.getBudg() : "");
                row.createCell(5).setCellValue(op.getExercice() != null ? op.getExercice() : "");
                row.createCell(6).setCellValue(op.getBeneficiaire() != null ? op.getBeneficiaire() : "");
                // Prefer dateEmission, then dateEntree, then dateVisa for export
                java.time.LocalDate de = op.getDateEmission() != null ? op.getDateEmission() : op.getDateEntree();
                row.createCell(7).setCellValue(de != null ? de.toString() : "");
                row.createCell(8).setCellValue(op.getDateVisa() != null ? op.getDateVisa().toString() : "");
                row.createCell(9).setCellValue(op.getOpOr() != null ? op.getOpOr() : 0);
                row.createCell(10).setCellValue(op.getOvCheqType() != null ? op.getOvCheqType() : "");
                row.createCell(11).setCellValue(op.getOvCheq() != null ? op.getOvCheq() : 0);
                row.createCell(12).setCellValue(op.getRecette() != null ? op.getRecette() : 0.0);
                row.createCell(13).setCellValue(op.getSurRam() != null ? op.getSurRam() : 0.0);
                row.createCell(14).setCellValue(op.getSurEng() != null ? op.getSurEng() : 0.0);
                row.createCell(15).setCellValue(op.getDepense() != null ? op.getDepense() : 0.0);
                row.createCell(16).setCellValue(op.getSolde() != null ? op.getSolde() : 0.0);
                // getMontant() returns a primitive double (backwards-compatible), so write it directly
                row.createCell(17).setCellValue(op.getMontant());
                row.createCell(18).setCellValue(op.getDecision() != null ? op.getDecision() : "");
                row.createCell(19).setCellValue(op.getMotifRejet() != null ? op.getMotifRejet() : "");
                row.createCell(20).setCellValue(op.getDateReponse() != null ? op.getDateReponse().toString() : "");
                row.createCell(21).setCellValue(op.getContenuReponse() != null ? op.getContenuReponse() : "");
                row.createCell(22).setCellValue(op.getMois() != null ? op.getMois() : "");
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
                    // First column contains legacy `op` identifier
                    operation.setOp(getCellStringValue(row.getCell(0)));
                    // For backward compatibility, also set `imp` from second column if available
                    operation.setImp(getCellStringValue(row.getCell(0)));
                    operation.setDesignation(getCellStringValue(row.getCell(1)));
                    operation.setNature(getCellStringValue(row.getCell(2)));
                    operation.setN(getCellStringValue(row.getCell(3)));
                    operation.setBudg(getCellStringValue(row.getCell(4)));
                    operation.setExercice(getCellStringValue(row.getCell(5)));
                    operation.setBeneficiaire(getCellStringValue(row.getCell(6)));
                    // The export writes date_emission (or date_entree) into column 7.
                    // Populate both dateEmission and dateEntree from that column so downstream
                    // logic (mois calculation, recap grouping) has a consistent date to use.
                    java.time.LocalDate parsedCol7 = parseDate(getCellStringValue(row.getCell(7)));
                    operation.setDateEmission(parsedCol7);
                    // If the import contains an explicit date_entree, prefer it, otherwise mirror the exported date
                    operation.setDateEntree(parsedCol7);
                    operation.setDateVisa(parseDate(getCellStringValue(row.getCell(8))));
                    // OP/OR
                    String opOrStr = getCellStringValue(row.getCell(9));
                    try { operation.setOpOr(opOrStr == null || opOrStr.isBlank() ? null : Integer.parseInt(opOrStr)); } catch (NumberFormatException ex) { operation.setOpOr(null); }
                    // OV/CHEQ type and value
                    operation.setOvCheqType(getCellStringValue(row.getCell(10)));
                    String ovVal = getCellStringValue(row.getCell(11));
                    try { operation.setOvCheq(ovVal == null || ovVal.isBlank() ? null : Integer.parseInt(ovVal)); } catch (NumberFormatException ex) { operation.setOvCheq(null); }
                    // Numeric columns: recette (12), sur_ram (13), sur_eng (14), depense (15), solde (16), montant (17)
                    operation.setRecette(getCellNumericValue(row.getCell(12)));
                    operation.setSurRam(getCellNumericValue(row.getCell(13)));
                    operation.setSurEng(getCellNumericValue(row.getCell(14)));
                    operation.setDepense(getCellNumericValue(row.getCell(15)));
                    double sol = getCellNumericValue(row.getCell(16));
                    operation.setSolde(sol);
                    double montantVal = getCellNumericValue(row.getCell(17));
                    operation.setMontant(montantVal);
                    // additional text fields
                    operation.setDecision(getCellStringValue(row.getCell(18)));
                    operation.setMotifRejet(getCellStringValue(row.getCell(19)));
                    operation.setDateReponse(parseDate(getCellStringValue(row.getCell(20))));
                    operation.setContenuReponse(getCellStringValue(row.getCell(21)));
                    // mois column (22) may already be present; prefer explicit mois if provided
                    String moisCell = getCellStringValue(row.getCell(22));
                    if (moisCell != null && !moisCell.isBlank()) operation.setMois(moisCell);
                    // Ensure mois is set based on date_entree (preferred) or date_emission
                    if (operation.getMois() == null || operation.getMois().isBlank()) {
                        java.time.LocalDate monthSource = operation.getDateEntree() != null ? operation.getDateEntree() : operation.getDateEmission();
                        if (monthSource != null) {
                            operation.setMois(toFrenchMonth(monthSource));
                        }
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
