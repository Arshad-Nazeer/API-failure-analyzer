package com.apifailureanalyzer.controller;

import com.apifailureanalyzer.repository.AnalysisReportRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class HealthController {

    private final AnalysisReportRepository reportRepository;

    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> health() {
        Map<String, Object> status = new LinkedHashMap<>();
        status.put("status", "UP");
        status.put("timestamp", Instant.now().toString());
        status.put("service", "API Failure Analyzer");
        status.put("version", "1.0.0");

        try {
            long count = reportRepository.count();
            status.put("database", "UP");
            status.put("savedReports", count);
        } catch (Exception e) {
            status.put("database", "DOWN");
            status.put("databaseError", "Database connectivity issue");
        }

        return ResponseEntity.ok(status);
    }
}
