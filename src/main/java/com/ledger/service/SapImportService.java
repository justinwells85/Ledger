package com.ledger.service;

import com.ledger.entity.ActualLine;
import com.ledger.entity.SapImport;
import com.ledger.entity.SapImportStatus;
import com.ledger.repository.ActualLineRepository;
import com.ledger.repository.FiscalPeriodRepository;
import com.ledger.repository.SapImportRepository;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.poi.ss.usermodel.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;

/**
 * SAP import pipeline: Upload → Parse → Dedup → Stage.
 * Spec: 05-sap-ingestion.md Steps 1-4, BR-08, BR-09
 */
@Service
@Transactional
public class SapImportService {

    private final SapImportRepository sapImportRepository;
    private final ActualLineRepository actualLineRepository;
    private final FiscalPeriodRepository fiscalPeriodRepository;

    // Column name variants for header auto-detection
    private static final String[] COL_DOC_NUMBER = {"Document Number", "Doc Number", "Document No", "DOCNO"};
    private static final String[] COL_POSTING_DATE = {"Posting Date", "PostingDate", "DATE", "Posting_Date"};
    private static final String[] COL_AMOUNT = {"Amount", "AMOUNT", "Value", "Net Amount"};
    private static final String[] COL_VENDOR = {"Vendor Name", "Vendor", "VENDOR"};
    private static final String[] COL_COST_CENTER = {"Cost Center", "CostCenter", "CCTR"};
    private static final String[] COL_WBSE = {"WBS Element", "WBSE", "WBS", "WBS_Element"};
    private static final String[] COL_GL = {"GL Account", "GL", "G/L Account"};
    private static final String[] COL_DESCRIPTION = {"Description", "Text", "Ref. Doc. Text", "Item Text"};

    public SapImportService(SapImportRepository sapImportRepository,
                             ActualLineRepository actualLineRepository,
                             FiscalPeriodRepository fiscalPeriodRepository) {
        this.sapImportRepository = sapImportRepository;
        this.actualLineRepository = actualLineRepository;
        this.fiscalPeriodRepository = fiscalPeriodRepository;
    }

    /**
     * Upload a SAP file, parse it, run dedup, and stage the import.
     * Spec: 05-sap-ingestion.md Steps 1-4
     *
     * @throws SapParseException if the file cannot be parsed at all
     */
    public SapImport uploadAndStage(MultipartFile file, String importedBy) {
        String filename = file.getOriginalFilename() != null ? file.getOriginalFilename() : "unknown";
        String lower = filename.toLowerCase();
        boolean isExcel = lower.endsWith(".xlsx") || lower.endsWith(".xls");
        boolean isCsv = lower.endsWith(".csv") || lower.endsWith(".txt");

        if (!isExcel && !isCsv) {
            throw new SapParseException(
                    "Unsupported file format: '" + filename + "'. Accepted formats: CSV (.csv, .txt) or Excel (.xlsx, .xls)");
        }

        List<ParsedRow> rows;
        try {
            rows = isExcel ? parseExcel(file) : parseCsv(file);
        } catch (Exception e) {
            throw new SapParseException("Cannot parse file '" + filename + "': " + e.getMessage(), e);
        }

        List<ActualLine> lines = new ArrayList<>();
        int errorCount = 0;
        int duplicateCount = 0;

        SapImport sapImport = new SapImport();
        sapImport.setFilename(filename);
        sapImport.setImportedBy(importedBy);
        sapImport.setStatus(SapImportStatus.STAGED);
        sapImportRepository.save(sapImport);

        for (ParsedRow row : rows) {
            if (row.hasError()) {
                errorCount++;
                continue;
            }

            String hash = computeHash(row);
            boolean isDup = actualLineRepository.findByLineHashAndDuplicateFalse(hash).isPresent();

            ActualLine line = new ActualLine();
            line.setSapImport(sapImport);
            line.setSapDocumentNumber(row.docNumber);
            line.setPostingDate(row.postingDate);
            line.setAmount(row.amount);
            line.setVendorName(row.vendorName);
            line.setCostCenter(row.costCenter);
            line.setWbse(row.wbse);
            line.setGlAccount(row.glAccount);
            line.setDescription(row.description);
            line.setLineHash(hash);
            line.setDuplicate(isDup);

            // Resolve fiscal period from posting date
            LocalDate firstOfMonth = row.postingDate.withDayOfMonth(1);
            fiscalPeriodRepository.findByCalendarMonth(firstOfMonth)
                    .ifPresent(fp -> line.setFiscalPeriodId(fp.getPeriodId()));

            actualLineRepository.save(line);
            lines.add(line);

            if (isDup) {
                duplicateCount++;
            }
        }

        int newLines = lines.size() - duplicateCount;
        sapImport.setTotalLines(rows.size());
        sapImport.setNewLines(newLines);
        sapImport.setDuplicateLines(duplicateCount);
        sapImport.setErrorLines(errorCount);
        sapImportRepository.save(sapImport);

        return sapImport;
    }

