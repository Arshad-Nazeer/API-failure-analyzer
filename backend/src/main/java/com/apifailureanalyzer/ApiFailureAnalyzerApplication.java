package com.apifailureanalyzer;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;

import java.nio.file.Files;
import java.nio.file.Path;

@SpringBootApplication
@EnableMongoRepositories
public class ApiFailureAnalyzerApplication {
    public static void main(String[] args) {
        loadDotenv();
        SpringApplication.run(ApiFailureAnalyzerApplication.class, args);
    }

    private static void loadDotenv() {
        Path[] candidatePaths = new Path[]{
            Path.of(".env"),
            Path.of("backend", ".env"),
            Path.of("..", "backend", ".env")
        };
        for (Path p : candidatePaths) {
            if (Files.exists(p)) {
                try {
                    for (String line : Files.readAllLines(p)) {
                        line = line.trim();
                        if (!line.isEmpty() && !line.startsWith("#") && line.contains("=")) {
                            int eq = line.indexOf('=');
                            String key = line.substring(0, eq).trim();
                            String val = line.substring(eq + 1).trim();
                            if (System.getProperty(key) == null && System.getenv(key) == null) {
                                System.setProperty(key, val);
                            }
                        }
                    }
                } catch (Exception ignored) {
                }
                break;
            }
        }
    }
}
