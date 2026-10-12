import React, { useState } from "react";
import { NavLink, Outlet } from "react-router-dom";
import {
  Activity, Search, BookOpen, Info, Zap, Menu, X
} from "lucide-react";

const navItems = [
  { to: "/", label: "Overview", icon: Activity, end: true },
  { to: "/analyze", label: "Analyze Logs", icon: Search },
  { to: "/reports", label: "Saved Reports", icon: BookOpen },
  { to: "/about", label: "About", icon: Info },
];

export default function Layout() {
  const [sidebarOpen, setSidebarOpen] = useState(false);

  return (
    <div className="flex h-screen overflow-hidden">
      {/* Sidebar */}
      <aside
        className={`fixed inset-y-0 left-0 z-50 w-64 bg-dark-800/95 backdrop-blur border-r border-white/5
          transform transition-transform duration-300 lg:static lg:translate-x-0
          ${sidebarOpen ? "translate-x-0" : "-translate-x-full"}`}
      >
        <div className="flex flex-col h-full">
          {/* Logo */}
          <div className="flex items-center gap-3 px-5 py-5 border-b border-white/5">
            <div className="w-8 h-8 bg-accent-primary/20 rounded-lg flex items-center justify-center">
              <Zap size={16} className="text-accent-hover" />
            </div>
            <div>
              <h1 className="font-bold text-sm text-slate-100">API Failure</h1>
              <p className="text-xs text-slate-500">Analyzer v1.0</p>
            </div>
            <button
              className="ml-auto lg:hidden text-slate-400 hover:text-slate-100"
              onClick={() => setSidebarOpen(false)}
            >
              <X size={18} />
            </button>
          </div>

          {/* Nav */}
          <nav className="flex-1 px-3 py-4 space-y-1">
            {navItems.map(({ to, label, icon: Icon, end }) => (
              <NavLink
                key={to}
                to={to}
                end={end}
                className={({ isActive }) =>
                  `nav-link ${isActive ? "active" : ""}`
                }
                onClick={() => setSidebarOpen(false)}
              >
                <Icon size={18} />
                {label}
              </NavLink>
            ))}
          </nav>

          {/* Footer */}
          <div className="px-5 py-4 border-t border-white/5">
            <p className="text-xs text-slate-600">
              Built for internship portfolio
            </p>
          </div>
        </div>
      </aside>

      {/* Mobile overlay */}
      {sidebarOpen && (
        <div
          className="fixed inset-0 z-40 bg-black/60 lg:hidden"
          onClick={() => setSidebarOpen(false)}
        />
      )}

      {/* Main content */}
      <div className="flex-1 flex flex-col overflow-hidden">
        {/* Mobile topbar */}
        <header className="lg:hidden flex items-center gap-3 px-4 py-3 bg-dark-800/80 border-b border-white/5">
          <button
            className="text-slate-400 hover:text-slate-100"
            onClick={() => setSidebarOpen(true)}
          >
            <Menu size={20} />
          </button>
          <div className="flex items-center gap-2">
            <Zap size={16} className="text-accent-hover" />
            <span className="font-semibold text-sm">API Failure Analyzer</span>
          </div>
        </header>

        <main className="flex-1 overflow-y-auto">
          <Outlet />
        </main>
      </div>
    </div>
  );
}
