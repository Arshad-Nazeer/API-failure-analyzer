package com.apifailureanalyzer.analyzer;

import com.apifailureanalyzer.model.AnalysisReport;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Deterministic rule-based recommendation engine.
 * Maps error patterns to possible causes and suggested actions.
 */
@Component
public class RecommendationEngine {

    public List<String> generateRecommendations(
            Map<String, Long> statusCodeCounts,
            List<AnalysisReport.ErrorGroup> errorGroups,
            List<AnalysisReport.EndpointSummary> endpointSummaries) {

        List<String> recommendations = new ArrayList<>();

        // HTTP status code based recommendations
        addStatusCodeRecommendations(statusCodeCounts, recommendations);

        // Error group based recommendations
        addErrorGroupRecommendations(errorGroups, recommendations);

        // Endpoint performance recommendations
        addEndpointRecommendations(endpointSummaries, recommendations);

        // De-duplicate while preserving order
        return recommendations.stream().distinct().toList();
    }

    private void addStatusCodeRecommendations(Map<String, Long> statusCodeCounts, List<String> recs) {
        long count401 = statusCodeCounts.getOrDefault("401", 0L);
        long count403 = statusCodeCounts.getOrDefault("403", 0L);
        long count404 = statusCodeCounts.getOrDefault("404", 0L);
        long count429 = statusCodeCounts.getOrDefault("429", 0L);
        long count500 = statusCodeCounts.getOrDefault("500", 0L);
        long count502 = statusCodeCounts.getOrDefault("502", 0L);
        long count503 = statusCodeCounts.getOrDefault("503", 0L);
        long count504 = statusCodeCounts.getOrDefault("504", 0L);

        if (count401 > 0) {
            recs.add("[HTTP 401] " + count401 + " unauthorized request(s) detected. " +
                "Check token validity, expiry, and authentication headers. " +
                "Verify the Authorization header is sent with each request.");
        }
        if (count403 > 0) {
            recs.add("[HTTP 403] " + count403 + " forbidden request(s) detected. " +
                "Verify authorization rules and user permissions. " +
                "Confirm the authenticated user has appropriate roles for the requested resources.");
        }
        if (count404 > 0) {
            recs.add("[HTTP 404] " + count404 + " not-found response(s) detected. " +
                "Check endpoint paths, resource IDs, and routing configuration. " +
                "Ensure the requested resources exist and IDs are valid.");
        }
        if (count429 > 0) {
            recs.add("[HTTP 429] " + count429 + " rate-limit response(s) detected. " +
                "Inspect API quotas and implement exponential backoff with jitter. " +
                "Consider request batching to reduce call frequency.");
        }
        if (count500 > 0) {
            recs.add("[HTTP 500] " + count500 + " internal server error(s) detected. " +
                "Inspect server stack traces and application logs for the root cause. " +
                "Ensure all inputs are validated server-side and null references are handled.");
        }
        if (count502 > 0) {
            recs.add("[HTTP 502] " + count502 + " bad-gateway response(s) detected. " +
                "Check proxy configuration and upstream service health. " +
                "Verify the upstream service is running and accessible from the proxy.");
        }
        if (count503 > 0) {
            recs.add("[HTTP 503] " + count503 + " service-unavailable response(s) detected. " +
                "Inspect service health, deployment status, and capacity. " +
                "Check for resource exhaustion (CPU, memory, connections) and scale if needed.");
        }
        if (count504 > 0) {
            recs.add("[HTTP 504] " + count504 + " gateway-timeout response(s) detected. " +
                "Inspect downstream service latency and timeout configuration. " +
                "Review database query performance and external service response times.");
        }
    }

    private void addErrorGroupRecommendations(List<AnalysisReport.ErrorGroup> groups, List<String> recs) {
        if (groups == null) return;
        for (AnalysisReport.ErrorGroup group : groups) {
            String errorType = group.getErrorType();
            if (errorType == null) continue;

            if (errorType.contains("NullPointerException")) {
                recs.add("[NullPointerException] Unexpected null reference detected " + group.getOccurrenceCount() + " time(s). " +
                    "Inspect the stack trace, validate object initialization, and add null checks or use Optional.");
            } else if (errorType.contains("ConnectionTimeout") || errorType.contains("TimeoutError")) {
                recs.add("[Timeout] Connection/request timeout detected " + group.getOccurrenceCount() + " time(s). " +
                    "Inspect network connectivity, downstream service latency, and timeout settings. " +
                    "Consider implementing circuit breakers and retry logic.");
            } else if (errorType.contains("ConnectionRefused")) {
                recs.add("[Connection Refused] Target service not accepting connections " + group.getOccurrenceCount() + " time(s). " +
                    "Verify host, port, firewall rules, listener configuration, and service availability.");
            } else if (errorType.contains("OutOfMemoryError")) {
                recs.add("[OutOfMemoryError] JVM heap exhausted " + group.getOccurrenceCount() + " time(s). " +
                    "Review heap allocation (-Xmx), check for memory leaks, and profile object retention.");
            } else if (errorType.contains("StackOverflowError")) {
                recs.add("[StackOverflowError] Excessive recursion detected " + group.getOccurrenceCount() + " time(s). " +
                    "Check for infinite recursion in the call stack and convert to iterative logic where possible.");
            }
        }
    }

    private void addEndpointRecommendations(List<AnalysisReport.EndpointSummary> summaries, List<String> recs) {
        if (summaries == null) return;
        for (AnalysisReport.EndpointSummary summary : summaries) {
            if (summary.getSlowRequestCount() > 0) {
                recs.add("[Slow Endpoint] " + summary.getEndpointPattern() + " had " +
                    summary.getSlowRequestCount() + " slow request(s)" +
                    (summary.getAvgDurationMs() != null ? " (avg " + Math.round(summary.getAvgDurationMs()) + "ms)" : "") +
                    ". Review database queries, caching, and resource-intensive operations for this endpoint.");
            }
            if (summary.getFailureCount() >= 5) {
                recs.add("[Frequent Failures] " + summary.getEndpointPattern() + " failed " +
                    summary.getFailureCount() + " time(s). " +
                    "This endpoint may be unstable. Check service dependencies and error handling.");
            }
        }
    }
}