    private List<ParsedRow> parseCsv(MultipartFile file) throws Exception {
        try (InputStreamReader reader = new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8);
             CSVParser parser = CSVFormat.DEFAULT
                     .builder()
                     .setHeader()
                     .setSkipHeaderRecord(true)
                     .setIgnoreHeaderCase(true)
                     .setTrim(true)
                     .build()
                     .parse(reader)) {

            List<ParsedRow> rows = new ArrayList<>();
            for (CSVRecord record : parser) {
                rows.add(mapCsvRecord(record));
            }
            return rows;
        }
    }

    private ParsedRow mapCsvRecord(CSVRecord record) {
        ParsedRow row = new ParsedRow();
        row.docNumber = getField(record, COL_DOC_NUMBER);
        row.vendorName = getField(record, COL_VENDOR);
        row.costCenter = getField(record, COL_COST_CENTER);
        row.wbse = getField(record, COL_WBSE);
        row.glAccount = getField(record, COL_GL);
        row.description = getField(record, COL_DESCRIPTION);

        String dateStr = getField(record, COL_POSTING_DATE);
        if (dateStr == null || dateStr.isBlank()) {
            row.error = "Missing posting_date";
            return row;
        }
        try {
            row.postingDate = parseDate(dateStr);
        } catch (DateTimeParseException e) {
            row.error = "Invalid posting_date: " + dateStr;
            return row;
        }

        String amountStr = getField(record, COL_AMOUNT);
        if (amountStr == null || amountStr.isBlank()) {
            row.error = "Missing amount";
            return row;
        }
        try {
            row.amount = parseAmount(amountStr);
        } catch (NumberFormatException e) {
            row.error = "Invalid amount: " + amountStr;
            return row;
        }

        return row;
    }

    private List<ParsedRow> parseExcel(MultipartFile file) throws Exception {
        try (Workbook workbook = WorkbookFactory.create(file.getInputStream())) {
            Sheet sheet = workbook.getSheetAt(0);
            Row headerRow = sheet.getRow(0);
            if (headerRow == null) {
                throw new SapParseException("Excel file has no header row");
            }

            // Build column index map from headers
            java.util.Map<String, Integer> colIndex = new java.util.HashMap<>();
            for (Cell cell : headerRow) {
                colIndex.put(cell.getStringCellValue().trim().toLowerCase(), cell.getColumnIndex());
            }

            List<ParsedRow> rows = new ArrayList<>();
            for (int r = 1; r <= sheet.getLastRowNum(); r++) {
                Row row = sheet.getRow(r);
                if (row == null) continue;
                rows.add(mapExcelRow(row, colIndex));
            }
            return rows;
        }
    }

    private ParsedRow mapExcelRow(Row row, java.util.Map<String, Integer> colIndex) {
        ParsedRow parsed = new ParsedRow();
        parsed.docNumber = getExcelCell(row, colIndex, COL_DOC_NUMBER);
        parsed.vendorName = getExcelCell(row, colIndex, COL_VENDOR);
        parsed.costCenter = getExcelCell(row, colIndex, COL_COST_CENTER);
        parsed.wbse = getExcelCell(row, colIndex, COL_WBSE);
        parsed.glAccount = getExcelCell(row, colIndex, COL_GL);
        parsed.description = getExcelCell(row, colIndex, COL_DESCRIPTION);

        String dateStr = getExcelCell(row, colIndex, COL_POSTING_DATE);
        if (dateStr == null || dateStr.isBlank()) {
            parsed.error = "Missing posting_date";
            return parsed;
        }
        try {
            parsed.postingDate = parseDate(dateStr);
        } catch (DateTimeParseException e) {
            parsed.error = "Invalid posting_date: " + dateStr;
            return parsed;
        }

        String amountStr = getExcelCell(row, colIndex, COL_AMOUNT);
        if (amountStr == null || amountStr.isBlank()) {
            parsed.error = "Missing amount";
            return parsed;
        }
        try {
            parsed.amount = parseAmount(amountStr);
        } catch (NumberFormatException e) {
            parsed.error = "Invalid amount: " + amountStr;
            return parsed;
        }

        return parsed;
    }

    private String getField(CSVRecord record, String[] candidates) {
        for (String name : candidates) {
            try {
                String val = record.get(name);
                return val != null ? val.trim() : null;
            } catch (IllegalArgumentException ignored) {
                // column not present in this CSV
            }
        }
        return null;
    }

    private String getExcelCell(Row row, java.util.Map<String, Integer> colIndex, String[] candidates) {
        for (String name : candidates) {
            Integer idx = colIndex.get(name.toLowerCase());
            if (idx != null) {
                Cell cell = row.getCell(idx);
                if (cell != null) {
                    return getCellValueAsString(cell);
                }
            }
        }
        return null;
    }

    private String getCellValueAsString(Cell cell) {
        return switch (cell.getCellType()) {
            case STRING -> cell.getStringCellValue().trim();
            case NUMERIC -> {
                if (DateUtil.isCellDateFormatted(cell)) {
                    yield cell.getLocalDateTimeCellValue().toLocalDate().toString();
                }
                yield String.valueOf(cell.getNumericCellValue());
            }
            case BLANK -> null;
            default -> cell.toString().trim();
        };
    }

    private LocalDate parseDate(String dateStr) {
        // Try common formats
        for (DateTimeFormatter fmt : new DateTimeFormatter[]{
                DateTimeFormatter.ISO_LOCAL_DATE,
                DateTimeFormatter.ofPattern("MM/dd/yyyy"),
                DateTimeFormatter.ofPattern("MM/dd/yy"),
                DateTimeFormatter.ofPattern("d.M.yyyy"),
                DateTimeFormatter.ofPattern("d-MMM-yyyy", java.util.Locale.ENGLISH)
        }) {
            try {
                return LocalDate.parse(dateStr, fmt);
            } catch (DateTimeParseException ignored) {
            }
        }
        throw new DateTimeParseException("Cannot parse date: " + dateStr, dateStr, 0);
    }

    private BigDecimal parseAmount(String amountStr) {
        // Remove currency symbols, commas, parens for negatives
        String cleaned = amountStr.replaceAll("[^0-9.\\-]", "");
        return new BigDecimal(cleaned).setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * Compute SHA-256 hash for dedup.
     * Spec: 05-sap-ingestion.md Section 3, BR-08
     */
    String computeHash(ParsedRow row) {
        String input = normalize(row.docNumber) + "|" +
                       normalize(row.postingDate != null ? row.postingDate.toString() : null) + "|" +
                       normalize(row.amount != null ? row.amount.toPlainString() : null) + "|" +
                       normalize(row.vendorName) + "|" +
                       normalize(row.costCenter) + "|" +
                       normalize(row.wbse) + "|" +
                       normalize(row.glAccount) + "|" +
                       normalize(row.description);
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(input.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (Exception e) {
            throw new RuntimeException("SHA-256 not available", e);
        }
    }

    private String normalize(String value) {
        if (value == null || value.isBlank()) return "";
        return value.trim().toUpperCase();
    }

    /** Simple DTO for a parsed row before persistence. */
    static class ParsedRow {
        String docNumber;
        LocalDate postingDate;
        BigDecimal amount;
        String vendorName;
        String costCenter;
        String wbse;
        String glAccount;
        String description;
        String error;

        boolean hasError() {
            return error != null;
        }
    }
}
