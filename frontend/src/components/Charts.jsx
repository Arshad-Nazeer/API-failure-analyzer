import React from "react";
import {
  BarChart, Bar, PieChart, Pie, Cell, XAxis, YAxis,
  CartesianGrid, Tooltip, Legend, ResponsiveContainer
} from "recharts";

const STATUS_COLORS = {
  "2xx": "#22c55e", "3xx": "#3b82f6",
  "4xx": "#eab308", "5xx": "#ef4444", "unknown": "#6b7280"
};
const SEVERITY_COLORS = {
  CRITICAL: "#ef4444", HIGH: "#f97316",
  MEDIUM: "#eab308", LOW: "#22c55e", INFO: "#3b82f6"
};

function CustomTooltip({ active, payload, label }) {
  if (!active || !payload?.length) return null;
  return (
    <div className="glass-card px-3 py-2 text-xs">
      {label && <p className="text-slate-400 mb-1">{label}</p>}
      {payload.map((p, i) => (
        <p key={i} style={{ color: p.color || p.fill }}>
          {p.name}: <span className="font-bold">{p.value}</span>
        </p>
      ))}
    </div>
  );
}

export function StatusDistributionChart({ statusGroupCounts }) {
  const data = Object.entries(statusGroupCounts || {})
    .filter(([, v]) => v > 0)
    .map(([name, value]) => ({ name, value, fill: STATUS_COLORS[name] || "#6b7280" }));

  if (!data.length) {
    return <p className="text-slate-500 text-sm text-center py-8">No HTTP status data</p>;
  }

  return (
    <ResponsiveContainer width="100%" height={200}>
      <PieChart>
        <Pie
          data={data}
          cx="50%" cy="50%"
          innerRadius={55} outerRadius={80}
          dataKey="value"
          nameKey="name"
          paddingAngle={3}
        >
          {data.map((entry, i) => (
            <Cell key={i} fill={entry.fill} stroke="transparent" />
          ))}
        </Pie>
        <Tooltip content={<CustomTooltip />} />
        <Legend
          formatter={(value) => <span className="text-xs text-slate-400">{value}</span>}
        />
      </PieChart>
    </ResponsiveContainer>
  );
}

export function SeverityChart({ errorGroups }) {
  const counts = {};
  (errorGroups || []).forEach((g) => {
    counts[g.severity] = (counts[g.severity] || 0) + 1;
  });
  const data = Object.entries(counts).map(([name, value]) => ({
    name, value, fill: SEVERITY_COLORS[name] || "#6b7280"
  }));

  if (!data.length) {
    return <p className="text-slate-500 text-sm text-center py-8">No severity data</p>;
  }

  return (
    <ResponsiveContainer width="100%" height={200}>
      <BarChart data={data} margin={{ top: 5, right: 5, left: -20, bottom: 5 }}>
        <CartesianGrid strokeDasharray="3 3" stroke="rgba(255,255,255,0.05)" />
        <XAxis dataKey="name" tick={{ fontSize: 11, fill: "#94a3b8" }} />
        <YAxis tick={{ fontSize: 11, fill: "#94a3b8" }} />
        <Tooltip content={<CustomTooltip />} />
        <Bar dataKey="value" name="Groups" radius={[4,4,0,0]}>
          {data.map((entry, i) => <Cell key={i} fill={entry.fill} />)}
        </Bar>
      </BarChart>
    </ResponsiveContainer>
  );
}

export function TopEndpointsChart({ endpointSummaries }) {
  const data = (endpointSummaries || [])
    .filter((e) => e.failureCount > 0)
    .slice(0, 8)
    .map((e) => ({
      name: (e.method ? e.method + " " : "") + e.endpointPattern,
      failures: e.failureCount,
      total: e.totalRequests,
    }));

  if (!data.length) {
    return <p className="text-slate-500 text-sm text-center py-8">No endpoint failure data</p>;
  }

  return (
    <ResponsiveContainer width="100%" height={220}>
      <BarChart data={data} layout="vertical" margin={{ top: 5, right: 20, left: 10, bottom: 5 }}>
        <CartesianGrid strokeDasharray="3 3" stroke="rgba(255,255,255,0.05)" horizontal={false} />
        <XAxis type="number" tick={{ fontSize: 11, fill: "#94a3b8" }} />
        <YAxis
          type="category" dataKey="name"
          tick={{ fontSize: 10, fill: "#94a3b8" }}
          width={140}
          tickFormatter={(v) => v.length > 22 ? v.slice(0, 22) + "…" : v}
        />
        <Tooltip content={<CustomTooltip />} />
        <Bar dataKey="failures" name="Failures" fill="#ef4444" radius={[0,4,4,0]} />
      </BarChart>
    </ResponsiveContainer>
  );
}

export function StatusCodeBarChart({ statusCodeCounts }) {
  const data = Object.entries(statusCodeCounts || {})
    .sort(([a], [b]) => Number(a) - Number(b))
    .map(([code, count]) => {
      const n = Number(code);
      const fill = n >= 500 ? "#ef4444" : n >= 400 ? "#eab308" : n >= 300 ? "#3b82f6" : "#22c55e";
      return { code, count, fill };
    });

  if (!data.length) {
    return <p className="text-slate-500 text-sm text-center py-8">No status code data</p>;
  }

  return (
    <ResponsiveContainer width="100%" height={200}>
      <BarChart data={data} margin={{ top: 5, right: 5, left: -20, bottom: 5 }}>
        <CartesianGrid strokeDasharray="3 3" stroke="rgba(255,255,255,0.05)" />
        <XAxis dataKey="code" tick={{ fontSize: 11, fill: "#94a3b8" }} />
        <YAxis tick={{ fontSize: 11, fill: "#94a3b8" }} />
        <Tooltip content={<CustomTooltip />} />
        <Bar dataKey="count" name="Requests" radius={[4,4,0,0]}>
          {data.map((entry, i) => <Cell key={i} fill={entry.fill} />)}
        </Bar>
      </BarChart>
    </ResponsiveContainer>
  );
}
