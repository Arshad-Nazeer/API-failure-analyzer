import React from "react";

export default function StatCard({ label, value, icon: Icon, color = "text-accent-hover", sub }) {
  return (
    <div className="stat-card">
      <div className="flex items-center justify-between">
        <span className="text-xs font-medium text-slate-500 uppercase tracking-wider">{label}</span>
        {Icon && (
          <div className={`w-7 h-7 rounded-lg bg-white/5 flex items-center justify-center ${color}`}>
            <Icon size={14} />
          </div>
        )}
      </div>
      <span className={`text-2xl font-bold mt-1 ${color}`}>{value ?? "—"}</span>
      {sub && <span className="text-xs text-slate-500">{sub}</span>}
    </div>
  );
}
