package com.jcarranza.minimidoffice.service;

import com.jcarranza.minimidoffice.domain.report.ReportResult;
import com.jcarranza.minimidoffice.persistence.dao.ReportingDao;
import org.springframework.stereotype.Service;

import java.time.LocalDate;

/**
 * Direct delegation to the DAO. The business logic of reporting IS the SQL —
 * there is no additional transformation at this layer. The service exists to maintain
 * layer separation and to keep the controller unaware of the DAO.
 */
@Service
public class ReportingServiceImpl implements ReportingService {

    private final ReportingDao reportingDao;

    public ReportingServiceImpl(ReportingDao reportingDao) {
        this.reportingDao = reportingDao;
    }

    @Override
    public ReportResult byDestination(LocalDate from, LocalDate to) {
        return reportingDao.bookingsByDestination(from, to);
    }

    @Override
    public ReportResult byProvider(LocalDate from, LocalDate to) {
        return reportingDao.bookingsByProvider(from, to);
    }

    @Override
    public ReportResult byMonth(LocalDate from, LocalDate to) {
        return reportingDao.bookingsByMonth(from, to);
    }
}
