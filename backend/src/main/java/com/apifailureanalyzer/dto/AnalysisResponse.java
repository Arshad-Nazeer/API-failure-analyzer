package com.apifailureanalyzer.dto;

import com.apifailureanalyzer.model.AnalysisReport;
import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
@Builder
public class AnalysisResponse {
    private int totalLines;
    private int parsedLines;
    private int unparsedLines;
    private int totalHttpResponses;
    private int totalErrors;
    private int totalWarnings;
    private Map<String, Long> statusCodeCounts;
    private Map<String, Long> statusGroupCounts;
    private List<AnalysisReport.EndpointSummary> endpointSummaries;
    private List<AnalysisReport.ErrorGroup> groupedErrorPatterns;
    private List<String> recommendations;
    private List<String> unparsedLineSamples;
    private String analysisVersion;
    private String warningMessage;
}
