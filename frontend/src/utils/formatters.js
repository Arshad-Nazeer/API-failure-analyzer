export function formatDate(isoString) {
  if (!isoString) return "—";
  try {
    return new Intl.DateTimeFormat("en-US", {
      year: "numeric", month: "short", day: "numeric",
      hour: "2-digit", minute: "2-digit",
    }).format(new Date(isoString));
  } catch {
    return isoString;
  }
}

export function formatDuration(ms) {
  if (ms == null) return "—";
  if (ms < 1000) return `${Math.round(ms)}ms`;
  return `${(ms / 1000).toFixed(2)}s`;
}

export function severityColor(severity) {
  switch (severity?.toUpperCase()) {
    case "CRITICAL": return "severity-critical";
    case "HIGH": return "severity-high";
    case "MEDIUM": return "severity-medium";
    case "LOW": return "severity-low";
    default: return "severity-info";
  }
}

export function severityDotColor(severity) {
  switch (severity?.toUpperCase()) {
    case "CRITICAL": return "bg-red-500";
    case "HIGH": return "bg-orange-500";
    case "MEDIUM": return "bg-yellow-500";
    case "LOW": return "bg-green-500";
    default: return "bg-blue-500";
  }
}

export function statusColor(code) {
  const n = Number(code);
  if (n >= 500) return "text-red-400";
  if (n >= 400) return "text-yellow-400";
  if (n >= 300) return "text-blue-400";
  if (n >= 200) return "text-green-400";
  return "text-slate-400";
}

export function statusBg(code) {
  const n = Number(code);
  if (n >= 500) return "bg-red-500/15 text-red-400 border border-red-500/30";
  if (n >= 400) return "bg-yellow-500/15 text-yellow-400 border border-yellow-500/30";
  if (n >= 300) return "bg-blue-500/15 text-blue-400 border border-blue-500/30";
  if (n >= 200) return "bg-green-500/15 text-green-400 border border-green-500/30";
  return "bg-slate-500/15 text-slate-400 border border-slate-500/30";
}
