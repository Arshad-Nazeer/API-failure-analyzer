package com.apifailureanalyzer.parser;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Multi-format log parser supporting:
 * 1. Common app log:  2026-10-13 10:42:12 ERROR ServiceName - Message
 * 2. HTTP access log: 2026-10-13T10:42:13Z GET /api/users 500 145ms
 * 3. JSON log:        {"timestamp":...,"level":...,"method":...,"endpoint":...,"status":...}
 * 4. Simple HTTP:     GET /api/products 404
 * 5. Exception lines: NullPointerException, Connection timed out, etc.
 */
@Component
@Slf4j
public class LogParser {

    private static final int MAX_UNPARSED_SAMPLES = 50;
    private final ObjectMapper objectMapper = new ObjectMapper();

    // Pattern 1: Common application log with datetime
    // 2026-10-13 10:42:12 ERROR PaymentService - Payment gateway timeout
    private static final Pattern APP_LOG_PATTERN = Pattern.compile(
        "^(\\d{4}-\\d{2}-\\d{2}[T ]\\d{2}:\\d{2}:\\d{2}(?:[.,]\\d+)?(?:Z|[+-]\\d{2}:?\\d{2})?)\\s+(ERROR|WARN|INFO|DEBUG|TRACE)\\s+([\\w.]+(?:\\[.*?\\])?)?\\s*[-:]?\\s*(.+)$",
        Pattern.CASE_INSENSITIVE
    );

    // Pattern 2: HTTP access log with timestamp
    // 2026-10-13T10:42:13Z GET /api/users 500 145ms
    private static final Pattern HTTP_ACCESS_WITH_TS = Pattern.compile(
        "^(\\d{4}-\\d{2}-\\d{2}[T ]\\d{2}:\\d{2}:\\d{2}(?:[.,]\\d+)?(?:Z|[+-]\\d{2}:?\\d{2})?)\\s+(GET|POST|PUT|DELETE|PATCH|HEAD|OPTIONS)\\s+(/\\S*)\\s+(\\d{3})(?:\\s+(\\d+)ms?)?.*$",
        Pattern.CASE_INSENSITIVE
    );

    // Pattern 3: Simple HTTP: GET /api/products 404
    private static final Pattern SIMPLE_HTTP_PATTERN = Pattern.compile(
        "^(GET|POST|PUT|DELETE|PATCH|HEAD|OPTIONS)\\s+(/\\S*)\\s+(\\d{3})(?:\\s+(\\d+)ms?)?.*$",
        Pattern.CASE_INSENSITIVE
    );

    // Pattern 4: App log with HTTP method embedded in message
    // 2026-10-13 10:42:12 ERROR Svc - POST /api/orders 503 2000ms
    private static final Pattern APP_LOG_WITH_HTTP = Pattern.compile(
        "^(\\d{4}-\\d{2}-\\d{2}[T ]\\d{2}:\\d{2}:\\d{2}(?:[.,]\\d+)?(?:Z|[+-]\\d{2}:?\\d{2})?)\\s+(ERROR|WARN|INFO|DEBUG|TRACE)\\s+([\\w.]+)?\\s*[-:]?\\s*(GET|POST|PUT|DELETE|PATCH|HEAD|OPTIONS)\\s+(/\\S*)\\s+(\\d{3})(?:\\s+(\\d+)ms?)?.*$",
        Pattern.CASE_INSENSITIVE
    );

    // Pattern 5: Exception patterns
    private static final Pattern EXCEPTION_PATTERN = Pattern.compile(
        ".*?([A-Z][a-zA-Z]*Exception|[A-Z][a-zA-Z]*Error|Connection (timed out|refused)|timeout|Unauthorized|Forbidden).*",
        Pattern.CASE_INSENSITIVE
    );

    // Pattern for extracting HTTP method and endpoint from message strings
    private static final Pattern HTTP_IN_MESSAGE = Pattern.compile(
        "(GET|POST|PUT|DELETE|PATCH|HEAD|OPTIONS)\\s+(/[^\\s]*)(?:\\s+(\\d{3}))?(?:\\s+(\\d+)ms?)?",
        Pattern.CASE_INSENSITIVE
    );

    // Pattern for UUID/numeric IDs in paths
    private static final Pattern PATH_ID_PATTERN = Pattern.compile(
        "/([0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}|\\d+)(?=[/\\s?#]|$)"
    );

