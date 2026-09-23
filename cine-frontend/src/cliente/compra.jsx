import { createContext, useContext, useEffect, useRef, useState } from "react";
import { NativeSelect } from "@mantine/core";
import * as api from "../api/api-http.js";
import { etiqueta } from "../api/etiquetas.js";

// Código de butaca → tarifa, porque la tarifa es por persona.
// En memoria y no en el navegador: recargar la pierde, igual que vence el bloqueo del backend.
const Contexto = createContext(null);

export function CompraEnCurso({ children }) {
  const [seleccion, setSeleccion] = useState({ funcionId: null, butacas: {} });
  return <Contexto.Provider value={{ seleccion, setSeleccion }}>{children}</Contexto.Provider>;
}

export const useCompra = () => useContext(Contexto);

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

const RENOVAR_CADA_MS = 60_000;

export function useRenovarBloqueo(funcionId) {
  const { seleccion, setSeleccion } = useCompra();
  // El intervalo se arma una vez: la ref le deja leer la selección de ahora.
  const actual = useRef(seleccion.butacas);
  actual.current = seleccion.butacas;

  useEffect(() => {
    if (!funcionId) return;
    const renovacion = setInterval(async () => {
      const rechazadas = await sostenerSeleccion(funcionId, actual.current).catch(() => []);
      if (rechazadas.length) {
        // Seguir mostrándolas elegidas sería prometer una butaca que ya no está.
        setSeleccion((s) => ({ ...s, butacas: sinButacas(s.butacas, rechazadas) }));
      }
    }, RENOVAR_CADA_MS);
    return () => clearInterval(renovacion);
  }, [funcionId, setSeleccion]);
}

let tarifas = null;

export async function catalogoTarifas() {
  if (!tarifas) tarifas = await api.obtenerTarifas();
  return tarifas;
}

export function tarifaPorNombre(nombre) {
  return (tarifas || []).find((t) => t.nombre === nombre) || { multiplicador: 1, requiereAcreditacion: false };
}

/** El precio de esa butaca con esa tarifa. asiento.precio siempre viene en GENERAL. */
export function precioConTarifa(asiento, nombreTarifa) {
  return Math.round(asiento.precio * tarifaPorNombre(nombreTarifa).multiplicador * 100) / 100;
}

export function SelectorTarifa({ valor, alCambiar }) {
  return (
    <NativeSelect size="xs" value={valor} onChange={(e) => alCambiar(e.currentTarget.value)}
      data={(tarifas || []).map((t) => ({ value: t.nombre, label: etiqueta(t.nombre) }))} />
  );
}

const CLAVE_CLIENTE = "cine.cliente";

export function clienteRecordado() {
  try {
    return JSON.parse(localStorage.getItem(CLAVE_CLIENTE)) || null;
  } catch {
    return null;
  }
}

export function recordarCliente(cliente) {
  localStorage.setItem(CLAVE_CLIENTE, JSON.stringify({ nombre: cliente.nombre, email: cliente.email }));
}

export function olvidarCliente() {
  localStorage.removeItem(CLAVE_CLIENTE);
}
