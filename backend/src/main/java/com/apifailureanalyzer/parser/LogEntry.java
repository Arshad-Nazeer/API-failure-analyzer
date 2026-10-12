package com.apifailureanalyzer.parser;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class LogEntry {
    private int lineNumber;
    private String rawLine;
    private String timestamp;
    private String level;       // ERROR, WARN, INFO, DEBUG
    private String httpMethod;  // GET, POST, etc.
    private String endpoint;    // /api/users/123
    private String endpointPattern; // /api/users/{id}
    private Integer statusCode;
    private Long durationMs;
    private String message;
    private String exceptionType;
    private String serviceName;
    private boolean parsed;
}
