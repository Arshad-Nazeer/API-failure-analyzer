import React from "react";
import { Routes, Route } from "react-router-dom";
import Layout from "./components/Layout.jsx";
import OverviewPage from "./pages/OverviewPage.jsx";
import AnalyzePage from "./pages/AnalyzePage.jsx";
import ReportsPage from "./pages/ReportsPage.jsx";
import ReportDetailPage from "./pages/ReportDetailPage.jsx";
import AboutPage from "./pages/AboutPage.jsx";

export default function App() {
  return (
    <Routes>
      <Route path="/" element={<Layout />}>
        <Route index element={<OverviewPage />} />
        <Route path="analyze" element={<AnalyzePage />} />
        <Route path="reports" element={<ReportsPage />} />
        <Route path="reports/:id" element={<ReportDetailPage />} />
        <Route path="about" element={<AboutPage />} />
      </Route>
    </Routes>
  );
}