    public ParseResult parse(String rawLogs) {
        if (rawLogs == null || rawLogs.isBlank()) {
            return ParseResult.builder()
                .entries(List.of())
                .unparsedLines(List.of())
                .totalLines(0)
                .parsedCount(0)
                .unparsedCount(0)
                .build();
        }

        String[] lines = rawLogs.split("\\r?\\n");
        List<LogEntry> entries = new ArrayList<>();
        List<String> unparsedSamples = new ArrayList<>();
        int totalLines = 0;
        int parsedCount = 0;
        int unparsedCount = 0;

        for (String rawLine : lines) {
            totalLines++;
            String line = rawLine.trim();
            if (line.isEmpty()) continue;

            LogEntry entry = parseLine(line, totalLines);
            if (entry.isParsed()) {
                entries.add(entry);
                parsedCount++;
            } else {
                unparsedCount++;
                if (unparsedSamples.size() < MAX_UNPARSED_SAMPLES) {
                    unparsedSamples.add(line);
                }
            }
        }

        return ParseResult.builder()
            .entries(entries)
            .unparsedLines(unparsedSamples)
            .totalLines(totalLines)
            .parsedCount(parsedCount)
            .unparsedCount(unparsedCount)
            .build();
    }

    private LogEntry parseLine(String line, int lineNumber) {
        // Try JSON first
        if (line.startsWith("{")) {
            LogEntry entry = tryParseJson(line, lineNumber);
            if (entry != null) return entry;
        }

        // Try app log with HTTP method in body
        Matcher m = APP_LOG_WITH_HTTP.matcher(line);
        if (m.matches()) {
            return LogEntry.builder()
                .lineNumber(lineNumber).rawLine(line).parsed(true)
                .timestamp(m.group(1))
                .level(m.group(2).toUpperCase())
                .serviceName(m.group(3))
                .httpMethod(m.group(4).toUpperCase())
                .endpoint(m.group(5))
                .endpointPattern(normalizeEndpoint(m.group(5)))
                .statusCode(Integer.parseInt(m.group(6)))
                .durationMs(m.group(7) != null ? Long.parseLong(m.group(7)) : null)
                .message(line)
                .build();
        }

        // Try HTTP access log with timestamp
        m = HTTP_ACCESS_WITH_TS.matcher(line);
        if (m.matches()) {
            return LogEntry.builder()
                .lineNumber(lineNumber).rawLine(line).parsed(true)
                .timestamp(m.group(1))
                .httpMethod(m.group(2).toUpperCase())
                .endpoint(m.group(3))
                .endpointPattern(normalizeEndpoint(m.group(3)))
                .statusCode(Integer.parseInt(m.group(4)))
                .durationMs(m.group(5) != null ? Long.parseLong(m.group(5)) : null)
                .level(deriveLevel(Integer.parseInt(m.group(4))))
                .message(line)
                .build();
        }

        // Try common app log
        m = APP_LOG_PATTERN.matcher(line);
        if (m.matches()) {
            String timestamp = m.group(1);
            String level = m.group(2).toUpperCase();
            String service = m.group(3);
            String message = m.group(4);

            // Check if the message contains HTTP info
            Matcher httpM = HTTP_IN_MESSAGE.matcher(message);
            if (httpM.find()) {
                Integer status = httpM.group(3) != null ? Integer.parseInt(httpM.group(3)) : null;
                Long duration = httpM.group(4) != null ? Long.parseLong(httpM.group(4)) : null;
                return LogEntry.builder()
                    .lineNumber(lineNumber).rawLine(line).parsed(true)
                    .timestamp(timestamp).level(level).serviceName(service)
                    .httpMethod(httpM.group(1).toUpperCase())
                    .endpoint(httpM.group(2))
                    .endpointPattern(normalizeEndpoint(httpM.group(2)))
                    .statusCode(status)
                    .durationMs(duration)
                    .message(message)
                    .exceptionType(extractExceptionType(message))
                    .build();
            }

            return LogEntry.builder()
                .lineNumber(lineNumber).rawLine(line).parsed(true)
                .timestamp(timestamp).level(level).serviceName(service)
                .message(message)
                .exceptionType(extractExceptionType(message))
                .build();
        }

        // Try simple HTTP: GET /api/users 404
        m = SIMPLE_HTTP_PATTERN.matcher(line);
        if (m.matches()) {
            return LogEntry.builder()
                .lineNumber(lineNumber).rawLine(line).parsed(true)
                .httpMethod(m.group(1).toUpperCase())
                .endpoint(m.group(2))
                .endpointPattern(normalizeEndpoint(m.group(2)))
                .statusCode(Integer.parseInt(m.group(3)))
                .durationMs(m.group(4) != null ? Long.parseLong(m.group(4)) : null)
                .level(deriveLevel(Integer.parseInt(m.group(3))))
                .message(line)
                .build();
        }

        // Try exception/error-only lines
        m = EXCEPTION_PATTERN.matcher(line);
        if (m.matches()) {
            return LogEntry.builder()
                .lineNumber(lineNumber).rawLine(line).parsed(true)
                .level("ERROR")
                .message(line)
                .exceptionType(extractExceptionType(line))
                .build();
        }

        // Unparsed
        return LogEntry.builder()
            .lineNumber(lineNumber).rawLine(line).parsed(false)
            .message(line)
            .build();
    }

