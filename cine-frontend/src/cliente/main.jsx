import { StrictMode } from "react";
import { createRoot } from "react-dom/client";
import { HashRouter } from "react-router";
import { Base } from "../Base.jsx";
import { AppCliente } from "./AppCliente.jsx";

createRoot(document.getElementById("root")).render(
  // En dev monta, desmonta y vuelve a montar: deja a la vista los efectos que no limpian.
  <StrictMode>
    <Base>
      <HashRouter>
        <AppCliente />
      </HashRouter>
    </Base>
  </StrictMode>,
);
