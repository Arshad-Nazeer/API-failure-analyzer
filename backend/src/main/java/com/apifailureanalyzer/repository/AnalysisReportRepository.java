package com.apifailureanalyzer.repository;

import com.apifailureanalyzer.model.AnalysisReport;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AnalysisReportRepository extends MongoRepository<AnalysisReport, String> {
    Page<AnalysisReport> findAllByOrderByCreatedAtDesc(Pageable pageable);
}
