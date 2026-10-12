import React, { useState } from "react";
import {
  AlertCircle, AlertTriangle, CheckCircle, Info, ChevronDown,
  ChevronUp, BarChart2, Layers, Lightbulb, List, FileWarning, Clock
} from "lucide-react";
import StatCard from "./StatCard.jsx";
import {
  StatusDistributionChart, SeverityChart,
  TopEndpointsChart, StatusCodeBarChart
} from "./Charts.jsx";
import { severityColor, statusBg, formatDuration, formatDate } from "../utils/formatters.js";

function SectionCard({ title, icon: Icon, children, defaultOpen = true }) {
  const [open, setOpen] = useState(defaultOpen);
  return (
    <div className="glass-card overflow-hidden">
      <button
        className="w-full flex items-center justify-between px-5 py-4 hover:bg-white/3 transition-colors"
        onClick={() => setOpen((o) => !o)}
      >
        <div className="flex items-center gap-2 font-semibold text-slate-200">
          {Icon && <Icon size={16} className="text-accent-hover" />}
          {title}
        </div>
        {open ? <ChevronUp size={16} className="text-slate-500" /> : <ChevronDown size={16} className="text-slate-500" />}
      </button>
      {open && <div className="px-5 pb-5">{children}</div>}
    </div>
  );
}

