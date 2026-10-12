package com.apifailureanalyzer.service;

import com.apifailureanalyzer.analyzer.RecommendationEngine;
import com.apifailureanalyzer.dto.AnalysisResponse;
import com.apifailureanalyzer.model.AnalysisReport;
import com.apifailureanalyzer.parser.LogEntry;
import com.apifailureanalyzer.parser.LogParser;
import com.apifailureanalyzer.parser.ParseResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class LogAnalysisService {

    private final LogParser logParser;
    private final RecommendationEngine recommendationEngine;

    @Value("${app.analysis-version:1.0.0}")
    private String analysisVersion;

    private static final long SLOW_REQUEST_THRESHOLD_MS = 1000L;
    private static final int CRITICAL_THRESHOLD = 10;

    public AnalysisResponse analyze(String rawLogs) {
        log.info("Starting log analysis, input length={}", rawLogs != null ? rawLogs.length() : 0);

        ParseResult parseResult = logParser.parse(rawLogs);
        List<LogEntry> entries = parseResult.getEntries();

        // Status code distribution
        Map<String, Long> statusCodeCounts = buildStatusCodeCounts(entries);
        Map<String, Long> statusGroupCounts = buildStatusGroupCounts(statusCodeCounts);

        // Count errors and warnings
        long totalErrors = entries.stream()
            .filter(e -> "ERROR".equals(e.getLevel()) ||
                (e.getStatusCode() != null && e.getStatusCode() >= 500))
            .count();
        long totalWarnings = entries.stream()
            .filter(e -> "WARN".equals(e.getLevel()) ||
                (e.getStatusCode() != null && e.getStatusCode() >= 400 && e.getStatusCode() < 500))
            .count();
        long totalHttpResponses = entries.stream()
            .filter(e -> e.getStatusCode() != null)
            .count();

        // Endpoint analysis
        List<AnalysisReport.EndpointSummary> endpointSummaries = buildEndpointSummaries(entries);

        // Error grouping
        List<AnalysisReport.ErrorGroup> errorGroups = buildErrorGroups(entries);

        // Recommendations
        List<String> recommendations = recommendationEngine.generateRecommendations(
            statusCodeCounts, errorGroups, endpointSummaries);

        // Warning for high unparsed rate
        String warningMessage = null;
        if (parseResult.getTotalLines() > 0) {
            double unparsedRate = (double) parseResult.getUnparsedCount() / parseResult.getTotalLines();
            if (unparsedRate > 0.5) {
                warningMessage = String.format(
                    "%.0f%% of log lines could not be parsed. The input may contain unsupported formats. " +
                    "Review the unparsed samples below.",
                    unparsedRate * 100);
            }
        }

        log.info("Analysis complete: total={}, parsed={}, unparsed={}, errors={}, warnings={}",
            parseResult.getTotalLines(), parseResult.getParsedCount(),
            parseResult.getUnparsedCount(), totalErrors, totalWarnings);

        return AnalysisResponse.builder()
            .totalLines(parseResult.getTotalLines())
            .parsedLines(parseResult.getParsedCount())
            .unparsedLines(parseResult.getUnparsedCount())
            .totalHttpResponses((int) totalHttpResponses)
            .totalErrors((int) totalErrors)
            .totalWarnings((int) totalWarnings)
            .statusCodeCounts(statusCodeCounts)
            .statusGroupCounts(statusGroupCounts)
            .endpointSummaries(endpointSummaries)
            .groupedErrorPatterns(errorGroups)
            .recommendations(recommendations)
            .unparsedLineSamples(parseResult.getUnparsedLines())
            .analysisVersion(analysisVersion)
            .warningMessage(warningMessage)
            .build();
    }

    private Map<String, Long> buildStatusCodeCounts(List<LogEntry> entries) {
        return entries.stream()
            .filter(e -> e.getStatusCode() != null)
            .collect(Collectors.groupingBy(
                e -> String.valueOf(e.getStatusCode()),
                Collectors.counting()
            ));
    }

    private Map<String, Long> buildStatusGroupCounts(Map<String, Long> statusCodeCounts) {
        Map<String, Long> groups = new LinkedHashMap<>();
        groups.put("2xx", 0L); groups.put("3xx", 0L);
        groups.put("4xx", 0L); groups.put("5xx", 0L); groups.put("unknown", 0L);

        statusCodeCounts.forEach((code, count) -> {
            try {
                int statusInt = Integer.parseInt(code);
                if (statusInt >= 200 && statusInt < 300) groups.merge("2xx", count, Long::sum);
                else if (statusInt >= 300 && statusInt < 400) groups.merge("3xx", count, Long::sum);
                else if (statusInt >= 400 && statusInt < 500) groups.merge("4xx", count, Long::sum);
                else if (statusInt >= 500) groups.merge("5xx", count, Long::sum);
                else groups.merge("unknown", count, Long::sum);
            } catch (NumberFormatException e) {
                groups.merge("unknown", count, Long::sum);
            }
        });
        return groups;
    }

    private List<AnalysisReport.EndpointSummary> buildEndpointSummaries(List<LogEntry> entries) {
        // Group by (method, endpointPattern)
        Map<String, List<LogEntry>> byEndpoint = entries.stream()
            .filter(e -> e.getEndpointPattern() != null)
            .collect(Collectors.groupingBy(e ->
                (e.getHttpMethod() != null ? e.getHttpMethod() : "UNKNOWN") + " " + e.getEndpointPattern()
            ));

        return byEndpoint.entrySet().stream()
            .map(entry -> {
                String key = entry.getKey();
                List<LogEntry> group = entry.getValue();
                String[] parts = key.split(" ", 2);
                String method = parts[0];
                String pattern = parts.length > 1 ? parts[1] : key;

                long failures = group.stream()
                    .filter(e -> e.getStatusCode() != null && e.getStatusCode() >= 400)
                    .count();
                long total = group.size();

                Map<String, Long> statusDist = group.stream()
                    .filter(e -> e.getStatusCode() != null)
                    .collect(Collectors.groupingBy(
                        e -> String.valueOf(e.getStatusCode()), Collectors.counting()));

                List<Long> durations = group.stream()
                    .filter(e -> e.getDurationMs() != null)
                    .map(LogEntry::getDurationMs)
                    .toList();
                Double avgDuration = durations.isEmpty() ? null :
                    durations.stream().mapToLong(Long::longValue).average().orElse(0);

                long slowCount = group.stream()
                    .filter(e -> e.getDurationMs() != null && e.getDurationMs() > SLOW_REQUEST_THRESHOLD_MS)
                    .count();

                return AnalysisReport.EndpointSummary.builder()
                    .endpointPattern(pattern)
                    .method(method)
                    .failureCount(failures)
                    .totalRequests(total)
                    .statusDistribution(statusDist)
                    .avgDurationMs(avgDuration)
                    .slowRequestCount(slowCount)
                    .build();
            })
            .sorted(Comparator.comparingLong(AnalysisReport.EndpointSummary::getFailureCount).reversed())
            .limit(20)
            .collect(Collectors.toList());
    }

    private List<AnalysisReport.ErrorGroup> buildErrorGroups(List<LogEntry> entries) {
        // Group by (errorType or statusCode) + endpointPattern
        Map<String, List<LogEntry>> groups = new LinkedHashMap<>();

        for (LogEntry entry : entries) {
            if (entry.getStatusCode() == null || entry.getStatusCode() < 400) {
                // Only group non-HTTP error entries if they have an exception type
                if (entry.getExceptionType() == null) continue;
            }

            String groupKey = buildGroupKey(entry);
            groups.computeIfAbsent(groupKey, k -> new ArrayList<>()).add(entry);
        }

        return groups.entrySet().stream()
            .filter(e -> !e.getValue().isEmpty())
            .map(entry -> {
                List<LogEntry> group = entry.getValue();
                LogEntry representative = group.get(0);

                String severity = determineSeverity(group);
                String errorType = representative.getExceptionType();
                if (errorType == null && representative.getStatusCode() != null) {
                    errorType = "HTTP " + representative.getStatusCode();
                }

                List<String> endpoints = group.stream()
                    .filter(e -> e.getEndpointPattern() != null)
                    .map(e -> (e.getHttpMethod() != null ? e.getHttpMethod() + " " : "") + e.getEndpointPattern())
                    .distinct().limit(5).collect(Collectors.toList());

                String firstSeen = group.stream()
                    .filter(e -> e.getTimestamp() != null)
                    .map(LogEntry::getTimestamp).findFirst().orElse(null);
                String lastSeen = group.stream()
                    .filter(e -> e.getTimestamp() != null)
                    .map(LogEntry::getTimestamp).reduce((a, b) -> b).orElse(null);

                List<String> suggestions = getSuggestionsForGroup(entry.getKey(), representative);

                return AnalysisReport.ErrorGroup.builder()
                    .groupKey(entry.getKey())
                    .errorType(errorType)
                    .severity(severity)
                    .occurrenceCount(group.size())
                    .affectedEndpoints(endpoints)
                    .firstSeen(firstSeen)
                    .lastSeen(lastSeen)
                    .representativeMessage(representative.getMessage())
                    .suggestions(suggestions)
                    .build();
            })
            .sorted(Comparator.comparingLong(AnalysisReport.ErrorGroup::getOccurrenceCount).reversed())
            .limit(50)
            .collect(Collectors.toList());
    }

    private String buildGroupKey(LogEntry entry) {
        StringBuilder key = new StringBuilder();
        if (entry.getStatusCode() != null) key.append(entry.getStatusCode());
        if (entry.getHttpMethod() != null) key.append("|").append(entry.getHttpMethod());
        if (entry.getEndpointPattern() != null) key.append("|").append(entry.getEndpointPattern());
        if (entry.getExceptionType() != null) key.append("|").append(entry.getExceptionType());
        if (key.length() == 0) key.append(entry.getMessage() != null ?
            entry.getMessage().substring(0, Math.min(100, entry.getMessage().length())) : "unknown");
        return key.toString();
    }

    private String determineSeverity(List<LogEntry> group) {
        long count = group.size();
        boolean has5xx = group.stream().anyMatch(e -> e.getStatusCode() != null && e.getStatusCode() >= 500);
        boolean hasException = group.stream().anyMatch(e -> e.getExceptionType() != null);
        boolean hasTimeout = group.stream().anyMatch(e ->
            e.getExceptionType() != null && e.getExceptionType().contains("Timeout"));
        boolean has4xx = group.stream().anyMatch(e -> e.getStatusCode() != null &&
            e.getStatusCode() >= 400 && e.getStatusCode() < 500);
        boolean has429 = group.stream().anyMatch(e -> e.getStatusCode() != null && e.getStatusCode() == 429);

        if (has5xx && count >= CRITICAL_THRESHOLD) return "CRITICAL";
        if (hasException && count >= CRITICAL_THRESHOLD) return "CRITICAL";
        if (has5xx || (hasException && hasTimeout)) return "HIGH";
        if (has429) return "MEDIUM";
        if (has4xx) return "MEDIUM";
        if (count > 0) return "LOW";
        return "INFO";
    }

    private List<String> getSuggestionsForGroup(String groupKey, LogEntry representative) {
        List<String> suggestions = new ArrayList<>();
        Integer status = representative.getStatusCode();
        String exType = representative.getExceptionType();

        if (status != null) {
            switch (status) {
                case 401 -> suggestions.add("Check token validity and authentication headers");
                case 403 -> suggestions.add("Verify user permissions and authorization rules");
                case 404 -> suggestions.add("Verify endpoint paths, resource IDs, and routing");
                case 429 -> suggestions.add("Implement exponential backoff; review rate limit quotas");
                case 500 -> suggestions.add("Inspect server stack traces and validate inputs");
                case 502 -> suggestions.add("Check proxy and upstream service health");
                case 503 -> suggestions.add("Check service capacity and deployment health");
                case 504 -> suggestions.add("Review downstream latency and timeout configuration");
            }
        }

        if (exType != null) {
            if (exType.contains("NullPointer")) suggestions.add("Add null checks and validate object initialization");
            if (exType.contains("Timeout")) suggestions.add("Review timeout settings and add circuit breaker logic");
            if (exType.contains("ConnectionRefused")) suggestions.add("Verify host, port, and service availability");
            if (exType.contains("OutOfMemory")) suggestions.add("Profile heap usage and check for memory leaks");
        }

        if (suggestions.isEmpty()) suggestions.add("Review application logs for the root cause");
        return suggestions;
    }
}
