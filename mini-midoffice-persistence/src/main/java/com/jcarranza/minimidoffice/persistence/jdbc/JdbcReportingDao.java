package com.jcarranza.minimidoffice.persistence.jdbc;

import com.jcarranza.minimidoffice.domain.report.ReportResult;
import com.jcarranza.minimidoffice.persistence.dao.ReportingDao;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;

import javax.sql.DataSource;
import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Ad-hoc reporting via native JDBC — no Hibernate, no HQL.
 *
 * Rationale: reports are analytical queries with GROUP BY and aggregations
 * that do not fit the ORM model. Using DataSource directly is more explicit,
 * faster (no Hibernate session overhead), and mirrors the real support flow:
 * "someone asks for data, we write the SQL and export the result."
 *
 * Column headers are derived from ResultSetMetaData — if the query changes,
 * the export reflects it automatically without touching any mapping code.
 */
@Repository
public class JdbcReportingDao implements ReportingDao {

    private static final Logger log = LoggerFactory.getLogger(JdbcReportingDao.class);

    private final DataSource dataSource;

    public JdbcReportingDao(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    // -------------------------------------------------------------------------
    // Queries
    // -------------------------------------------------------------------------

    @Override
    public ReportResult bookingsByDestination(LocalDate from, LocalDate to) {
        String select =
            "SELECT b.destination                                AS \"Destination\", " +
            "       COUNT(*)                                     AS \"Bookings\", " +
            "       COALESCE(SUM(b.confirmed_price),  0)         AS \"Revenue\", " +
            "       COALESCE(ROUND(AVG(b.confirmed_price), 2), 0) AS \"Avg Price\" " +
            "FROM booking b ";
        String group = " GROUP BY b.destination ORDER BY COUNT(*) DESC";
        return run("Bookings by Destination", select, null, group, from, to);
    }

    @Override
    public ReportResult bookingsByProvider(LocalDate from, LocalDate to) {
        String select =
            "SELECT b.provider                                   AS \"Provider\", " +
            "       COUNT(*)                                     AS \"Bookings\", " +
            "       COALESCE(SUM(b.confirmed_price),  0)         AS \"Revenue\", " +
            "       COALESCE(ROUND(AVG(b.confirmed_price), 2), 0) AS \"Avg Price\" " +
            "FROM booking b ";
        String group = " GROUP BY b.provider ORDER BY COUNT(*) DESC";
        return run("Bookings by Provider", select, null, group, from, to);
    }

    @Override
    public ReportResult bookingsByMonth(LocalDate from, LocalDate to) {
        // departure_date reflects the actual travel month, not the booking creation date.
        // Bookings without departure_date (NULL) are excluded to avoid a "null" row in the GROUP BY.
        String select =
            "SELECT TO_CHAR(b.departure_date, 'YYYY-MM')         AS \"Month\", " +
            "       COUNT(*)                                      AS \"Bookings\", " +
            "       COALESCE(SUM(b.confirmed_price),  0)          AS \"Revenue\", " +
            "       COALESCE(ROUND(AVG(b.confirmed_price), 2), 0) AS \"Avg Price\" " +
            "FROM booking b ";
        String extra = "AND b.departure_date IS NOT NULL";
        String group = " GROUP BY TO_CHAR(b.departure_date, 'YYYY-MM') ORDER BY 1";
        return run("Bookings by Month", select, extra, group, from, to);
    }

    // -------------------------------------------------------------------------
    // Query engine
    // -------------------------------------------------------------------------

    /**
     * Builds and executes the dynamic query, returning a ReportResult whose
     * column headers are derived from ResultSetMetaData (auto-adapts to SQL changes).
     *
     * @param select     SELECT ... FROM booking b (no WHERE clause)
     * @param extraWhere additional AND condition (nullable), e.g. "AND b.departure_date IS NOT NULL"
     * @param groupByOrder GROUP BY ... ORDER BY ...
     * @param from       start date filter (nullable)
     * @param to         end date filter (nullable)
     */
    private ReportResult run(String title, String select, String extraWhere,
                              String groupByOrder, LocalDate from, LocalDate to) {

        StringBuilder sql = new StringBuilder(select)
            .append("WHERE b.status = 'RESERVED' ");

        if (extraWhere != null) {
            sql.append(extraWhere).append(' ');
        }

        List<Object> params = new ArrayList<>();
        if (from != null) {
            sql.append("AND b.departure_date >= ? ");
            params.add(Date.valueOf(from));
        }
        if (to != null) {
            sql.append("AND b.departure_date <= ? ");
            params.add(Date.valueOf(to));
        }
        sql.append(groupByOrder);

        log.debug("Report [{}] SQL: {}", title, sql);

        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql.toString())) {

            for (int i = 0; i < params.size(); i++) {
                ps.setObject(i + 1, params.get(i));
            }

            try (ResultSet rs = ps.executeQuery()) {
                ResultSetMetaData meta  = rs.getMetaData();
                int               cols  = meta.getColumnCount();

                List<String> headers = new ArrayList<>(cols);
                for (int c = 1; c <= cols; c++) {
                    headers.add(meta.getColumnLabel(c));
                }

                List<List<Object>> rows = new ArrayList<>();
                while (rs.next()) {
                    List<Object> row = new ArrayList<>(cols);
                    for (int c = 1; c <= cols; c++) {
                        row.add(rs.getObject(c));
                    }
                    rows.add(row);
                }

                log.info("Report [{}] → {} rows (from={} to={})", title, rows.size(), from, to);
                return new ReportResult(title, headers, rows);
            }

        } catch (SQLException e) {
            throw new RuntimeException("Report query failed: " + title, e);
        }
    }
}
