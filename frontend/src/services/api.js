import axios from "axios";

const BASE_URL = import.meta.env.VITE_API_BASE_URL || "http://localhost:8080";

const api = axios.create({
  baseURL: BASE_URL,
  timeout: 60000,
  headers: { "Content-Type": "application/json" },
});

api.interceptors.response.use(
  (res) => res,
  (err) => {
    const message =
      err.response?.data?.message ||
      err.response?.data?.error ||
      err.message ||
      "An unexpected error occurred";
    return Promise.reject(new Error(message));
  }
);

export const analyzeApi = {
  analyze: (logs, name) =>
    api.post("/api/analysis/analyze", { logs, name }).then((r) => r.data),

  upload: (file, name) => {
    const form = new FormData();
    form.append("file", file);
    if (name) form.append("name", name);
    return api
      .post("/api/analysis/upload", form, {
        headers: { "Content-Type": "multipart/form-data" },
      })
      .then((r) => r.data);
  },

  saveReport: (reportName, analysisResult) =>
    api
      .post("/api/analysis/reports", { reportName, analysisResult })
      .then((r) => r.data),

  listReports: (page = 0, size = 10) =>
    api
      .get("/api/analysis/reports", { params: { page, size } })
      .then((r) => r.data),

  getReport: (id) =>
    api.get(`/api/analysis/reports/${id}`).then((r) => r.data),

  deleteReport: (id) => api.delete(`/api/analysis/reports/${id}`),

  health: () => api.get("/api/health").then((r) => r.data),
};

export default api;
