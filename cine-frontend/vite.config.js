import { defineConfig } from "vite";
import react from "@vitejs/plugin-react";

// En desarrollo Vite le pasa al nginx de Docker lo que no es suyo: /api queda en el mismo origen que en producción.
const SISTEMA = "http://localhost:8080";

export default defineConfig({
  plugins: [react()],
  server: {
    proxy: {
      "/api": SISTEMA,
      "/swagger-ui": SISTEMA,
      "/v3/api-docs": SISTEMA,
    },
  },
});
