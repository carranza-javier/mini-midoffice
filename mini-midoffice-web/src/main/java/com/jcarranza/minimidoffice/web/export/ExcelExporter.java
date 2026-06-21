package com.jcarranza.minimidoffice.web.export;

import com.jcarranza.minimidoffice.domain.report.ReportResult;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.IOException;
import java.io.OutputStream;
import java.util.List;

/**
 * Serializes a ReportResult to Excel (.xlsx) using Apache POI XSSFWorkbook.
 *
 * Format:
 *   - Row 0: bold headers with light-blue background.
 *   - Rows 1+: data with automatic numeric formatting based on Java type:
 *       Long/Integer → integer with no decimals (#,##0)
 *       BigDecimal/Double → two decimals (#,##0.00)
 *       Others → text.
 *   - Columns auto-sized to content.
 */
public final class ExcelExporter {

    private ExcelExporter() {}

    public static void write(ReportResult report, OutputStream out) throws IOException {
        try (Workbook wb = new XSSFWorkbook()) {
            // Sheet name: report title sanitised (Excel does not accept / \ ? * : [ ])
            String sheetName = report.getTitle().replaceAll("[/\\\\?*:\\[\\]]", "-");
            Sheet sheet = wb.createSheet(sheetName);

            // ----- Styles -------------------------------------------------
            DataFormat format = wb.createDataFormat();

            CellStyle headerStyle = wb.createCellStyle();
            Font headerFont = wb.createFont();
            headerFont.setBold(true);
            headerStyle.setFont(headerFont);
            headerStyle.setFillForegroundColor(IndexedColors.PALE_BLUE.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            headerStyle.setBorderBottom(BorderStyle.THIN);

            CellStyle intStyle = wb.createCellStyle();
            intStyle.setDataFormat(format.getFormat("#,##0"));

            CellStyle moneyStyle = wb.createCellStyle();
            moneyStyle.setDataFormat(format.getFormat("#,##0.00"));

            // ----- Header -------------------------------------------------
            Row headerRow = sheet.createRow(0);
            List<String> headers = report.getHeaders();
            for (int c = 0; c < headers.size(); c++) {
                Cell cell = headerRow.createCell(c);
                cell.setCellValue(headers.get(c));
                cell.setCellStyle(headerStyle);
            }

            // ----- Data ---------------------------------------------------
            int rowIdx = 1;
            for (List<Object> rowData : report.getRows()) {
                Row row = sheet.createRow(rowIdx++);
                for (int c = 0; c < rowData.size(); c++) {
                    Cell cell = row.createCell(c);
                    Object val = rowData.get(c);
                    if (val instanceof Long || val instanceof Integer) {
                        cell.setCellValue(((Number) val).longValue());
                        cell.setCellStyle(intStyle);
                    } else if (val instanceof Number) {
                        cell.setCellValue(((Number) val).doubleValue());
                        cell.setCellStyle(moneyStyle);
                    } else if (val != null) {
                        cell.setCellValue(val.toString());
                    }
                }
            }

            // ----- Auto-size columns --------------------------------------
            for (int c = 0; c < headers.size(); c++) {
                sheet.autoSizeColumn(c);
            }

            wb.write(out);
            out.flush();
        }
    }
}
