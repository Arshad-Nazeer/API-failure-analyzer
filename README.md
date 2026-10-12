# ⚡ API Failure Analyzer

A full-stack developer observability tool built with **Spring Boot 3**, **MongoDB Atlas**, and **React 18 + Vite + Tailwind CSS**. It analyzes API logs in real-time, extracts HTTP error patterns, normalizes dynamic endpoints, groups failures, and provides actionable root-cause recommendations.

---

## 🚀 Features

- **Multi-Format Log Parser**: Supports Standard HTTP logs, Spring Boot logs, Nginx access logs, and JSON logs.
- **Dynamic Endpoint Normalization**: Replaces UUIDs, IDs, hashes, and dates with clean patterns (e.g., `/api/v1/users/42` → `/api/v1/users/{id}`).
- **Pattern & Severity Grouping**: Automatically groups recurring errors by method, endpoint, and status code with severity tagging (`HIGH`, `MEDIUM`, `LOW`).
- **Recommendation Engine**: Heuristic root-cause guidance for HTTP 4xx, 5xx, timeouts, connection refused, out-of-memory, and rate limiting.
- **MongoDB Atlas Persistence**: Save, query, view, and delete historical failure reports.
- **Interactive UI**:
  - Live log paste with 1-click sample presets (E-Commerce Incident, Auth Failure Storm, Microservices Outage).
  - Drag-and-drop log file upload (`.log`, `.txt`, `.json`).
  - Charts powered by Recharts (Status code distribution pie chart, top failing endpoints bar chart).
  - Reports management with pagination and search.
  - Actuator and Database health indicator pill in the navbar.

---

## 🛠️ Tech Stack

- **Backend**: Java 17, Spring Boot 3.3.4, Spring Data MongoDB, Jakarta Validation, Spring Boot Actuator, JUnit 5 & Mockito.
- **Database**: MongoDB Atlas (`Cluster0`, AWS `ap-south-1`).
- **Frontend**: React 18, Vite, Tailwind CSS, Lucide React, Recharts, Axios, React Router v6.

---

## 📦 Project Structure

```
sp_api/
├── backend/
│   ├── src/main/java/com/apifailureanalyzer/
│   │   ├── config/          # CORS & Web configs
│   │   ├── controller/      # AnalysisController, HealthController
│   │   ├── dto/             # Request & Response DTOs
│   │   ├── exception/       # GlobalExceptionHandler & custom exceptions
│   │   ├── model/           # MongoDB AnalysisReport document
│   │   ├── parser/          # Regex LogParser, LogEntry, ParseResult
│   │   ├── recommendation/  # RecommendationEngine
│   │   ├── repository/      # Spring Data AnalysisReportRepository
│   │   └── service/         # LogAnalysisService, ReportService
│   ├── src/main/resources/  # application.properties
│   └── pom.xml
├── frontend/
│   ├── src/
│   │   ├── components/      # Layout, StatCard, Charts, Spinner, EmptyState
│   │   ├── pages/           # OverviewPage, AnalyzePage, ReportsPage, ReportDetailPage, AboutPage
│   │   ├── services/        # api.js (Axios API client)
│   │   └── utils/           # formatters.js, sampleLogs.js
│   └── package.json
└── README.md
```

---

## 🚦 Quick Start

### Prerequisites
- **Java 17+** (JDK installed)
- **Node.js 18+** & npm

### 1. Backend

```bash
cd backend
# Set JAVA_HOME if needed
$env:JAVA_HOME = "C:\Program Files\Java\jdk-17"
$env:PATH = "$env:JAVA_HOME\bin;$env:PATH"

# Run tests
.\mvnw.cmd test

# Run application
.\mvnw.cmd spring-boot:run
```
Backend runs on **http://localhost:8080**.

### 2. Frontend

```bash
cd frontend
npm install
npm run dev
```
Frontend runs on **http://localhost:5173**.

---

## 📡 Key API Endpoints

| Method | Endpoint | Description |
|---|---|---|
| `GET` | `/api/health` | Health status and DB connectivity check |
| `POST` | `/api/analysis/analyze` | Analyzes raw text logs |
| `POST` | `/api/analysis/upload` | Multipart file upload analysis |
| `POST` | `/api/analysis/reports` | Saves an analysis report to MongoDB |
| `GET` | `/api/analysis/reports` | Paginated list of saved reports |
| `GET` | `/api/analysis/reports/{id}` | Fetches report by ID |
| `DELETE`| `/api/analysis/reports/{id}` | Deletes a report |
