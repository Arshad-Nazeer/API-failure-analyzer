import React, { useEffect, useState, useCallback } from "react";
import { useNavigate } from "react-router-dom";
import { BookOpen, Trash2, Eye, RefreshCw, AlertCircle, ChevronLeft, ChevronRight } from "lucide-react";
import { analyzeApi } from "../services/api.js";
import EmptyState from "../components/EmptyState.jsx";
import Spinner from "../components/Spinner.jsx";
import { formatDate, severityColor } from "../utils/formatters.js";

export default function ReportsPage() {
  const navigate = useNavigate();
  const [reports, setReports] = useState([]);
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [deleting, setDeleting] = useState(null);

  const load = useCallback(async (p = 0) => {
    setLoading(true); setError(null);
    try {
      const data = await analyzeApi.listReports(p, 10);
      setReports(data.content || []);
      setTotalPages(data.totalPages || 0);
      setPage(data.number || 0);
    } catch (e) {
      setError(e.message);
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => { load(0); }, [load]);

  const handleDelete = async (id, name) => {
    if (!confirm(`Delete report "${name}"?`)) return;
    setDeleting(id);
    try {
      await analyzeApi.deleteReport(id);
      load(page);
    } catch (e) {
      setError(e.message);
    } finally {
      setDeleting(null);
    }
  };

  return (
    <div className="p-6 max-w-7xl mx-auto space-y-6">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-bold text-gradient">Saved Reports</h1>
          <p className="text-slate-500 text-sm mt-1">Analysis reports persisted to MongoDB Atlas</p>
        </div>
        <button
          id="refresh-reports-btn"
          className="btn-secondary flex items-center gap-2 text-sm"
          onClick={() => load(page)}
          disabled={loading}
        >
          <RefreshCw size={14} className={loading ? "animate-spin" : ""} />
          Refresh
        </button>
      </div>

      {error && (
        <div className="flex gap-3 items-start p-4 bg-red-500/10 border border-red-500/30 rounded-xl">
          <AlertCircle size={18} className="text-red-400 mt-0.5 shrink-0" />
          <p className="text-sm text-red-300">{error}</p>
        </div>
      )}

      {loading ? (
        <div className="flex items-center justify-center py-20">
          <Spinner size={28} className="text-accent-hover" />
        </div>
      ) : reports.length === 0 ? (
        <EmptyState
          icon={BookOpen}
          title="No saved reports"
          description="Analyze some logs and save the results to see them here."
          action={
            <button className="btn-primary" onClick={() => navigate("/analyze")}>
              Analyze Logs
            </button>
          }
        />
      ) : (
        <>
          <div className="glass-card overflow-hidden">
            <div className="divide-y divide-white/5">
              {reports.map((r) => (
                <div
                  key={r.id}
                  className="flex items-center justify-between px-5 py-4 hover:bg-white/3 transition-colors"
                >
                  <div className="flex-1 min-w-0">
                    <h3 className="font-semibold text-slate-200 truncate">{r.reportName}</h3>
                    <div className="flex gap-3 mt-1 flex-wrap">
                      <span className="text-xs text-slate-500">{formatDate(r.createdAt)}</span>
                      <span className="text-xs text-red-400">{r.totalErrors} errors</span>
                      <span className="text-xs text-yellow-400">{r.totalWarnings} warnings</span>
                      <span className="text-xs text-slate-600">{r.totalLines} lines</span>
                    </div>
                  </div>
                  <div className="flex gap-2 ml-4 shrink-0">
                    <button
                      id={`view-report-${r.id}`}
                      className="btn-secondary flex items-center gap-1.5 text-xs py-2 px-3"
                      onClick={() => navigate(`/reports/${r.id}`)}
                    >
                      <Eye size={13} /> View
                    </button>
                    <button
                      id={`delete-report-${r.id}`}
                      className="btn-danger flex items-center gap-1.5 text-xs py-2 px-3"
                      onClick={() => handleDelete(r.id, r.reportName)}
                      disabled={deleting === r.id}
                    >
                      {deleting === r.id ? <Spinner size={13} /> : <Trash2 size={13} />}
                      Delete
                    </button>
                  </div>
                </div>
              ))}
            </div>
          </div>

          {/* Pagination */}
          {totalPages > 1 && (
            <div className="flex items-center justify-center gap-3">
              <button
                className="btn-secondary py-1.5 px-3 text-sm flex items-center gap-1"
                onClick={() => load(page - 1)}
                disabled={page === 0}
              >
                <ChevronLeft size={14} /> Prev
              </button>
              <span className="text-sm text-slate-500">
                Page {page + 1} of {totalPages}
              </span>
              <button
                className="btn-secondary py-1.5 px-3 text-sm flex items-center gap-1"
                onClick={() => load(page + 1)}
                disabled={page >= totalPages - 1}
              >
                Next <ChevronRight size={14} />
              </button>
            </div>
          )}
        </>
      )}
    </div>
  );
}
