import { MantineProvider } from "@mantine/core";
import { domMax, LazyMotion, MotionConfig } from "motion/react";
import { render } from "@testing-library/react";
import { MemoryRouter, Route, Routes, useLocation } from "react-router";
import { Avisos } from "@/compartido/estado/Avisos.jsx";
import { CompraEnCurso } from "@/compra/CompraEnCurso.jsx";
import { SesionDelCliente } from "@/cuenta/SesionDelCliente.jsx";

// Adónde llevó la pantalla: el test afirma la ruta, no solo que se fue de la que estaba.
function OtraPantalla() {
  const { pathname, search } = useLocation();
  return (
    <>
      <p>otra pantalla</p>
      <p>{`en ${pathname}${search}`}</p>
    </>
  );
}

// Los mismos providers que Proveedores y AppCliente, sin animaciones: Motion salta a su estado final y los tests no esperan.
// `ruta` es el patrón de la pantalla (para useParams) y `en` la URL de entrada.
export function renderConApp(ui, { ruta = "/", en = "/", ...opciones } = {}) {
  return render(
    <MantineProvider>
      <LazyMotion features={domMax} strict>
        <MotionConfig reducedMotion="always">
          <Avisos>
            <MemoryRouter initialEntries={[en]}>
              <SesionDelCliente>
                <CompraEnCurso>
                  <Routes>
                    <Route path={ruta} element={ui} />
                    <Route path="*" element={<OtraPantalla />} />
                  </Routes>
                </CompraEnCurso>
              </SesionDelCliente>
            </MemoryRouter>
          </Avisos>
        </MotionConfig>
      </LazyMotion>
    </MantineProvider>,
    opciones,
  );
}
