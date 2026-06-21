package com.jcarranza.minimidoffice.persistence.dao;

import com.jcarranza.minimidoffice.domain.report.ReportResult;

import java.time.LocalDate;

public interface ReportingDao {
    ReportResult bookingsByDestination(LocalDate from, LocalDate to);
    ReportResult bookingsByProvider(LocalDate from, LocalDate to);
    ReportResult bookingsByMonth(LocalDate from, LocalDate to);
}
