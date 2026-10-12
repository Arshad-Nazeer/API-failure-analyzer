import React, { useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";
import { Activity, AlertCircle, BookOpen, Search, Zap, Server, Database } from "lucide-react";
import { analyzeApi } from "../services/api.js";
import StatCard from "../components/StatCard.jsx";
import Spinner from "../components/Spinner.jsx";
import { formatDate } from "../utils/formatters.js";

export default function OverviewPage() {
  const navigate = useNavigate();
  const [health, setHealth] = useState(null);
  const [recentReports, setRecentReports] = useState([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    Promise.all([
      analyzeApi.health().catch(() => null),
      analyzeApi.listReports(0, 5).catch(() => ({ content: [] })),
    ]).then(([h, r]) => {
      setHealth(h);
      setRecentReports(r?.content || []);
      setLoading(false);
    });
  }, []);

  return (
    <div className="p-6 max-w-7xl mx-auto space-y-8">
      {/* Hero */}
      <div className="glass-card p-8 relative overflow-hidden">
        <div className="absolute inset-0 bg-gradient-to-br from-accent-primary/10 via-transparent to-purple-500/5 pointer-events-none" />
        <div className="relative">
          <div className="flex items-center gap-3 mb-3">
            <div className="w-10 h-10 bg-accent-primary/20 rounded-xl flex items-center justify-center">
              <Zap size={20} className="text-accent-hover" />
            </div>
            <div>
              <h1 className="text-2xl font-bold text-gradient">API Failure Analyzer</h1>
              <p className="text-xs text-slate-500">Developer tool for log analysis & debugging</p>
            </div>
          </div>
          <p className="text-slate-400 text-sm max-w-2xl leading-relaxed">
            Parse multi-format API logs, detect HTTP errors, group recurring failures,
            identify failing endpoints, and get deterministic fix recommendations.
            Save analysis reports to MongoDB Atlas for future reference.
          </p>
          <div className="flex gap-3 mt-5">
            <button
              id="overview-analyze-btn"
              className="btn-primary flex items-center gap-2"
              onClick={() => navigate("/analyze")}
            >
              <Search size={16} /> Start Analyzing
            </button>
            <button
              id="overview-reports-btn"
              className="btn-secondary flex items-center gap-2"
              onClick={() => navigate("/reports")}
            >
              <BookOpen size={16} /> View Reports
            </button>
          </div>
        </div>
      </div>

      {/* Health status */}
      <div>
        <h2 className="text-sm font-semibold text-slate-400 uppercase tracking-wider mb-3">System Status</h2>
        {loading ? (
          <div className="flex items-center gap-2 text-slate-500">
            <Spinner size={16} /> Checking...
          </div>
        ) : (
          <div className="grid grid-cols-1 sm:grid-cols-3 gap-3">
            <StatCard
              label="API Server"
              value={health ? "Online" : "Offline"}
              icon={Server}
              color={health ? "text-green-400" : "text-red-400"}
              sub={health?.timestamp ? formatDate(health.timestamp) : "—"}
            />
            <StatCard
              label="Database"
              value={health?.database ?? "Unknown"}
              icon={Database}
              color={health?.database === "UP" ? "text-green-400" : "text-red-400"}
            />
            <StatCard
              label="Saved Reports"
              value={health?.savedReports ?? "—"}
              icon={BookOpen}
              color="text-accent-hover"
            />
          </div>
        )}
      </div>

      {/* Features */}
      <div>
        <h2 className="text-sm font-semibold text-slate-400 uppercase tracking-wider mb-3">Capabilities</h2>
        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-3">
          {[
            { title: "Multi-Format Parsing", desc: "Supports common app logs, HTTP access logs, JSON structured logs, and simple HTTP patterns.", icon: Activity },
            { title: "HTTP Status Analysis", desc: "Count and categorize all HTTP responses: 2xx success, 3xx redirects, 4xx client errors, 5xx server errors.", icon: AlertCircle },
            { title: "Error Grouping", desc: "Normalize and group repeated errors by exception type, endpoint, and HTTP status code.", icon: Zap },
            { title: "Endpoint Analysis", desc: "Identify frequently failing endpoints, slow requests, and error distributions per route.", icon: Search },
            { title: "Fix Recommendations", desc: "Deterministic rule-based suggestions for every detected error pattern.", icon: BookOpen },
            { title: "MongoDB Persistence", desc: "Save reports to MongoDB Atlas and retrieve them across sessions.", icon: Database },
          ].map(({ title, desc, icon: Icon }) => (
            <div key={title} className="glass-card-hover p-5">
              <div className="w-8 h-8 bg-accent-primary/15 rounded-lg flex items-center justify-center mb-3">
                <Icon size={16} className="text-accent-hover" />
              </div>
              <h3 className="font-semibold text-slate-200 text-sm mb-1">{title}</h3>
              <p className="text-xs text-slate-500 leading-relaxed">{desc}</p>
            </div>
          ))}
        </div>
      </div>

      {/* Recent reports */}
      {recentReports.length > 0 && (
        <div>
          <h2 className="text-sm font-semibold text-slate-400 uppercase tracking-wider mb-3">Recent Reports</h2>
          <div className="glass-card overflow-hidden">
            <div className="divide-y divide-white/5">
              {recentReports.map((r) => (
                <button
                  key={r.id}
                  className="w-full flex items-center justify-between px-5 py-3.5 hover:bg-white/3 transition-colors text-left"
                  onClick={() => navigate(`/reports/${r.id}`)}
                >
                  <div>
                    <p className="text-sm font-medium text-slate-200">{r.reportName}</p>
                    <p className="text-xs text-slate-500">{formatDate(r.createdAt)}</p>
                  </div>
                  <div className="flex gap-3 text-xs text-slate-500">
                    <span className="text-red-400">{r.totalErrors} errors</span>
                    <span>{r.totalLines} lines</span>
                  </div>
                </button>
              ))}
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
