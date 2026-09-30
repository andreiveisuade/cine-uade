import { createRoot } from "react-dom/client";
import { HashRouter } from "react-router";
import { Base } from "../Base.jsx";
import { AppCliente } from "./AppCliente.jsx";

createRoot(document.getElementById("root")).render(
  <Base>
    <HashRouter>
      <AppCliente />
    </HashRouter>
  </Base>,
);
