import { defineConfig } from "vite";
import react from "@vitejs/plugin-react";

// El sistema completo corre en Docker con nginx en el 8080. En desarrollo, Vite sirve el
// front con recarga en caliente y le pasa a ese nginx lo que no es suyo: así el código
// pide /api igual que en producción y el backend no se entera de que hay otro origen.
const SISTEMA = "http://localhost:8080";

export default defineConfig({
  plugins: [react()],
  build: {
    // Dos entradas, como antes del build: el cliente y el panel no comparten sesión ni
    // menú, y mantener admin.html deja intactos los links que ya circulan.
    rolldownOptions: {
      input: {
        cliente: "index.html",
        admin: "admin.html",
      },
    },
  },
  server: {
    proxy: {
      "/api": SISTEMA,
      "/swagger-ui": SISTEMA,
      "/v3/api-docs": SISTEMA,
    },
  },
});