    private LogEntry tryParseJson(String line, int lineNumber) {
        try {
            JsonNode node = objectMapper.readTree(line);
            String timestamp = getJsonStr(node, "timestamp", "time", "ts", "@timestamp");
            String level = getJsonStr(node, "level", "severity", "log_level");
            String method = getJsonStr(node, "method", "httpMethod", "http_method");
            String endpoint = getJsonStr(node, "endpoint", "path", "url", "uri", "request_uri");
            String message = getJsonStr(node, "message", "msg", "error", "exception");
            Integer status = node.has("status") ? node.get("status").asInt() : 
                             node.has("statusCode") ? node.get("statusCode").asInt() :
                             node.has("http_status") ? node.get("http_status").asInt() : null;
            Long duration = node.has("durationMs") ? node.get("durationMs").asLong() :
                            node.has("duration_ms") ? node.get("duration_ms").asLong() :
                            node.has("responseTime") ? node.get("responseTime").asLong() : null;

            if (level == null && status == null && method == null && endpoint == null && message == null) {
                return null;
            }

            String resolvedLevel = level != null ? level.toUpperCase() : 
                                   (status != null ? deriveLevel(status) : "INFO");

            return LogEntry.builder()
                .lineNumber(lineNumber).rawLine(line).parsed(true)
                .timestamp(timestamp)
                .level(resolvedLevel)
                .httpMethod(method != null ? method.toUpperCase() : null)
                .endpoint(endpoint)
                .endpointPattern(endpoint != null ? normalizeEndpoint(endpoint) : null)
                .statusCode(status)
                .durationMs(duration)
                .message(message != null ? message : line)
                .exceptionType(extractExceptionType(message != null ? message : ""))
                .build();
        } catch (Exception e) {
            return null;
        }
    }

    private String getJsonStr(JsonNode node, String... keys) {
        for (String key : keys) {
            if (node.has(key) && !node.get(key).isNull()) {
                return node.get(key).asText();
            }
        }
        return null;
    }

    public static String normalizeEndpoint(String endpoint) {
        if (endpoint == null) return null;
        // Remove query string
        int queryIdx = endpoint.indexOf('?');
        if (queryIdx > 0) endpoint = endpoint.substring(0, queryIdx);
        // Replace UUIDs
        endpoint = endpoint.replaceAll(
            "[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}",
            "{id}"
        );
        // Replace numeric path segments
        endpoint = endpoint.replaceAll("/(\\d+)(?=[/\\s]|$)", "/{id}");
        return endpoint;
    }

    private String extractExceptionType(String message) {
        if (message == null) return null;
        Pattern p = Pattern.compile("([A-Z][a-zA-Z]*(?:Exception|Error))");
        Matcher m = p.matcher(message);
        if (m.find()) return m.group(1);
        if (message.toLowerCase().contains("connection timed out") || 
            message.toLowerCase().contains("connection timeout")) return "ConnectionTimeoutError";
        if (message.toLowerCase().contains("connection refused")) return "ConnectionRefusedError";
        if (message.toLowerCase().contains("timeout")) return "TimeoutError";
        return null;
    }

    private String deriveLevel(int statusCode) {
        if (statusCode >= 500) return "ERROR";
        if (statusCode >= 400) return "WARN";
        if (statusCode >= 300) return "INFO";
        return "INFO";
    }
}
