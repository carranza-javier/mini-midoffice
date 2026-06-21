package com.jcarranza.minimidoffice.web.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.jcarranza.minimidoffice.domain.report.ReportResult;
import com.jcarranza.minimidoffice.service.ReportingService;
import com.jcarranza.minimidoffice.web.export.CsvExporter;
import com.jcarranza.minimidoffice.web.export.ExcelExporter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;

/**
 * Ad-hoc reporting endpoints.
 *
 * GET /api/reports/by-destination  ?from=&to=           → JSON (for on-screen tables)
 * GET /api/reports/by-destination  ?from=&to=&format=csv|excel → file download
 * (same pattern for /by-provider and /by-month)
 *
 * Omitting ?format= (or passing format=json) returns JSON.
 * The ?from= and ?to= parameters filter by departure_date (ISO yyyy-MM-dd).
 */
@RestController
@RequestMapping("/reports")
public class ReportController {

    private static final Logger log = LoggerFactory.getLogger(ReportController.class);

    private static final ObjectMapper JSON = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    private final ReportingService reportingService;

    public ReportController(ReportingService reportingService) {
        this.reportingService = reportingService;
    }

    @GetMapping("/by-destination")
    public void byDestination(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) String format,
            HttpServletResponse response) throws IOException {
        ReportResult report = reportingService.byDestination(from, to);
        if (format == null) { writeJson(report, response); }
        else                { writeReport(report, format, "bookings-by-destination", response); }
    }

    @GetMapping("/by-provider")
    public void byProvider(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) String format,
            HttpServletResponse response) throws IOException {
        ReportResult report = reportingService.byProvider(from, to);
        if (format == null) { writeJson(report, response); }
        else                { writeReport(report, format, "bookings-by-provider", response); }
    }

    @GetMapping("/by-month")
    public void byMonth(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) String format,
            HttpServletResponse response) throws IOException {
        ReportResult report = reportingService.byMonth(from, to);
        if (format == null) { writeJson(report, response); }
        else                { writeReport(report, format, "bookings-by-month", response); }
    }

    // -------------------------------------------------------------------------
    // Export dispatch
    // -------------------------------------------------------------------------

    private void writeJson(ReportResult report, HttpServletResponse response) throws IOException {
        log.info("Report JSON: {} rows={}", report.getTitle(), report.getRows().size());
        response.setContentType("application/json;charset=UTF-8");
        JSON.writeValue(response.getOutputStream(), report);
    }

    private void writeReport(ReportResult report, String format, String filename,
                              HttpServletResponse response) throws IOException {
        log.info("Report export: {} rows={} format={}", report.getTitle(), report.getRows().size(), format);

        if ("excel".equalsIgnoreCase(format) || "xlsx".equalsIgnoreCase(format)) {
            response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            response.setHeader("Content-Disposition",
                "attachment; filename=\"" + filename + ".xlsx\"");
            ExcelExporter.write(report, response.getOutputStream());
        } else {
            response.setContentType("text/csv; charset=UTF-8");
            response.setHeader("Content-Disposition",
                "attachment; filename=\"" + filename + ".csv\"");
            // UTF-8 BOM so Excel on Windows opens the CSV with the correct encoding
            response.getOutputStream().write(new byte[]{(byte) 0xEF, (byte) 0xBB, (byte) 0xBF});
            CsvExporter.write(
                report,
                new OutputStreamWriter(response.getOutputStream(), StandardCharsets.UTF_8));
        }
    }
}
