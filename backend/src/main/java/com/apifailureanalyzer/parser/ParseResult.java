package com.apifailureanalyzer.parser;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class ParseResult {
    private List<LogEntry> entries;
    private List<String> unparsedLines;
    private int totalLines;
    private int parsedCount;
    private int unparsedCount;
}
