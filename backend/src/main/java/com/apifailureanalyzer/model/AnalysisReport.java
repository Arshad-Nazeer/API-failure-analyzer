package com.apifailureanalyzer.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "analysis_reports")
public class AnalysisReport {

    @Id
    private String id;

    @Indexed
    private String reportName;

    @Indexed
    private Instant createdAt;

    private int totalLines;
    private int parsedLines;
    private int unparsedLines;
    private int totalErrors;
    private int totalWarnings;
    private int totalHttpResponses;

    private Map<String, Long> statusCodeCounts;
    private Map<String, Long> statusGroupCounts;

    private List<EndpointSummary> endpointSummaries;
    private List<ErrorGroup> groupedErrorPatterns;
    private List<String> recommendations;
    private List<String> unparsedLineSamples;

    private String analysisVersion;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class EndpointSummary {
        private String endpointPattern;
        private String method;
        private long failureCount;
        private long totalRequests;
        private Map<String, Long> statusDistribution;
        private Double avgDurationMs;
        private long slowRequestCount;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ErrorGroup {
        private String groupKey;
        private String errorType;
        private String severity;
        private long occurrenceCount;
        private List<String> affectedEndpoints;
        private String firstSeen;
        private String lastSeen;
        private String representativeMessage;
        private List<String> suggestions;
    }
}
