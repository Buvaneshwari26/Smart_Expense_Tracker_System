package com.tracker.service;

import com.tracker.dto.*;
import com.tracker.model.*;
import com.tracker.repository.*;
import com.lowagie.text.*;
import com.lowagie.text.pdf.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ExportService {

    private final ExpenseService expenseService;
    private final IncomeService incomeService;

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    // =================== EXCEL EXPORTS ===================

    @Transactional(readOnly = true)
    public byte[] exportExpensesToExcel(Long userId) throws IOException {
        List<Expense> expenses = expenseService.getExpenseEntitiesByUserId(userId);
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("Expenses");
            CellStyle headerStyle = createHeaderStyle(workbook);

            String[] headers = {"ID", "Date", "Category", "Amount (₹)", "Description"};
            Row header = sheet.createRow(0);
            for (int i = 0; i < headers.length; i++) {
                Cell cell = header.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }
            int rowNum = 1;
            BigDecimal total = BigDecimal.ZERO;
            for (Expense e : expenses) {
                Row row = sheet.createRow(rowNum++);
                row.createCell(0).setCellValue(e.getId());
                row.createCell(1).setCellValue(e.getDate().format(FORMATTER));
                row.createCell(2).setCellValue(e.getCategory().getName());
                row.createCell(3).setCellValue(e.getAmount().doubleValue());
                row.createCell(4).setCellValue(e.getDescription() != null ? e.getDescription() : "");
                total = total.add(e.getAmount());
            }
            Row totalRow = sheet.createRow(rowNum);
            totalRow.createCell(2).setCellValue("TOTAL");
            totalRow.createCell(3).setCellValue(total.doubleValue());

            for (int i = 0; i < headers.length; i++) sheet.autoSizeColumn(i);
            workbook.write(out);
            return out.toByteArray();
        }
    }

    @Transactional(readOnly = true)
    public byte[] exportIncomesToExcel(Long userId) throws IOException {
        List<Income> incomes = incomeService.getIncomeEntitiesByUserId(userId);
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("Incomes");
            CellStyle headerStyle = createHeaderStyle(workbook);

            String[] headers = {"ID", "Date", "Source", "Category", "Amount (₹)", "Description"};
            Row header = sheet.createRow(0);
            for (int i = 0; i < headers.length; i++) {
                Cell cell = header.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }
            int rowNum = 1;
            BigDecimal total = BigDecimal.ZERO;
            for (Income inc : incomes) {
                Row row = sheet.createRow(rowNum++);
                row.createCell(0).setCellValue(inc.getId());
                row.createCell(1).setCellValue(inc.getDate().format(FORMATTER));
                row.createCell(2).setCellValue(inc.getSource() != null ? inc.getSource() : "");
                row.createCell(3).setCellValue(inc.getCategory().getName());
                row.createCell(4).setCellValue(inc.getAmount().doubleValue());
                row.createCell(5).setCellValue(inc.getDescription() != null ? inc.getDescription() : "");
                total = total.add(inc.getAmount());
            }
            Row totalRow = sheet.createRow(rowNum);
            totalRow.createCell(3).setCellValue("TOTAL");
            totalRow.createCell(4).setCellValue(total.doubleValue());

            for (int i = 0; i < headers.length; i++) sheet.autoSizeColumn(i);
            workbook.write(out);
            return out.toByteArray();
        }
    }

    // =================== CSV EXPORTS ===================

    @Transactional(readOnly = true)
    public byte[] exportExpensesToCsv(Long userId) {
        List<Expense> expenses = expenseService.getExpenseEntitiesByUserId(userId);
        StringBuilder sb = new StringBuilder("ID,Date,Category,Amount,Description\n");
        for (Expense e : expenses) {
            sb.append(e.getId()).append(",")
              .append(e.getDate().format(FORMATTER)).append(",")
              .append(e.getCategory().getName()).append(",")
              .append(e.getAmount()).append(",")
              .append(e.getDescription() != null ? e.getDescription().replace(",", " ") : "").append("\n");
        }
        return sb.toString().getBytes();
    }

    @Transactional(readOnly = true)
    public byte[] exportIncomesToCsv(Long userId) {
        List<Income> incomes = incomeService.getIncomeEntitiesByUserId(userId);
        StringBuilder sb = new StringBuilder("ID,Date,Source,Category,Amount,Description\n");
        for (Income inc : incomes) {
            sb.append(inc.getId()).append(",")
              .append(inc.getDate().format(FORMATTER)).append(",")
              .append(inc.getSource() != null ? inc.getSource() : "").append(",")
              .append(inc.getCategory().getName()).append(",")
              .append(inc.getAmount()).append(",")
              .append(inc.getDescription() != null ? inc.getDescription().replace(",", " ") : "").append("\n");
        }
        return sb.toString().getBytes();
    }

    // =================== PDF REPORT ===================

    @Transactional(readOnly = true)
    public byte[] exportExpensesToPdf(Long userId) throws DocumentException, IOException {
        List<Expense> expenses = expenseService.getExpenseEntitiesByUserId(userId);
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Document document = new Document(PageSize.A4);
            PdfWriter.getInstance(document, out);
            document.open();

            Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18);
            Font headerFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11);
            Font bodyFont = FontFactory.getFont(FontFactory.HELVETICA, 10);

            Paragraph title = new Paragraph("Smart Expense Tracker — Expense Report", titleFont);
            title.setAlignment(Element.ALIGN_CENTER);
            title.setSpacingAfter(10);
            document.add(title);

            Paragraph generatedOn = new Paragraph("Generated on: " + LocalDate.now().format(FORMATTER), bodyFont);
            generatedOn.setAlignment(Element.ALIGN_CENTER);
            generatedOn.setSpacingAfter(20);
            document.add(generatedOn);

            PdfPTable table = new PdfPTable(5);
            table.setWidthPercentage(100);
            table.setWidths(new float[]{1f, 2f, 3f, 2f, 4f});

            for (String h : new String[]{"ID", "Date", "Category", "Amount", "Description"}) {
                PdfPCell cell = new PdfPCell(new Phrase(h, headerFont));
                cell.setBackgroundColor(new java.awt.Color(15, 52, 96));
                cell.setPadding(8);
                table.addCell(cell);
            }

            BigDecimal total = BigDecimal.ZERO;
            for (Expense e : expenses) {
                table.addCell(new Phrase(String.valueOf(e.getId()), bodyFont));
                table.addCell(new Phrase(e.getDate().format(FORMATTER), bodyFont));
                table.addCell(new Phrase(e.getCategory().getName(), bodyFont));
                table.addCell(new Phrase("₹" + e.getAmount(), bodyFont));
                table.addCell(new Phrase(e.getDescription() != null ? e.getDescription() : "", bodyFont));
                total = total.add(e.getAmount());
            }
            document.add(table);

            Paragraph totalPara = new Paragraph("\nTotal Expenses: ₹" + total,
                    FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12));
            totalPara.setSpacingBefore(10);
            document.add(totalPara);
            document.close();
            return out.toByteArray();
        }
    }

    private CellStyle createHeaderStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        org.apache.poi.ss.usermodel.Font font = workbook.createFont();
        font.setBold(true);
        style.setFont(font);
        style.setFillForegroundColor(IndexedColors.DARK_BLUE.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        return style;
    }
}
