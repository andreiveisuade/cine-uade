import { defineConfig } from "vite";
import react from "@vitejs/plugin-react";

// En desarrollo Vite le pasa al nginx de Docker lo que no es suyo: /api queda en el mismo origen que en producción.
const SISTEMA = "http://localhost:8080";

export default defineConfig({
  plugins: [react()],
  build: {
    rolldownOptions: {
      output: {
        // Las librerías cambian menos que la app: en su propio chunk el navegador las reusa entre versiones.
        codeSplitting: {
          groups: [
            { name: "react", test: /node_modules[\\/](react|react-dom|react-router|scheduler)[\\/]/ },
            { name: "mantine", test: /node_modules[\\/](@mantine|@floating-ui|embla-carousel)/ },
          ],
        },
      },
    },
  },
  // Los tests corren en jsdom con el mismo Vite: comparten plugins y resolución con la app.
  test: {
    environment: "jsdom",
    setupFiles: "./src/test/setup.js",
    coverage: { include: ["src/**/*.{js,jsx}"], exclude: ["src/test/**", "src/**/*.test.{js,jsx}"] },
  },
  server: {
    proxy: {
      "/api": SISTEMA,
      "/swagger-ui": SISTEMA,
      "/v3/api-docs": SISTEMA,
    },
  },
});
