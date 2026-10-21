import { StrictMode } from "react";
import { createRoot } from "react-dom/client";
import { HashRouter } from "react-router";
import { Proveedores } from "./Proveedores.jsx";
import { AppCliente } from "./AppCliente.jsx";

createRoot(document.getElementById("root")).render(
  // En dev monta, desmonta y vuelve a montar: deja a la vista los efectos que no limpian.
  <StrictMode>
    <Proveedores>
      <HashRouter>
        <AppCliente />
      </HashRouter>
    </Proveedores>
  </StrictMode>,
);
