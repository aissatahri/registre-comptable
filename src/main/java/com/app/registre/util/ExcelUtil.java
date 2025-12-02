package com.app.registre.util;

import com.app.registre.model.Operation;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.*;

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
        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            XSSFSheet sheet = workbook.createSheet("Operations");

            // Styles
            CellStyle headerStyle = createEnhancedHeaderStyle(workbook);
            CellStyle titleStyle = createTitleStyle(workbook);
            CellStyle dataStyle = createDataStyle(workbook);
            CellStyle numberStyle = createNumberStyle(workbook);
            CellStyle dateStyle = createDateStyle(workbook);
            CellStyle totalStyle = createTotalStyle(workbook);
            CellStyle positiveStyle = createPositiveNumberStyle(workbook);
            CellStyle negativeStyle = createNegativeNumberStyle(workbook);

            int currentRow = 0;
            
            // Titre et logo
            Row titleRow = sheet.createRow(currentRow++);
            Cell titleCell = titleRow.createCell(0);
            titleCell.setCellValue("REGISTRE COMPTABLE - EXPORT");
            titleCell.setCellStyle(titleStyle);
            sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, 10));
            titleRow.setHeightInPoints(30);
            
            // Date d'export
            Row dateRow = sheet.createRow(currentRow++);
            Cell dateCell = dateRow.createCell(0);
            dateCell.setCellValue("Date d'export: " + LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));
            sheet.addMergedRegion(new CellRangeAddress(1, 1, 0, 5));
            
            currentRow++; // Ligne vide
            
            // Solde précédent si fourni
            if (prevSolde != null) {
                Row soldeRow = sheet.createRow(currentRow++);
                Cell labelCell = soldeRow.createCell(0);
                labelCell.setCellValue("Solde précédent:");
                labelCell.setCellStyle(createBoldStyle(workbook));
                Cell valueCell = soldeRow.createCell(1);
                valueCell.setCellValue(prevSolde);
                valueCell.setCellStyle(numberStyle);
                currentRow++; // Ligne vide
            }
            
            // Créer l'en-tête
            Row headerRow = sheet.createRow(currentRow++);
                        // Colonnes selon la structure de la table operations
                        String[] headers = {"IMP", "Désignation", "Nature", "N", "BUDG", "Exercice", "Bénéficiaire",
                            "Date émission", "Date entrée", "Date visa", "Date rejet", "OP/OR", "OV/CHEQ Type", "OV/CHEQ", 
                            "Recette", "SUR-RAM", "SUR-ENG", "Dépense", "Solde"};

            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }
            
            headerRow.setHeightInPoints(25);

            // Remplir les données avec styles conditionnels
            int rowNum = currentRow;
            double totalRecette = 0.0;
            double totalDepense = 0.0;
            int dataStartRow = rowNum;
            
            for (Operation op : operations) {
                Row row = sheet.createRow(rowNum++);
                
                // Colonnes texte
                Cell c0 = row.createCell(0); c0.setCellValue(op.getImp() != null ? op.getImp() : ""); c0.setCellStyle(dataStyle);
                Cell c1 = row.createCell(1); c1.setCellValue(op.getDesignation() != null ? op.getDesignation() : ""); c1.setCellStyle(dataStyle);
                Cell c2 = row.createCell(2); c2.setCellValue(op.getNature() != null ? op.getNature() : ""); c2.setCellStyle(dataStyle);
                Cell c3 = row.createCell(3); c3.setCellValue(op.getN() != null ? op.getN() : ""); c3.setCellStyle(dataStyle);
                Cell c4 = row.createCell(4); c4.setCellValue(op.getBudg() != null ? op.getBudg() : ""); c4.setCellStyle(dataStyle);
                Cell c5 = row.createCell(5); c5.setCellValue(op.getExercice() != null ? op.getExercice() : ""); c5.setCellStyle(dataStyle);
                Cell c6 = row.createCell(6); c6.setCellValue(op.getBeneficiaire() != null ? op.getBeneficiaire() : ""); c6.setCellStyle(dataStyle);
                
                // Dates
                Cell c7 = row.createCell(7);
                c7.setCellValue(op.getDateEmission() != null ? op.getDateEmission().toString() : "");
                c7.setCellStyle(dateStyle);
                
                Cell c8 = row.createCell(8);
                c8.setCellValue(op.getDateEntree() != null ? op.getDateEntree().toString() : "");
                c8.setCellStyle(dateStyle);
                
                Cell c9 = row.createCell(9);
                c9.setCellValue(op.getDateVisa() != null ? op.getDateVisa().toString() : "");
                c9.setCellStyle(dateStyle);
                
                Cell c10 = row.createCell(10);
                c10.setCellValue(op.getDateRejet() != null ? op.getDateRejet().toString() : "");
                c10.setCellStyle(dateStyle);
                
                // Numéros
                Cell c11 = row.createCell(11); c11.setCellValue(op.getOpOr() != null ? op.getOpOr() : 0); c11.setCellStyle(dataStyle);
                Cell c12 = row.createCell(12); c12.setCellValue(op.getOvCheqType() != null ? op.getOvCheqType() : ""); c12.setCellStyle(dataStyle);
                Cell c13 = row.createCell(13); c13.setCellValue(op.getOvCheq() != null ? op.getOvCheq() : 0); c13.setCellStyle(dataStyle);
                
                // Montants avec couleurs conditionnelles
                double recette = op.getRecette() != null ? op.getRecette() : 0.0;
                double depense = op.getDepense() != null ? op.getDepense() : 0.0;
                double solde = op.getSolde() != null ? op.getSolde() : 0.0;
                
                totalRecette += recette;
                totalDepense += depense;
                
                Cell c14 = row.createCell(14); 
                c14.setCellValue(recette); 
                c14.setCellStyle(recette > 0 ? positiveStyle : numberStyle);
                
                Cell c15 = row.createCell(15); 
                c15.setCellValue(op.getSurRam() != null ? op.getSurRam() : 0.0); 
                c15.setCellStyle(numberStyle);
                
                Cell c16 = row.createCell(16); 
                c16.setCellValue(op.getSurEng() != null ? op.getSurEng() : 0.0); 
                c16.setCellStyle(numberStyle);
                
                Cell c17 = row.createCell(17); 
                c17.setCellValue(depense); 
                c17.setCellStyle(depense > 0 ? negativeStyle : numberStyle);
                
                Cell c18 = row.createCell(18); 
                c18.setCellValue(solde); 
                c18.setCellStyle(solde >= 0 ? positiveStyle : negativeStyle);
            }
            
            // Ligne de totaux
            Row totalRow = sheet.createRow(rowNum++);
            totalRow.setHeightInPoints(20);
            Cell labelTotal = totalRow.createCell(0);
            labelTotal.setCellValue("TOTAUX");
            labelTotal.setCellStyle(totalStyle);
            sheet.addMergedRegion(new CellRangeAddress(rowNum - 1, rowNum - 1, 0, 13));
            
            Cell totalRecetteCell = totalRow.createCell(14);
            totalRecetteCell.setCellValue(totalRecette);
            totalRecetteCell.setCellStyle(totalStyle);
            
            totalRow.createCell(15).setCellStyle(totalStyle);
            totalRow.createCell(16).setCellStyle(totalStyle);
            
            Cell totalDepenseCell = totalRow.createCell(17);
            totalDepenseCell.setCellValue(totalDepense);
            totalDepenseCell.setCellStyle(totalStyle);
            
            Cell totalSoldeCell = totalRow.createCell(18);
            totalSoldeCell.setCellValue(totalRecette - totalDepense);
            totalSoldeCell.setCellStyle(totalStyle);

            // Ajuster automatiquement la largeur des colonnes
            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
                // Largeur minimum
                if (sheet.getColumnWidth(i) < 2000) {
                    sheet.setColumnWidth(i, 2000);
                }
            }
            
            // Activer les filtres sur la ligne d'en-tête
            sheet.setAutoFilter(new CellRangeAddress(currentRow - 1, currentRow - 1, 0, headers.length - 1));

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
                    // Read according to export order:
                    // {"imp", "designation", "nature", "n", "budg", "exercice", "beneficiaire",
                    //  "date_emission", "date_visa", "op_or", "ov_cheq_type", "ov_cheq", "recette",
                    //  "sur_ram", "sur_eng", "depense", "solde", "montant", ...}
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
                    Double sol = getCellNumericValue(row.getCell(16));
                    if (sol != null) operation.setSolde(sol);
                    Double montantVal = getCellNumericValue(row.getCell(17));
                    if (montantVal != null) operation.setMontant(montantVal);
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

    private static Double getCellNumericValue(Cell cell) {
        if (cell == null) return null;

        switch (cell.getCellType()) {
            case NUMERIC:
                return cell.getNumericCellValue();
            case STRING:
                String val = cell.getStringCellValue().trim();
                if (val.isEmpty()) return null;
                try {
                    return Double.parseDouble(val);
                } catch (NumberFormatException e) {
                    return null;
                }
            case BLANK:
                return null;
            default:
                return null;
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
    
    // Styles améliorés
    private static CellStyle createTitleStyle(Workbook wb) {
        CellStyle style = wb.createCellStyle();
        Font font = wb.createFont();
        font.setBold(true);
        font.setFontHeightInPoints((short) 18);
        font.setColor(IndexedColors.WHITE.getIndex());
        style.setFont(font);
        style.setFillForegroundColor(IndexedColors.DARK_BLUE.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        return style;
    }
    
    private static CellStyle createEnhancedHeaderStyle(Workbook wb) {
        CellStyle style = wb.createCellStyle();
        Font font = wb.createFont();
        font.setBold(true);
        font.setFontHeightInPoints((short) 12);
        font.setColor(IndexedColors.WHITE.getIndex());
        style.setFont(font);
        style.setFillForegroundColor(IndexedColors.GREY_50_PERCENT.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        style.setBorderTop(BorderStyle.MEDIUM);
        style.setBorderBottom(BorderStyle.MEDIUM);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        style.setWrapText(true);
        return style;
    }
    
    private static CellStyle createDataStyle(Workbook wb) {
        CellStyle style = wb.createCellStyle();
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        return style;
    }
    
    private static CellStyle createNumberStyle(Workbook wb) {
        CellStyle style = createDataStyle(wb);
        style.setDataFormat(wb.createDataFormat().getFormat("#,##0.00"));
        style.setAlignment(HorizontalAlignment.RIGHT);
        return style;
    }
    
    private static CellStyle createDateStyle(Workbook wb) {
        CellStyle style = createDataStyle(wb);
        style.setDataFormat(wb.createDataFormat().getFormat("dd/mm/yyyy"));
        style.setAlignment(HorizontalAlignment.CENTER);
        return style;
    }
    
    private static CellStyle createTotalStyle(Workbook wb) {
        CellStyle style = wb.createCellStyle();
        Font font = wb.createFont();
        font.setBold(true);
        font.setFontHeightInPoints((short) 11);
        style.setFont(font);
        style.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setBorderTop(BorderStyle.MEDIUM);
        style.setBorderBottom(BorderStyle.MEDIUM);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        style.setDataFormat(wb.createDataFormat().getFormat("#,##0.00"));
        style.setAlignment(HorizontalAlignment.RIGHT);
        return style;
    }
    
    private static CellStyle createPositiveNumberStyle(Workbook wb) {
        CellStyle style = createNumberStyle(wb);
        Font font = wb.createFont();
        font.setColor(IndexedColors.GREEN.getIndex());
        style.setFont(font);
        return style;
    }
    
    private static CellStyle createNegativeNumberStyle(Workbook wb) {
        CellStyle style = createNumberStyle(wb);
        Font font = wb.createFont();
        font.setColor(IndexedColors.RED.getIndex());
        style.setFont(font);
        return style;
    }
    
    private static CellStyle createBoldStyle(Workbook wb) {
        CellStyle style = wb.createCellStyle();
        Font font = wb.createFont();
        font.setBold(true);
        style.setFont(font);
        return style;
    }
}

