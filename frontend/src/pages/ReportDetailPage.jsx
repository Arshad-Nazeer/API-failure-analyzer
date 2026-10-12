import React, { useEffect, useState } from "react";
import { useParams, useNavigate } from "react-router-dom";
import { ArrowLeft, Trash2, AlertCircle } from "lucide-react";
import { analyzeApi } from "../services/api.js";
import AnalysisResults from "../components/AnalysisResults.jsx";
import Spinner from "../components/Spinner.jsx";
import { formatDate } from "../utils/formatters.js";

export default function ReportDetailPage() {
  const { id } = useParams();
  const navigate = useNavigate();
  const [report, setReport] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [deleting, setDeleting] = useState(false);

  useEffect(() => {
    analyzeApi.getReport(id)
      .then(setReport)
      .catch((e) => setError(e.message))
      .finally(() => setLoading(false));
  }, [id]);

  const handleDelete = async () => {
    if (!confirm(`Delete report "${report?.reportName}"?`)) return;
    setDeleting(true);
    try {
      await analyzeApi.deleteReport(id);
      navigate("/reports");
    } catch (e) {
      setError(e.message);
      setDeleting(false);
    }
  };

  if (loading) {
    return (
      <div className="flex items-center justify-center py-20">
        <Spinner size={28} className="text-accent-hover" />
      </div>
    );
  }

  if (error) {
    return (
      <div className="p-6">
        <div className="flex gap-3 items-start p-4 bg-red-500/10 border border-red-500/30 rounded-xl">
          <AlertCircle size={18} className="text-red-400 mt-0.5" />
          <p className="text-sm text-red-300">{error}</p>
        </div>
        <button className="btn-secondary flex items-center gap-2 mt-4" onClick={() => navigate("/reports")}>
          <ArrowLeft size={14} /> Back to Reports
        </button>
      </div>
    );
  }

  // Build an AnalysisResponse-compatible object from the report
  const analysisResult = report ? {
    totalLines: report.totalLines,
    parsedLines: report.parsedLines,
    unparsedLines: report.unparsedLines,
    totalErrors: report.totalErrors,
    totalWarnings: report.totalWarnings,
    totalHttpResponses: report.totalHttpResponses,
    statusCodeCounts: report.statusCodeCounts || {},
    statusGroupCounts: report.statusGroupCounts || {},
    endpointSummaries: report.endpointSummaries || [],
    groupedErrorPatterns: report.groupedErrorPatterns || [],
    recommendations: report.recommendations || [],
    unparsedLineSamples: report.unparsedLineSamples || [],
    analysisVersion: report.analysisVersion,
    warningMessage: null,
  } : null;

  return (
    <div className="p-6 max-w-7xl mx-auto space-y-6">
      <div className="flex items-start justify-between gap-4">
        <div>
          <button
            className="flex items-center gap-2 text-slate-500 hover:text-slate-300 text-sm mb-3 transition-colors"
            onClick={() => navigate("/reports")}
          >
            <ArrowLeft size={14} /> Back to Reports
          </button>
          <h1 className="text-2xl font-bold text-gradient">{report?.reportName}</h1>
          <p className="text-slate-500 text-sm mt-1">{formatDate(report?.createdAt)} · v{report?.analysisVersion}</p>
          <p className="text-xs text-slate-600 mt-1 font-mono">ID: {report?.id}</p>
        </div>
        <button
          id="detail-delete-btn"
          className="btn-danger flex items-center gap-2"
          onClick={handleDelete}
          disabled={deleting}
        >
          {deleting ? <Spinner size={14} /> : <Trash2 size={14} />}
          Delete
        </button>
      </div>

      {analysisResult && (
        <AnalysisResults result={analysisResult} onSave={null} saving={false} />
      )}
    </div>
  );
}
