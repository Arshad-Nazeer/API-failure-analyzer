package com.apifailureanalyzer.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class AnalyzeRequest {

    @NotBlank(message = "Log content must not be blank")
    @Size(max = 5_242_880, message = "Log content exceeds maximum allowed size")
    private String logs;

    @Size(max = 200, message = "Report name too long")
    private String name;
}
