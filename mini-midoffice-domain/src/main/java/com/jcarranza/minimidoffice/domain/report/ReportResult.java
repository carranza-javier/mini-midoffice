package com.jcarranza.minimidoffice.domain.report;

import java.util.List;

/**
 * Tabular result of an ad-hoc report: column headers + raw value rows.
 * Built directly from ResultSetMetaData by the DAO — no POJO mapping.
 */
public class ReportResult {

    private final String            title;
    private final List<String>      headers;
    private final List<List<Object>> rows;

    public ReportResult(String title, List<String> headers, List<List<Object>> rows) {
        this.title   = title;
        this.headers = headers;
        this.rows    = rows;
    }

    public String             getTitle()   { return title; }
    public List<String>       getHeaders() { return headers; }
    public List<List<Object>> getRows()    { return rows; }
}
