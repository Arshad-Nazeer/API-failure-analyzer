package com.apifailureanalyzer.controller;

import com.apifailureanalyzer.dto.AnalysisResponse;
import com.apifailureanalyzer.dto.AnalyzeRequest;
import com.apifailureanalyzer.dto.SaveReportRequest;
import com.apifailureanalyzer.exception.BadRequestException;
import com.apifailureanalyzer.model.AnalysisReport;
import com.apifailureanalyzer.service.LogAnalysisService;
import com.apifailureanalyzer.service.ReportService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Set;

@RestController
@RequestMapping("/api/analysis")
@RequiredArgsConstructor
@Slf4j
public class AnalysisController {

    private final LogAnalysisService analysisService;
    private final ReportService reportService;

    @Value("${app.max-upload-size-mb:5}")
    private int maxUploadSizeMb;

    private static final Set<String> ALLOWED_EXTENSIONS = Set.of("log", "txt", "json");

    @PostMapping("/analyze")
    public ResponseEntity<AnalysisResponse> analyze(@Valid @RequestBody AnalyzeRequest request) {
        log.info("Analyze request: name={}", request.getName());
        AnalysisResponse response = analysisService.analyze(request.getLogs());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/upload")
    public ResponseEntity<AnalysisResponse> upload(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "name", required = false) String name) throws IOException {

        if (file.isEmpty()) throw new BadRequestException("Uploaded file is empty.");

        long maxBytes = (long) maxUploadSizeMb * 1024 * 1024;
        if (file.getSize() > maxBytes) {
            throw new BadRequestException("File exceeds maximum allowed size of " + maxUploadSizeMb + " MB.");
        }

        String originalName = file.getOriginalFilename();
        String ext = "";
        if (originalName != null && originalName.contains(".")) {
            ext = originalName.substring(originalName.lastIndexOf('.') + 1).toLowerCase();
        }
        if (!ALLOWED_EXTENSIONS.contains(ext)) {
            throw new BadRequestException("Unsupported file type. Allowed: .log, .txt, .json");
        }

        String content = new String(file.getBytes(), StandardCharsets.UTF_8);
        log.info("Upload request: filename={}, size={}", ext, file.getSize());

        AnalysisResponse response = analysisService.analyze(content);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/reports")
    public ResponseEntity<AnalysisReport> saveReport(@Valid @RequestBody SaveReportRequest request) {
        AnalysisReport saved = reportService.saveReport(request.getReportName(), request.getAnalysisResult());
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    @GetMapping("/reports")
    public ResponseEntity<Page<AnalysisReport>> listReports(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        PageRequest pageable = PageRequest.of(page, Math.min(size, 50),
            Sort.by(Sort.Direction.DESC, "createdAt"));
        return ResponseEntity.ok(reportService.listReports(pageable));
    }

    @GetMapping("/reports/{id}")
    public ResponseEntity<AnalysisReport> getReport(@PathVariable String id) {
        return ResponseEntity.ok(reportService.getReport(id));
    }

    @DeleteMapping("/reports/{id}")
    public ResponseEntity<Void> deleteReport(@PathVariable String id) {
        reportService.deleteReport(id);
        return ResponseEntity.noContent().build();
    }
}
