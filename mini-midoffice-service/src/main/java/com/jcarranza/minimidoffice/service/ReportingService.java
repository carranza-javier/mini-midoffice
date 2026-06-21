package com.jcarranza.minimidoffice.service;

import com.jcarranza.minimidoffice.domain.report.ReportResult;

import java.time.LocalDate;

public interface ReportingService {
    ReportResult byDestination(LocalDate from, LocalDate to);
    ReportResult byProvider(LocalDate from, LocalDate to);
    ReportResult byMonth(LocalDate from, LocalDate to);
}
