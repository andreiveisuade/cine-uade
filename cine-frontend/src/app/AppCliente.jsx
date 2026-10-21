import { useEffect } from "react";
import { AppShell } from "@mantine/core";
import { useLocation } from "react-router";
import { Contenedor } from "@/compartido/layout/Contenedor.jsx";
import { CompraEnCurso } from "@/cliente/compra.jsx";
import { SesionDelCliente } from "@/cliente/sesion.jsx";
import { Barra } from "@/app/layout/Barra.jsx";
import { Pie } from "@/app/layout/Pie.jsx";
import { ErrorDePantalla } from "./ErrorDePantalla.jsx";
import { Rutas } from "./Rutas.jsx";

// El marco de la venta: barra arriba, pantalla en el medio y pie abajo, con la sesión y la compra en curso.
export function AppCliente() {
  const { pathname } = useLocation();
  // Cada pantalla arranca arriba, como una página.
  useEffect(() => window.scrollTo(0, 0), [pathname]);

  return (
    <SesionDelCliente>
      <AppShell header={{ height: 64 }}>
        <AppShell.Header style={{ borderColor: "var(--cine-borde)" }}>
          <Barra />
        </AppShell.Header>

        <AppShell.Main style={{ display: "flex", flexDirection: "column" }}>
          <Contenedor pt="var(--cine-gutter)" w="100%" style={{ flex: 1 }}>
            <CompraEnCurso>
              <ErrorDePantalla>
                <Rutas />
              </ErrorDePantalla>
            </CompraEnCurso>
          </Contenedor>
          <Pie />
        </AppShell.Main>
      </AppShell>
    </SesionDelCliente>
  );
}
