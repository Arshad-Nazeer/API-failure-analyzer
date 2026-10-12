package com.apifailureanalyzer.service;

import com.apifailureanalyzer.dto.AnalysisResponse;
import com.apifailureanalyzer.exception.ResourceNotFoundException;
import com.apifailureanalyzer.model.AnalysisReport;
import com.apifailureanalyzer.repository.AnalysisReportRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReportService {

    private final AnalysisReportRepository reportRepository;

    public AnalysisReport saveReport(String reportName, AnalysisResponse analysis) {
        AnalysisReport report = AnalysisReport.builder()
            .reportName(reportName != null && !reportName.isBlank() ? reportName : "Unnamed Report")
            .createdAt(Instant.now())
            .totalLines(analysis.getTotalLines())
            .parsedLines(analysis.getParsedLines())
            .unparsedLines(analysis.getUnparsedLines())
            .totalErrors(analysis.getTotalErrors())
            .totalWarnings(analysis.getTotalWarnings())
            .totalHttpResponses(analysis.getTotalHttpResponses())
            .statusCodeCounts(analysis.getStatusCodeCounts())
            .statusGroupCounts(analysis.getStatusGroupCounts())
            .endpointSummaries(analysis.getEndpointSummaries())
            .groupedErrorPatterns(analysis.getGroupedErrorPatterns())
            .recommendations(analysis.getRecommendations())
            .unparsedLineSamples(analysis.getUnparsedLineSamples())
            .analysisVersion(analysis.getAnalysisVersion())
            .build();

        AnalysisReport saved = reportRepository.save(report);
        log.info("Saved report id={}, name={}", saved.getId(), saved.getReportName());
        return saved;
    }

    public Page<AnalysisReport> listReports(Pageable pageable) {
        return reportRepository.findAll(pageable);
    }

    public AnalysisReport getReport(String id) {
        return reportRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Report not found with id: " + id));
    }

    public void deleteReport(String id) {
        if (!reportRepository.existsById(id)) {
            throw new ResourceNotFoundException("Report not found with id: " + id);
        }
        reportRepository.deleteById(id);
        log.info("Deleted report id={}", id);
    }
}
