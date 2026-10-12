package com.apifailureanalyzer;

import com.apifailureanalyzer.analyzer.RecommendationEngine;
import com.apifailureanalyzer.dto.AnalysisResponse;
import com.apifailureanalyzer.service.LogAnalysisService;
import com.apifailureanalyzer.parser.LogParser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class LogAnalysisServiceTest {

    private LogAnalysisService service;

    @BeforeEach
    void setUp() {
        service = new LogAnalysisService(new LogParser(), new RecommendationEngine());
    }

    @Test
    @DisplayName("Analyze empty input returns zeroed result")
    void testEmptyInput() {
        AnalysisResponse result = service.analyze("");
        assertThat(result.getTotalLines()).isEqualTo(0);
        assertThat(result.getTotalErrors()).isEqualTo(0);
        assertThat(result.getStatusCodeCounts()).isEmpty();
    }

    @Test
    @DisplayName("HTTP 5xx counted as errors")
    void testStatusCodeCounting() {
        String logs = "GET /api/users 500\nPOST /api/orders 503\nGET /api/products 200";
        AnalysisResponse result = service.analyze(logs);
        assertThat(result.getStatusCodeCounts().get("500")).isEqualTo(1L);
        assertThat(result.getStatusCodeCounts().get("503")).isEqualTo(1L);
        assertThat(result.getStatusCodeCounts().get("200")).isEqualTo(1L);
        assertThat(result.getStatusGroupCounts().get("5xx")).isEqualTo(2L);
        assertThat(result.getStatusGroupCounts().get("2xx")).isEqualTo(1L);
    }

    @Test
    @DisplayName("Error grouping groups same endpoint+status")
    void testErrorGrouping() {
        String logs = "GET /api/users/123 500\nGET /api/users/456 500\nGET /api/users/789 500";
        AnalysisResponse result = service.analyze(logs);
        // All three should group to same pattern GET /api/users/{id} 500
        assertThat(result.getGroupedErrorPatterns()).hasSize(1);
        assertThat(result.getGroupedErrorPatterns().get(0).getOccurrenceCount()).isEqualTo(3L);
    }

    @Test
    @DisplayName("Severity HIGH for 5xx responses")
    void testSeverityHigh() {
        String logs = "GET /api/payments 500\nPOST /api/orders 502";
        AnalysisResponse result = service.analyze(logs);
        assertThat(result.getGroupedErrorPatterns()).isNotEmpty();
        boolean hasHighOrCritical = result.getGroupedErrorPatterns().stream()
            .anyMatch(g -> g.getSeverity().equals("HIGH") || g.getSeverity().equals("CRITICAL"));
        assertThat(hasHighOrCritical).isTrue();
    }

    @Test
    @DisplayName("Severity MEDIUM for 4xx responses")
    void testSeverityMedium() {
        String logs = "GET /api/profile 401\nDELETE /api/posts 403";
        AnalysisResponse result = service.analyze(logs);
        assertThat(result.getGroupedErrorPatterns()).isNotEmpty();
        boolean hasMedium = result.getGroupedErrorPatterns().stream()
            .anyMatch(g -> g.getSeverity().equals("MEDIUM"));
        assertThat(hasMedium).isTrue();
    }

    @Test
    @DisplayName("Recommendations generated for 401 errors")
    void testRecommendations401() {
        String logs = "GET /api/secure 401\nPOST /api/secure 401";
        AnalysisResponse result = service.analyze(logs);
        assertThat(result.getRecommendations()).isNotEmpty();
        boolean hasAuthRec = result.getRecommendations().stream()
            .anyMatch(r -> r.contains("401") || r.toLowerCase().contains("auth") || r.toLowerCase().contains("token"));
        assertThat(hasAuthRec).isTrue();
    }

    @Test
    @DisplayName("Endpoint summaries sorted by failure count")
    void testEndpointSummaries() {
        String logs = "GET /api/payments 500\nGET /api/payments 500\nGET /api/payments 500\n" +
                      "POST /api/users 404";
        AnalysisResponse result = service.analyze(logs);
        assertThat(result.getEndpointSummaries()).isNotEmpty();
        assertThat(result.getEndpointSummaries().get(0).getEndpointPattern()).isEqualTo("/api/payments");
    }

    @Test
    @DisplayName("Mixed log formats analyzed correctly")
    void testMixedLogAnalysis() {
        String logs = "2026-10-13 10:42:12 ERROR PaymentService - Payment gateway timeout\n" +
                      "GET /api/users 500 145ms\n" +
                      "{\"timestamp\":\"2026-10-13T10:42:14Z\",\"level\":\"ERROR\",\"method\":\"POST\",\"endpoint\":\"/api/orders\",\"status\":503,\"durationMs\":2000,\"message\":\"Service unavailable\"}\n" +
                      "401 Unauthorized\nConnection timed out";
        AnalysisResponse result = service.analyze(logs);
        assertThat(result.getTotalLines()).isEqualTo(5);
        assertThat(result.getTotalErrors()).isGreaterThan(0);
    }
}
