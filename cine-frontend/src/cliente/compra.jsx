import { createContext, useContext, useEffect, useRef, useState } from "react";
import { NativeSelect } from "@mantine/core";
import * as api from "../api/api-http.js";
import { etiqueta } from "../api/etiquetas.js";

/* ------------------------------------------------------ la selección en curso */

// Lo elegido en el mapa de butacas, para que lo lea la confirmación. Es un mapa de
// código a tarifa y no una lista, porque la tarifa es por persona: en una reserva de
// cuatro puede haber dos generales, un menor y un jubilado. De paso, elegir dos veces
// la misma butaca es imposible de expresar.
//
// Vive en memoria y no en el navegador a propósito: recargar la página la pierde, igual
// que el bloqueo en el backend vence solo. Guardarla sería mostrar butacas elegidas que
// ya nadie está sosteniendo.
const Contexto = createContext(null);

export function CompraEnCurso({ children }) {
  const [seleccion, setSeleccion] = useState({ funcionId: null, butacas: {} });
  return <Contexto.Provider value={{ seleccion, setSeleccion }}>{children}</Contexto.Provider>;
}

export const useCompra = () => useContext(Contexto);

/* ------------------------------------------ las butacas de mientras se elige */

/*
 * Mientras alguien elige, sus butacas dejan de ofrecerse al resto. El backend las guarda
 * a nombre de una sesión que genera este navegador y las suelta solo a los tres minutos:
 * cerrar la pestaña no avisa, así que el bloqueo tiene que caducar por su cuenta.
 *
 * Va en `sessionStorage` y no en `localStorage` porque cada pestaña es una compra
 * distinta. Y no es una credencial: lo peor que puede hacer una sesión inventada es
 * soltar el bloqueo de otro, y eso devuelve una butaca a la venta sin poder venderla dos
 * veces —eso lo sigue impidiendo la base—.
 */
const CLAVE_SESION = "cine.sesionDeCompra";

export function sesionDeCompra() {
  let sesion = sessionStorage.getItem(CLAVE_SESION);
  if (!sesion) {
    sesion = crypto.randomUUID();
    sessionStorage.setItem(CLAVE_SESION, sesion);
  }
  return sesion;
}

/**
 * Le avisa al backend qué tiene elegido esta sesión, entero y no de a una butaca: con eso
 * toma lo nuevo, renueva lo que sigue elegido y suelta lo que se deseleccionó. Devuelve
 * las que se escaparon, para sacarlas de la selección y avisar.
 */
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

// El bloqueo dura tres minutos y se renueva al tocar el mapa; sin esto, quedarse
// leyendo el resumen o tipeando el mail alcanzaría para perder las butacas.
const RENOVAR_CADA_MS = 60_000;

/** Renueva el bloqueo mientras la pantalla esté abierta. Irse la desmonta y corta. */
export function useRenovarBloqueo(funcionId) {
  const { seleccion, setSeleccion } = useCompra();
  // El intervalo se arma una vez: la ref le deja leer la selección de ahora y no la del
  // momento en que se creó.
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

/* ------------------------------------------------------------------ tarifas */

// El catálogo de tarifas sale del backend y no se repite acá: el multiplicador vive en
// el enum del dominio, y tenerlo duplicado serían dos fuentes de verdad para el precio.
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

/* ------------------------------------------------------- el cliente recordado */

// El cliente no inicia sesión. Recordar sus datos en el navegador es lo que hace que
// registrarse sirva de algo: no vuelve a tipearlos al comprar ni al buscar sus reservas.
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

/** "No soy yo": borra lo recordado para que la pantalla vuelva a pedir los datos. */
export function olvidarCliente() {
  localStorage.removeItem(CLAVE_CLIENTE);
}
