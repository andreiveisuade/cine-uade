import { render, screen, waitFor } from "@testing-library/react";
import { MantineProvider } from "@mantine/core";
import { domMax, LazyMotion, MotionConfig } from "motion/react";
import { Link, MemoryRouter } from "react-router";
import userEvent from "@testing-library/user-event";
import { afterEach, describe, expect, it, vi } from "vitest";
import { AppCliente } from "./AppCliente.jsx";

vi.mock("@/api/api-http.js", () => ({ sesionActual: vi.fn(() => Promise.resolve(null)) }));
vi.mock("./Rutas.jsx", () => ({ Rutas: () => <Link to="/nosotros">Ir</Link> }));

describe("AppCliente", () => {
  const original = window.scrollTo;
  afterEach(() => { window.scrollTo = original; });

  // Chrome 157 devuelve una Promise: si el efecto la devuelve, React la llama como cleanup y desmonta toda la app.
  it("sobrevive al cambio de ruta aunque scrollTo devuelva una promesa", async () => {
    window.scrollTo = vi.fn(() => Promise.resolve());
    render(
      <MantineProvider>
        <LazyMotion features={domMax} strict>
          <MotionConfig reducedMotion="always">
            <MemoryRouter><AppCliente /></MemoryRouter>
          </MotionConfig>
        </LazyMotion>
      </MantineProvider>,
    );
    await userEvent.click(await screen.findByRole("link", { name: "Ir" }));
    await waitFor(() => expect(window.scrollTo).toHaveBeenCalledTimes(2));
    expect(screen.getByRole("link", { name: "Ir" })).toBeInTheDocument();
  });
});
