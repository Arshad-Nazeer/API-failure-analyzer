package com.apifailureanalyzer.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class SaveReportRequest {
    @Size(max = 200, message = "Report name too long")
    private String reportName;

    @NotNull(message = "Analysis result is required")
    private AnalysisResponse analysisResult;
}
