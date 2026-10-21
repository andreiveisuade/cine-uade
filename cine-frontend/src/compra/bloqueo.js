import * as api from "@/api/api-http.js";

// sessionStorage: cada pestaña es una compra. No es credencial: la doble venta la impide la base.
const CLAVE_SESION = "cine.sesionDeCompra";

export function sesionDeCompra() {
  let sesion = sessionStorage.getItem(CLAVE_SESION);
  if (!sesion) {
    sesion = crypto.randomUUID();
    sessionStorage.setItem(CLAVE_SESION, sesion);
  }
  return sesion;
}

export async function sostenerSeleccion(funcionId, butacas) {
  const { rechazadas } = await api.bloquearButacas({
    funcionId,
    sesion: sesionDeCompra(),
    butacas: Object.keys(butacas),
  });
  return rechazadas;
}

export function sinButacas(butacas, codigos) {
  return Object.fromEntries(Object.entries(butacas).filter(([codigo]) => !codigos.includes(codigo)));
}
