package com.jcarranza.minimidoffice.web.export;

import com.jcarranza.minimidoffice.domain.report.ReportResult;

import java.io.IOException;
import java.io.Writer;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Serializes a ReportResult to CSV (RFC 4180).
 * Fields that contain a comma, quote, or newline are enclosed in double quotes;
 * internal quotes are escaped by doubling them.
 */
public final class CsvExporter {

    private CsvExporter() {}

    public static void write(ReportResult report, Writer out) throws IOException {
        out.write(toCsvRow(report.getHeaders()));
        out.write("\r\n");
        for (List<Object> row : report.getRows()) {
            List<String> strings = row.stream()
                .map(v -> v != null ? v.toString() : "")
                .collect(Collectors.toList());
            out.write(toCsvRow(strings));
            out.write("\r\n");
        }
        out.flush();
    }

    private static String toCsvRow(List<String> values) {
        return values.stream()
            .map(CsvExporter::escapeField)
            .collect(Collectors.joining(","));
    }

    private static String escapeField(String value) {
        if (value == null || value.isEmpty()) return "";
        if (value.contains(",") || value.contains("\"") || value.contains("\r") || value.contains("\n")) {
            return '"' + value.replace("\"", "\"\"") + '"';
        }
        return value;
    }
}
