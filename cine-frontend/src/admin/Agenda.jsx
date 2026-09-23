// El alto de cada bloque sale de la duración real: es lo que muestra si dos funciones se pisan (R3).

import { Box, Button, Group, Paper, Select, Text } from "@mantine/core";
import { Link, useNavigate, useParams } from "react-router";
import * as api from "../api/api-http.js";
import { etiqueta } from "../api/etiquetas.js";
import { duracion, hoyISO } from "../api/formato.js";
import { EsperaOError, Vacio } from "../componentes/Estado.jsx";
import { useCargar } from "../componentes/useCargar.js";
import { Encabezado, Nota } from "./comun.jsx";

// Dos modos: en una sola columna, las funciones simultáneas de varias salas se pisarían.
const MODOS = {
  semana: { etiqueta: "Semana (una sala)", dias: 7 },
  dia: { etiqueta: "Día (todas las salas)", dias: 1 },
};

const PX_POR_MINUTO = 1.1;

// Rompe la proporción a propósito: en un corto de 5 minutos no se leería ni el título.
const ALTO_MINIMO = 26;

// Un color estable por título, entre recargas.
const COLORES = ["teal", "blue", "yellow", "pink", "violet", "cyan", "orange", "indigo"];

const NOMBRE_DIA = ["Dom", "Lun", "Mar", "Mié", "Jue", "Vie", "Sáb"];

const minutosDe = (iso) => Number(iso.slice(11, 13)) * 60 + Number(iso.slice(14, 16));

const enHora = (m) => `${String(Math.floor(m / 60) % 24).padStart(2, "0")}:${String(m % 60).padStart(2, "0")}`;

