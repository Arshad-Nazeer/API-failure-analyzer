package com.apifailureanalyzer;

import com.apifailureanalyzer.parser.LogEntry;
import com.apifailureanalyzer.parser.LogParser;
import com.apifailureanalyzer.parser.ParseResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class LogParserTest {

    private LogParser parser;

    @BeforeEach
    void setUp() {
        parser = new LogParser();
    }

    @Test
    @DisplayName("Parse empty input returns empty result")
    void testEmptyInput() {
        ParseResult result = parser.parse("");
        assertThat(result.getTotalLines()).isEqualTo(0);
        assertThat(result.getEntries()).isEmpty();
    }

    @Test
    @DisplayName("Parse null input returns empty result")
    void testNullInput() {
        ParseResult result = parser.parse(null);
        assertThat(result.getEntries()).isEmpty();
    }

    @Test
    @DisplayName("Parse common app log format")
    void testAppLogFormat() {
        String log = "2026-10-13 10:42:12 ERROR PaymentService - Payment gateway timeout";
        ParseResult result = parser.parse(log);
        assertThat(result.getParsedCount()).isEqualTo(1);
        LogEntry entry = result.getEntries().get(0);
        assertThat(entry.getLevel()).isEqualTo("ERROR");
        assertThat(entry.getServiceName()).isEqualTo("PaymentService");
        assertThat(entry.getMessage()).contains("Payment gateway timeout");
    }

    @Test
    @DisplayName("Parse HTTP access log with timestamp")
    void testHttpAccessLogWithTimestamp() {
        String log = "2026-10-13T10:42:13Z GET /api/users 500 145ms";
        ParseResult result = parser.parse(log);
        assertThat(result.getParsedCount()).isEqualTo(1);
        LogEntry entry = result.getEntries().get(0);
        assertThat(entry.getHttpMethod()).isEqualTo("GET");
        assertThat(entry.getEndpoint()).isEqualTo("/api/users");
        assertThat(entry.getStatusCode()).isEqualTo(500);
        assertThat(entry.getDurationMs()).isEqualTo(145L);
    }

    @Test
    @DisplayName("Parse JSON log format")
    void testJsonLogFormat() {
        String log = "{\"timestamp\":\"2026-10-13T10:42:14Z\",\"level\":\"ERROR\",\"method\":\"POST\",\"endpoint\":\"/api/orders\",\"status\":503,\"durationMs\":2000,\"message\":\"Service unavailable\"}";
        ParseResult result = parser.parse(log);
        assertThat(result.getParsedCount()).isEqualTo(1);
        LogEntry entry = result.getEntries().get(0);
        assertThat(entry.getHttpMethod()).isEqualTo("POST");
        assertThat(entry.getEndpoint()).isEqualTo("/api/orders");
        assertThat(entry.getStatusCode()).isEqualTo(503);
        assertThat(entry.getDurationMs()).isEqualTo(2000L);
        assertThat(entry.getLevel()).isEqualTo("ERROR");
    }

    @Test
    @DisplayName("Parse simple HTTP format GET /api/products 404")
    void testSimpleHttpFormat() {
        String log = "GET /api/products 404";
        ParseResult result = parser.parse(log);
        assertThat(result.getParsedCount()).isEqualTo(1);
        LogEntry entry = result.getEntries().get(0);
        assertThat(entry.getHttpMethod()).isEqualTo("GET");
        assertThat(entry.getStatusCode()).isEqualTo(404);
    }

    @ParameterizedTest(name = "Parse simple HTTP: {0}")
    @CsvSource({
        "GET /api/products 404, GET, 404",
        "POST /api/orders 500, POST, 500",
        "DELETE /api/users 401, DELETE, 401",
        "PUT /api/items 200, PUT, 200"
    })
    void testSimpleHttpVariants(String log, String method, int status) {
        ParseResult result = parser.parse(log);
        assertThat(result.getParsedCount()).isEqualTo(1);
        assertThat(result.getEntries().get(0).getHttpMethod()).isEqualTo(method);
        assertThat(result.getEntries().get(0).getStatusCode()).isEqualTo(status);
    }

    @Test
    @DisplayName("Parse mixed format logs")
    void testMixedFormats() {
        String logs = "2026-10-13 10:42:12 ERROR PaymentService - Payment gateway timeout\n" +
                      "GET /api/users 500 145ms\n" +
                      "{\"timestamp\":\"2026-10-13T10:42:14Z\",\"level\":\"ERROR\",\"method\":\"POST\",\"endpoint\":\"/api/orders\",\"status\":503,\"durationMs\":2000,\"message\":\"Service unavailable\"}\n" +
                      "POST /api/orders 500\n" +
                      "NullPointerException at PaymentService.java:42";
        ParseResult result = parser.parse(logs);
        assertThat(result.getTotalLines()).isEqualTo(5);
        assertThat(result.getParsedCount()).isGreaterThanOrEqualTo(4);
    }

    @Test
    @DisplayName("Unrecognized lines are counted as unparsed")
    void testUnrecognizedLines() {
        String logs = "this is not a log line at all\nrandom text without structure";
        ParseResult result = parser.parse(logs);
        assertThat(result.getUnparsedCount()).isEqualTo(2);
        assertThat(result.getUnparsedLines()).hasSize(2);
    }

    @Test
    @DisplayName("Endpoint normalization replaces IDs with {id}")
    void testEndpointNormalization() {
        assertThat(LogParser.normalizeEndpoint("/api/users/123")).isEqualTo("/api/users/{id}");
        assertThat(LogParser.normalizeEndpoint("/api/users/456")).isEqualTo("/api/users/{id}");
        assertThat(LogParser.normalizeEndpoint("/api/products/abc123def456")).isEqualTo("/api/products/abc123def456");
    }

    @Test
    @DisplayName("UUID normalization in endpoints")
    void testUuidNormalization() {
        String path = "/api/orders/550e8400-e29b-41d4-a716-446655440000";
        assertThat(LogParser.normalizeEndpoint(path)).isEqualTo("/api/orders/{id}");
    }

    @Test
    @DisplayName("Exception type extraction")
    void testExceptionExtraction() {
        String log = "2026-10-13 10:42:12 ERROR PaymentService - NullPointerException at PaymentService.java:42";
        ParseResult result = parser.parse(log);
        assertThat(result.getParsedCount()).isEqualTo(1);
        assertThat(result.getEntries().get(0).getExceptionType()).isEqualTo("NullPointerException");
    }
}
