import { createRoot } from "react-dom/client";
import { HashRouter } from "react-router";
import { Base } from "../Base.jsx";
import { AppAdmin } from "./AppAdmin.jsx";
import { Sesion } from "./sesion.jsx";

createRoot(document.getElementById("root")).render(
  <Base>
    <HashRouter>
      <Sesion>
        <AppAdmin />
      </Sesion>
    </HashRouter>
  </Base>,
);