function correr(fechaISO, dias) {
  const d = new Date(`${fechaISO}T00:00:00`);
  d.setDate(d.getDate() + dias);
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, "0")}-${String(d.getDate()).padStart(2, "0")}`;
}

// #/agenda/modo/desde/sala: los tres tienen default para que un hash a mano no deje la pantalla en blanco.
function leerRuta({ modo, desde, salaId }) {
  return {
    modo: MODOS[modo] ? modo : "semana",
    desde: /^\d{4}-\d{2}-\d{2}$/.test(desde || "") ? desde : hoyISO(),
    salaId: Number(salaId) || 1,
  };
}

function armarColumnas(modo, desde, salas, sala) {
  if (modo === "dia") {
    return salas.map((s) => ({
      clave: `sala-${s.id}`,
      titulo: <>{s.nombre}<Text size="xs" c="dimmed">{etiqueta(s.tipo)}</Text></>,
      tomaA: (f) => f.sala.id === s.id && f.inicio.slice(0, 10) === desde,
      subtitulo: (f) => etiqueta(f.proyeccion),
    }));
  }
  return Array.from({ length: 7 }, (_, i) => {
    const fecha = correr(desde, i);
    const d = new Date(`${fecha}T00:00:00`);
    return {
      clave: fecha,
      titulo: <>{NOMBRE_DIA[d.getDay()]}<Text fz="lg">{d.getDate()}</Text></>,
      tomaA: (f) => f.sala.id === sala.id && f.inicio.slice(0, 10) === fecha,
      subtitulo: (f) => `${etiqueta(f.proyeccion)} · ${etiqueta(f.idioma).toLowerCase()}`,
    };
  });
}

// Sale de las funciones y no de 00 a 24: un cine abre a la tarde. Incluye la limpieza de la última.
function franjaHoraria(funciones) {
  if (!funciones.length) return null;
  const arranques = funciones.map((f) => minutosDe(f.inicio));
  const finales = funciones.map((f) => minutosDe(f.inicio) + f.pelicula.duracionMinutos + (f.sala.minutosLimpieza || 0));
  return {
    inicio: Math.floor(Math.min(...arranques) / 60) * 60,
    fin: Math.ceil(Math.max(...finales) / 60) * 60,
  };
}

// Un enlace y no un div con onClick: el clic del medio y el teclado funcionan solos.
function Bloque({ funcion, inicioFranja, columna }) {
  const arranca = minutosDe(funcion.inicio);
  const dura = funcion.pelicula.duracionMinutos;
  const alto = Math.max(dura * PX_POR_MINUTO, ALTO_MINIMO);
  const color = COLORES[funcion.pelicula.id % COLORES.length];
  const termina = arranca + dura;
  const limpieza = funcion.sala.minutosLimpieza;
  return (
    <>
      <Box component={Link} to={`/funciones/${funcion.id}`}
        title={`${funcion.pelicula.titulo}\n${enHora(arranca)}–${enHora(termina)} (${duracion(dura)})\n`
          + `${funcion.sala.nombre} · ${etiqueta(funcion.proyeccion)} · ${etiqueta(funcion.idioma)} — clic para verla en Funciones`}
        pos="absolute" left={2} right={2} top={(arranca - inicioFranja) * PX_POR_MINUTO} h={alto} px={6} py={2}
        bg={`var(--mantine-color-${color}-light)`} c={`var(--mantine-color-${color}-light-color)`}
        style={{ overflow: "hidden", borderRadius: 4, fontSize: 12, lineHeight: 1.25, textDecoration: "none",
                 borderLeft: `3px solid var(--mantine-color-${color}-filled)` }}>
        <strong>{enHora(arranca)}</strong> {funcion.pelicula.titulo}
        {alto > 44 && <div style={{ fontSize: 11, opacity: 0.75 }}>{columna.subtitulo(funcion)}</div>}
      </Box>
      {/* Sin la limpieza rayada el hueco parece libre, y la regla aparece recién cuando el alta falla. */}
      {limpieza > 0 && (
        <Box pos="absolute" left={2} right={2} top={(termina - inicioFranja) * PX_POR_MINUTO} h={limpieza * PX_POR_MINUTO}
          title={`Limpieza de ${funcion.sala.nombre}: ${limpieza} min, hasta ${enHora(termina + limpieza)}`}
          style={{ borderTop: "1px dashed var(--mantine-color-dimmed)", borderRadius: "0 0 4px 4px",
                   backgroundImage: "repeating-linear-gradient(45deg, rgb(100 116 139 / .22) 0 4px, transparent 4px 8px)" }} />
      )}
    </>
  );
}

function Grilla({ columnas, funciones, franja }) {
  const alto = (franja.fin - franja.inicio) * PX_POR_MINUTO;
  const horas = [];
  for (let m = franja.inicio; m <= franja.fin; m += 60) horas.push(m);
  const plantilla = { display: "grid", gridTemplateColumns: `56px repeat(${columnas.length}, minmax(0, 1fr))` };
  const borde = "1px solid var(--mantine-color-default-border)";

  return (
    <Paper withBorder style={{ overflowX: "auto" }}>
      <div style={{ minWidth: 720 }}>
        <div style={{ ...plantilla, borderBottom: borde, background: "var(--mantine-color-default-hover)" }}>
          <div />
          {columnas.map((c) => (
            <Text key={c.clave} component="div" size="sm" fw={500} ta="center" py="xs" style={{ borderLeft: borde }}>{c.titulo}</Text>
          ))}
        </div>
        {/* El margen deja entera la etiqueta de la primera y la última hora, que van centradas. */}
        <div style={{ ...plantilla, padding: "10px 0" }}>
          <div style={{ position: "relative", height: alto }}>
            {horas.map((m) => (
              <Text key={m} size="xs" c="dimmed" pos="absolute" right={4}
                style={{ top: (m - franja.inicio) * PX_POR_MINUTO, transform: "translateY(-50%)" }}>{enHora(m)}</Text>
            ))}
          </div>
          {columnas.map((columna) => (
            <div key={columna.clave} style={{ position: "relative", height: alto, borderLeft: borde }}>
              {horas.map((m) => (
                <div key={m} style={{ position: "absolute", left: 0, right: 0, top: (m - franja.inicio) * PX_POR_MINUTO,
                                      borderTop: "1px solid var(--mantine-color-default-border)", opacity: 0.5 }} />
              ))}
              {funciones.filter((f) => columna.tomaA(f)).map((f) => (
                <Bloque key={f.id} funcion={f} inicioFranja={franja.inicio} columna={columna} />
              ))}
            </div>
          ))}
        </div>
      </div>
    </Paper>
  );
}

export function Agenda() {
  const navegar = useNavigate();
  const { modo, desde, salaId } = leerRuta(useParams());
  const carga = useCargar(() => Promise.all([api.obtenerFunciones(), api.obtenerSalas()]), []);
  if (!carga.datos) return <EsperaOError carga={carga} />;
  const [funciones, salas] = carga.datos;

  const sala = salas.find((s) => s.id === salaId) || salas[0];
  const columnas = armarColumnas(modo, desde, salas, sala);
  const visibles = funciones.filter((f) => columnas.some((c) => c.tomaA(f)));
  const franja = franjaHoraria(visibles);
  const ir = (cambios) => {
    const proximo = { modo, desde, salaId: sala.id, ...cambios };
    navegar(`/agenda/${proximo.modo}/${proximo.desde}/${proximo.salaId}`);
  };

  return (
    <>
      <Encabezado titulo="Agenda">
        La programación como la ve quien la arma: cada bloque ocupa el alto de lo que dura. Los huecos son dónde entra
        algo nuevo.
      </Encabezado>

      <Group align="flex-end" gap="sm" mb="md">
        <Select label="Ver" w={210} allowDeselect={false} value={modo} onChange={(m) => ir({ modo: m })}
          data={Object.entries(MODOS).map(([clave, m]) => ({ value: clave, label: m.etiqueta }))} />
        {modo === "semana" && (
          <Select label="Sala" w={200} allowDeselect={false} value={String(sala.id)} onChange={(id) => ir({ salaId: Number(id) })}
            data={salas.map((s) => ({ value: String(s.id), label: `${s.nombre} — ${etiqueta(s.tipo)}` }))} />
        )}
        <Button.Group>
          <Button variant="default" onClick={() => ir({ desde: correr(desde, -MODOS[modo].dias) })} aria-label="Anterior">←</Button>
          <Button variant="default" onClick={() => ir({ desde: hoyISO() })}>Hoy</Button>
          <Button variant="default" onClick={() => ir({ desde: correr(desde, MODOS[modo].dias) })} aria-label="Siguiente">→</Button>
        </Button.Group>
        <Text size="sm" c="dimmed">{visibles.length} funciones{modo === "semana" ? ` en ${sala.nombre}` : ""}</Text>
      </Group>

      {franja ? <Grilla columnas={columnas} funciones={visibles} franja={franja} />
        : <Paper withBorder p="lg"><Vacio>No hay funciones programadas en este rango.</Vacio></Paper>}

      <Nota mt="sm">
        El alto de cada bloque es su duración. Los que duran menos de {Math.round(ALTO_MINIMO / PX_POR_MINUTO)} minutos se
        dibujan con un alto mínimo para que el título entre: es la única parte del gráfico que no está a escala.
      </Nota>
    </>
  );
}
