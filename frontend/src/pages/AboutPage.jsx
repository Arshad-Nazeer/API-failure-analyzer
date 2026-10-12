import React from "react";
import { Zap, Code, Database, Server, Activity } from "lucide-react";

export default function AboutPage() {
  return (
    <div className="p-6 max-w-4xl mx-auto space-y-6">
      <div>
        <h1 className="text-2xl font-bold text-gradient">About API Failure Analyzer</h1>
        <p className="text-slate-500 text-sm mt-1">A developer tool for log analysis and API debugging</p>
      </div>

      <div className="glass-card p-6 space-y-4">
        <h2 className="font-semibold text-slate-200">Overview</h2>
        <p className="text-sm text-slate-400 leading-relaxed">
          API Failure Analyzer is a full-stack developer tool that parses API logs, groups recurring failures,
          detects HTTP error patterns, and provides deterministic fix recommendations without requiring any external AI API.
          Built for internship portfolio demonstration.
        </p>
        <div className="grid grid-cols-2 gap-3 pt-2">
          {[
            { icon: Server, label: "Spring Boot 3.x", sub: "Java 21, REST APIs" },
            { icon: Database, label: "MongoDB Atlas", sub: "Spring Data MongoDB" },
            { icon: Code, label: "React + Vite", sub: "Tailwind CSS, Recharts" },
            { icon: Activity, label: "Log Analysis", sub: "Deterministic rules engine" },
          ].map(({ icon: Icon, label, sub }) => (
            <div key={label} className="glass-card p-4 flex items-center gap-3">
              <Icon size={18} className="text-accent-hover" />
              <div>
                <p className="text-sm font-medium text-slate-200">{label}</p>
                <p className="text-xs text-slate-500">{sub}</p>
              </div>
            </div>
          ))}
        </div>
      </div>

      <div className="glass-card p-6 space-y-3">
        <h2 className="font-semibold text-slate-200">Supported Log Formats</h2>
        {[
          ["Common App Log", "2026-10-13 10:42:12 ERROR PaymentService - Payment gateway timeout"],
          ["HTTP Access Log", "2026-10-13T10:42:13Z GET /api/users 500 145ms"],
          ["JSON Structured Log", `{"timestamp":"...","level":"ERROR","method":"POST","endpoint":"/api/orders","status":503}`],
          ["Simple HTTP", "GET /api/products 404"],
          ["Exception Lines", "NullPointerException at PaymentService.java:42"],
        ].map(([label, example]) => (
          <div key={label}>
            <p className="text-xs font-medium text-slate-400 mb-1">{label}</p>
            <code className="text-xs text-slate-500 font-mono bg-dark-900/60 px-2 py-1.5 rounded block break-all">
              {example}
            </code>
          </div>
        ))}
      </div>

      <div className="glass-card p-6">
        <h2 className="font-semibold text-slate-200 mb-3">API Notes</h2>
        <p className="text-xs text-slate-500">
          Reports are not user-isolated in this version. All saved reports are accessible to any user in
          the same local environment. Authentication and user isolation will be added in a future version.
          Do not store sensitive credentials or personal data in log content submitted to this tool.
        </p>
      </div>
    </div>
  );
}
