import { MantineProvider } from "@mantine/core";
import { domMax, LazyMotion, MotionConfig } from "motion/react";
import { render } from "@testing-library/react";
import { MemoryRouter, Route, Routes } from "react-router";
import { Avisos } from "@/compartido/estado/Avisos.jsx";
import { CompraEnCurso } from "@/compra/CompraEnCurso.jsx";
import { SesionDelCliente } from "@/cliente/sesion.jsx";

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
                    <Route path="*" element={<p>otra pantalla</p>} />
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
