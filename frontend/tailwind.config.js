/** @type {import("tailwindcss").Config} */
export default {
  content: ["./index.html", "./src/**/*.{js,ts,jsx,tsx}"],
  theme: {
    extend: {
      colors: {
        dark: {
          900: "#0a0e1a",
          800: "#0f1629",
          700: "#141e35",
          600: "#1a2744",
          500: "#1e3054",
        },
        accent: {
          primary: "#6366f1",
          hover: "#818cf8",
          glow: "#4f46e5",
        },
        status: {
          critical: "#ef4444",
          high: "#f97316",
          medium: "#eab308",
          low: "#22c55e",
          info: "#3b82f6",
        },
      },
      fontFamily: {
        sans: ["Inter", "system-ui", "sans-serif"],
        mono: ["JetBrains Mono", "Fira Code", "monospace"],
      },
    },
  },
  plugins: [],
};