export default function AnalysisResults({ result, onSave, saving }) {
  if (!result) return null;

  const hasErrors = result.totalErrors > 0;
  const hasWarnings = result.totalWarnings > 0;

  return (
    <div className="space-y-4">
      {/* Warning banner */}
      {result.warningMessage && (
        <div className="flex gap-3 items-start p-4 bg-yellow-500/10 border border-yellow-500/30 rounded-xl">
          <AlertTriangle size={18} className="text-yellow-400 mt-0.5 shrink-0" />
          <p className="text-sm text-yellow-300">{result.warningMessage}</p>
        </div>
      )}

      {/* Summary stats */}
      <div className="grid grid-cols-2 sm:grid-cols-4 gap-3">
        <StatCard label="Total Lines" value={result.totalLines} icon={List} color="text-slate-300" />
        <StatCard label="Parsed" value={result.parsedLines} icon={CheckCircle} color="text-green-400"
          sub={`${result.totalLines > 0 ? Math.round((result.parsedLines / result.totalLines) * 100) : 0}% success`} />
        <StatCard label="Errors" value={result.totalErrors} icon={AlertCircle} color="text-red-400" />
        <StatCard label="Unparsed" value={result.unparsedLines} icon={FileWarning}
          color={result.unparsedLines > 0 ? "text-yellow-400" : "text-slate-500"} />
      </div>

      <div className="grid grid-cols-2 sm:grid-cols-4 gap-3">
        <StatCard label="HTTP Responses" value={result.totalHttpResponses} icon={BarChart2} color="text-blue-400" />
        <StatCard label="Warnings" value={result.totalWarnings} icon={AlertTriangle} color="text-yellow-400" />
        <StatCard label="Error Groups" value={result.groupedErrorPatterns?.length ?? 0} icon={Layers} color="text-orange-400" />
        <StatCard label="Recommendations" value={result.recommendations?.length ?? 0} icon={Lightbulb} color="text-purple-400" />
      </div>

      {/* Charts row */}
      <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
        <SectionCard title="HTTP Status Distribution" icon={BarChart2}>
          <StatusDistributionChart statusGroupCounts={result.statusGroupCounts} />
        </SectionCard>
        <SectionCard title="Individual Status Codes" icon={BarChart2}>
          <StatusCodeBarChart statusCodeCounts={result.statusCodeCounts} />
        </SectionCard>
        <SectionCard title="Error Severity Distribution" icon={AlertCircle}>
          <SeverityChart errorGroups={result.groupedErrorPatterns} />
        </SectionCard>
        <SectionCard title="Top Failing Endpoints" icon={List}>
          <TopEndpointsChart endpointSummaries={result.endpointSummaries} />
        </SectionCard>
      </div>

      {/* Error Groups Table */}
      {result.groupedErrorPatterns?.length > 0 && (
        <SectionCard title={`Grouped Error Patterns (${result.groupedErrorPatterns.length})`} icon={Layers}>
          <div className="overflow-x-auto">
            <table className="w-full text-sm">
              <thead>
                <tr className="border-b border-white/5 text-left">
                  <th className="pb-3 pr-4 text-xs text-slate-500 uppercase tracking-wider">Severity</th>
                  <th className="pb-3 pr-4 text-xs text-slate-500 uppercase tracking-wider">Type</th>
                  <th className="pb-3 pr-4 text-xs text-slate-500 uppercase tracking-wider">Count</th>
                  <th className="pb-3 pr-4 text-xs text-slate-500 uppercase tracking-wider">Affected Endpoints</th>
                  <th className="pb-3 text-xs text-slate-500 uppercase tracking-wider">Message</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-white/5">
                {result.groupedErrorPatterns.map((g, i) => (
                  <tr key={i} className="hover:bg-white/3 transition-colors">
                    <td className="py-3 pr-4">
                      <span className={`text-xs font-medium px-2 py-1 rounded-md ${severityColor(g.severity)}`}>
                        {g.severity}
                      </span>
                    </td>
                    <td className="py-3 pr-4 font-mono text-xs text-slate-300">
                      {g.errorType ?? "—"}
                    </td>
                    <td className="py-3 pr-4 font-bold text-slate-200">{g.occurrenceCount}x</td>
                    <td className="py-3 pr-4">
                      <div className="flex flex-col gap-1">
                        {g.affectedEndpoints?.slice(0, 3).map((ep, j) => (
                          <span key={j} className="text-xs font-mono text-slate-400 bg-white/5 px-1.5 py-0.5 rounded">
                            {ep}
                          </span>
                        ))}
                      </div>
                    </td>
                    <td className="py-3 text-xs text-slate-400 max-w-xs truncate" title={g.representativeMessage}>
                      {g.representativeMessage}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </SectionCard>
      )}

      {/* Endpoint Summaries */}
      {result.endpointSummaries?.length > 0 && (
        <SectionCard title={`Endpoint Analysis (${result.endpointSummaries.length})`} icon={List}>
          <div className="overflow-x-auto">
            <table className="w-full text-sm">
              <thead>
                <tr className="border-b border-white/5 text-left">
                  <th className="pb-3 pr-4 text-xs text-slate-500 uppercase tracking-wider">Endpoint</th>
                  <th className="pb-3 pr-4 text-xs text-slate-500 uppercase tracking-wider">Method</th>
                  <th className="pb-3 pr-4 text-xs text-slate-500 uppercase tracking-wider">Failures</th>
                  <th className="pb-3 pr-4 text-xs text-slate-500 uppercase tracking-wider">Total</th>
                  <th className="pb-3 pr-4 text-xs text-slate-500 uppercase tracking-wider">Avg Duration</th>
                  <th className="pb-3 text-xs text-slate-500 uppercase tracking-wider">Status Dist.</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-white/5">
                {result.endpointSummaries.map((ep, i) => (
                  <tr key={i} className="hover:bg-white/3 transition-colors">
                    <td className="py-3 pr-4 font-mono text-xs text-slate-300">{ep.endpointPattern}</td>
                    <td className="py-3 pr-4">
                      <span className="text-xs bg-white/10 text-slate-300 px-2 py-0.5 rounded font-mono">
                        {ep.method}
                      </span>
                    </td>
                    <td className="py-3 pr-4 font-bold text-red-400">{ep.failureCount}</td>
                    <td className="py-3 pr-4 text-slate-300">{ep.totalRequests}</td>
                    <td className="py-3 pr-4 text-slate-400 flex items-center gap-1">
                      {ep.slowRequestCount > 0 && <Clock size={12} className="text-yellow-400" />}
                      {ep.avgDurationMs != null ? `${Math.round(ep.avgDurationMs)}ms` : "—"}
                    </td>
                    <td className="py-3">
                      <div className="flex gap-1 flex-wrap">
                        {Object.entries(ep.statusDistribution || {}).map(([code, count]) => (
                          <span key={code} className={`text-xs px-1.5 py-0.5 rounded font-mono ${statusBg(code)}`}>
                            {code}: {count}
                          </span>
                        ))}
                      </div>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </SectionCard>
      )}

      {/* Recommendations */}
      {result.recommendations?.length > 0 && (
        <SectionCard title={`Recommendations (${result.recommendations.length})`} icon={Lightbulb}>
          <ul className="space-y-3">
            {result.recommendations.map((rec, i) => (
              <li key={i} className="flex gap-3 items-start">
                <div className="w-5 h-5 rounded-full bg-accent-primary/20 flex items-center justify-center shrink-0 mt-0.5">
                  <span className="text-xs font-bold text-accent-hover">{i + 1}</span>
                </div>
                <p className="text-sm text-slate-300 leading-relaxed">{rec}</p>
              </li>
            ))}
          </ul>
        </SectionCard>
      )}

      {/* Unparsed lines */}
      {result.unparsedLines > 0 && result.unparsedLineSamples?.length > 0 && (
        <SectionCard title={`Unparsed Lines (${result.unparsedLines} total, showing ${result.unparsedLineSamples.length})`} icon={FileWarning} defaultOpen={false}>
          <div className="space-y-1.5 max-h-60 overflow-y-auto">
            {result.unparsedLineSamples.map((line, i) => (
              <div key={i} className="flex gap-2 items-start">
                <span className="text-xs text-slate-600 font-mono mt-0.5 w-5">{i + 1}</span>
                <code className="text-xs text-slate-500 font-mono bg-dark-900/50 px-2 py-1 rounded flex-1 break-all">
                  {line}
                </code>
              </div>
            ))}
          </div>
          <p className="text-xs text-slate-600 mt-3">
            These lines did not match any recognized log format. Review the format guide to see supported patterns.
          </p>
        </SectionCard>
      )}

      {/* Save button */}
      <div className="flex justify-end pt-2">
        <button
          id="save-report-btn"
          className="btn-primary flex items-center gap-2"
          onClick={onSave}
          disabled={saving}
        >
          {saving ? (
            <><span className="animate-spin">⟳</span> Saving...</>
          ) : (
            <><CheckCircle size={16} /> Save Report to MongoDB</>
          )}
        </button>
      </div>
    </div>
  );
}
