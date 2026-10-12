import React, { useState, useRef, useCallback } from "react";
import {
  Upload, FlaskConical, X, FileText, AlertCircle, CheckCircle
} from "lucide-react";
import { analyzeApi } from "../services/api.js";
import { SAMPLE_LOGS, SAMPLE_LOG_DESCRIPTION } from "../utils/sampleLogs.js";
import AnalysisResults from "../components/AnalysisResults.jsx";
import Spinner from "../components/Spinner.jsx";

const ALLOWED_TYPES = ["text/plain", "application/json", "text/x-log", ""];
const ALLOWED_EXTS = [".log", ".txt", ".json"];

export default function AnalyzePage() {
  const [logText, setLogText] = useState("");
  const [reportName, setReportName] = useState("");
  const [result, setResult] = useState(null);
  const [loading, setLoading] = useState(false);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState(null);
  const [success, setSuccess] = useState(null);
  const [dragOver, setDragOver] = useState(false);
  const [uploadedFile, setUploadedFile] = useState(null);
  const fileRef = useRef(null);

  const validateFile = (file) => {
    if (!file) return "No file selected.";
    const ext = "." + file.name.split(".").pop().toLowerCase();
    if (!ALLOWED_EXTS.includes(ext))
      return `Unsupported file type "${ext}". Allowed: .log, .txt, .json`;
    if (file.size > 5 * 1024 * 1024)
      return "File too large. Maximum size is 5 MB.";
    return null;
  };

  const handleFileSelect = (file) => {
    const err = validateFile(file);
    if (err) { setError(err); return; }
    setUploadedFile(file);
    setError(null);
    setResult(null);
    setLogText("");
  };

  const handleFileDrop = useCallback((e) => {
    e.preventDefault();
    setDragOver(false);
    const file = e.dataTransfer.files[0];
    if (file) handleFileSelect(file);
  }, []);

  const handleAnalyze = async () => {
    setError(null);
    setSuccess(null);
    setResult(null);

    if (uploadedFile) {
      setLoading(true);
      try {
        const res = await analyzeApi.upload(uploadedFile, reportName || uploadedFile.name);
        setResult(res);
      } catch (e) {
        setError(e.message);
      } finally {
        setLoading(false);
      }
      return;
    }

    if (!logText.trim()) {
      setError("Please paste some log content or upload a file.");
      return;
    }

    setLoading(true);
    try {
      const res = await analyzeApi.analyze(logText, reportName);
      setResult(res);
    } catch (e) {
      setError(e.message);
    } finally {
      setLoading(false);
    }
  };

  const handleSave = async () => {
    if (!result) return;
    setSaving(true);
    setSuccess(null);
    setError(null);
    try {
      const saved = await analyzeApi.saveReport(reportName || "Unnamed Report", result);
      setSuccess(`Report saved successfully! ID: ${saved.id}`);
    } catch (e) {
      setError("Failed to save report: " + e.message);
    } finally {
      setSaving(false);
    }
  };

  const handleClear = () => {
    setLogText(""); setUploadedFile(null); setResult(null);
    setError(null); setSuccess(null);
  };

  return (
    <div className="p-6 max-w-7xl mx-auto space-y-6">
      {/* Header */}
      <div>
        <h1 className="text-2xl font-bold text-gradient">Analyze Logs</h1>
        <p className="text-slate-500 text-sm mt-1">
          Paste log content or upload a file to detect failures, group errors, and get recommendations.
        </p>
      </div>

      {/* Input section */}
      <div className="grid grid-cols-1 lg:grid-cols-3 gap-4">
        <div className="lg:col-span-2 space-y-3">
          {/* Report name */}
          <input
            id="report-name-input"
            type="text"
            placeholder="Report name (optional)"
            value={reportName}
            onChange={(e) => setReportName(e.target.value)}
            className="w-full bg-dark-800/70 border border-white/10 rounded-lg px-4 py-2.5 text-sm
              text-slate-200 placeholder-slate-600 focus:outline-none focus:border-accent-primary/50
              focus:ring-1 focus:ring-accent-primary/20 transition-all"
          />

          {/* Textarea */}
          {!uploadedFile && (
            <textarea
              id="log-input"
              className="code-area h-72"
              placeholder={"Paste your API logs here...\n\nExamples:\n2026-10-13 10:42:12 ERROR PaymentService - Timeout\nGET /api/users 500 145ms\n{\"level\":\"ERROR\",\"status\":503}"}
              value={logText}
              onChange={(e) => setLogText(e.target.value)}
            />
          )}

          {/* Uploaded file display */}
          {uploadedFile && (
            <div className="glass-card p-4 flex items-center gap-3">
              <FileText size={20} className="text-accent-hover" />
              <div className="flex-1 min-w-0">
                <p className="text-sm font-medium text-slate-200 truncate">{uploadedFile.name}</p>
                <p className="text-xs text-slate-500">
                  {(uploadedFile.size / 1024).toFixed(1)} KB
                </p>
              </div>
              <button
                className="text-slate-500 hover:text-red-400 transition-colors"
                onClick={() => { setUploadedFile(null); setResult(null); }}
              >
                <X size={16} />
              </button>
            </div>
          )}
        </div>

        {/* Right panel */}
        <div className="space-y-3">
          {/* Drop zone */}
          <div
            id="drop-zone"
            className={`glass-card p-5 border-2 border-dashed transition-all duration-200 cursor-pointer
              flex flex-col items-center gap-3 text-center
              ${dragOver ? "border-accent-primary/60 bg-accent-primary/5" : "border-white/10 hover:border-white/20"}`}
            onDragOver={(e) => { e.preventDefault(); setDragOver(true); }}
            onDragLeave={() => setDragOver(false)}
            onDrop={handleFileDrop}
            onClick={() => fileRef.current?.click()}
          >
            <Upload size={24} className={dragOver ? "text-accent-hover" : "text-slate-500"} />
            <div>
              <p className="text-sm font-medium text-slate-300">Drop file here</p>
              <p className="text-xs text-slate-500 mt-1">or click to browse</p>
              <p className="text-xs text-slate-600 mt-1">.log, .txt, .json · max 5 MB</p>
            </div>
            <input
              ref={fileRef}
              type="file"
              accept=".log,.txt,.json"
              className="hidden"
              onChange={(e) => e.target.files?.[0] && handleFileSelect(e.target.files[0])}
            />
          </div>

          {/* Sample log button */}
          <button
            id="load-sample-btn"
            className="btn-secondary w-full flex items-center justify-center gap-2 text-sm"
            onClick={() => { setLogText(SAMPLE_LOGS); setUploadedFile(null); setResult(null); }}
          >
            <FlaskConical size={16} />
            Load Sample Logs
          </button>
          <p className="text-xs text-slate-600 px-1">{SAMPLE_LOG_DESCRIPTION}</p>
        </div>
      </div>

      {/* Action bar */}
      <div className="flex gap-3 items-center">
        <button
          id="analyze-btn"
          className="btn-primary flex items-center gap-2"
          onClick={handleAnalyze}
          disabled={loading || (!logText.trim() && !uploadedFile)}
        >
          {loading ? <Spinner size={16} /> : <FlaskConical size={16} />}
          {loading ? "Analyzing..." : "Analyze"}
        </button>
        {(logText || uploadedFile || result) && (
          <button className="btn-secondary text-sm" onClick={handleClear}>
            Clear
          </button>
        )}
      </div>

      {/* Alerts */}
      {error && (
        <div className="flex gap-3 items-start p-4 bg-red-500/10 border border-red-500/30 rounded-xl">
          <AlertCircle size={18} className="text-red-400 mt-0.5 shrink-0" />
          <p className="text-sm text-red-300">{error}</p>
        </div>
      )}
      {success && (
        <div className="flex gap-3 items-start p-4 bg-green-500/10 border border-green-500/30 rounded-xl">
          <CheckCircle size={18} className="text-green-400 mt-0.5 shrink-0" />
          <p className="text-sm text-green-300">{success}</p>
        </div>
      )}

      {/* Results */}
      {result && (
        <AnalysisResults result={result} onSave={handleSave} saving={saving} />
      )}
    </div>
  );
}
