import { useRef, useState } from "react";
import { Accordion, Alert, Box, Button, Grid, Group, Loader, NumberInput, Paper, Select, Skeleton, SimpleGrid, Stack, Table,
         Text, TextInput, Title } from "@mantine/core";
import * as api from "../api/api-http.js";
import { etiqueta } from "../api/etiquetas.js";
import { dia, duracion, hora, hoyISO } from "../api/formato.js";
import { useAvisar } from "../componentes/Avisos.jsx";
import { ErrorCaja, EsperaOError } from "../componentes/Estado.jsx";
import { useCargar } from "../componentes/useCargar.js";
import { Encabezado, Nota, opcionesDe } from "./comun.jsx";

/**
 * Los indicadores de la corrida anterior, para poder decir cuánto mejoró o empeoró la
 * nueva. Vive en el módulo y no en el componente porque comparar dos corridas es el uso
 * normal de esta pantalla, y sobrevive a salir y volver.
 */
let corridaAnterior = null;

const porcentaje = (fraccion) => `${Math.round(fraccion * 100)}%`;
const conDecimal = (numero) => numero.toFixed(1).replace(".", ",");

/**
 * Cuánto cambió un indicador contra la corrida anterior. Es la mitad del valor de la
 * pantalla: el número solo no dice si 79% de ocupación está bien, pero «79%, cuatro
 * puntos más que con seis títulos» sí.
 */
function Variacion({ actual, anterior, formato }) {
  if (anterior === null || anterior === undefined) return null;
  const delta = actual - anterior;
  if (Math.abs(delta) < 0.0001) return <Text size="xs" c="dimmed">igual que la corrida anterior</Text>;
  return (
    <Text size="xs" c={delta > 0 ? "green" : "yellow"}>
      {delta > 0 ? "▲" : "▼"} {formato(Math.abs(delta))} vs. la corrida anterior
    </Text>
  );
}

function Tarjeta({ titulo, valor, detalle, children }) {
  return (
    <Paper withBorder p="sm">
      <Text size="xs" tt="uppercase" c="dimmed">{titulo}</Text>
      <Text fz={26} fw={700}>{valor}</Text>
      <Text size="xs" c="dimmed">{detalle}</Text>
      {children}
    </Paper>
  );
}

function Indicadores({ indicadores: i, pases, anterior }) {
  const previos = anterior?.indicadores;
  return (
    <SimpleGrid cols={{ base: 1, sm: 2, xl: 4 }}>
      <Tarjeta titulo="Ocupación de las salas" valor={porcentaje(i.ocupacion)}
        detalle={<>{i.minutosProgramados.toLocaleString("es-AR")} de {i.minutosDisponibles.toLocaleString("es-AR")} minutos{" "}
          <strong>libres</strong>, que son la ventana menos lo que ya estaba programado</>}>
        <Variacion actual={i.ocupacion} anterior={previos?.ocupacion} formato={porcentaje} />
      </Tarjeta>
      <Tarjeta titulo="Puntaje promedio" valor={conDecimal(i.puntajePromedio)}
        detalle="por pase, así una película con más funciones pesa más">
        <Variacion actual={i.puntajePromedio} anterior={previos?.puntajePromedio} formato={conDecimal} />
      </Tarjeta>
      <Tarjeta titulo="Géneros cubiertos" valor={`${i.generosCubiertos} de ${i.generosTotales}`}
        detalle="cuántos géneros del catálogo aparecen en la semana">
        <Variacion actual={i.generosCubiertos} anterior={previos?.generosCubiertos} formato={(n) => `${n}`} />
      </Tarjeta>
      <Tarjeta titulo="Pases" valor={String(pases)} detalle="funciones que arma la propuesta" />
    </SimpleGrid>
  );
}

/**
 * Los pases por género, como barras. Es donde se ve el problema que el planificador
 * existe para evitar: una grilla con cuatro películas de acción y nada para el resto.
 */
function Generos({ pasesPorGenero }) {
  const entradas = Object.entries(pasesPorGenero || {}).sort((a, b) => b[1] - a[1]);
  if (!entradas.length) return null;
  const maximo = entradas[0][1];
  return (
    <Paper withBorder p="md">
      <Title order={3} size="h5" mb="sm">Pases por género</Title>
      <Stack gap={6}>
        {entradas.map(([genero, cuantos]) => (
          <Group key={genero} gap="sm" wrap="nowrap">
            <Text size="sm" w={128} style={{ flexShrink: 0 }}>{etiqueta(genero)}</Text>
            <Box h={12} bg="indigo" style={{ borderRadius: 3, width: `${(cuantos / maximo) * 70}%` }} />
            <Text size="xs" c="dimmed">{cuantos}</Text>
          </Group>
        ))}
      </Stack>
      <Nota mt="sm">Una película cuenta en todos sus géneros, así que la suma es mayor que la cantidad de pases.</Nota>
    </Paper>
  );
}

function Elenco({ elenco }) {
  return (
    <Paper withBorder>
      <Title order={3} size="h5" p="md" pb="xs">Elenco de la semana ({elenco.length})</Title>
      <Table.ScrollContainer minWidth={560}>
        <Table verticalSpacing="xs">
          <Table.Thead>
            <Table.Tr>
              <Table.Th>Película</Table.Th><Table.Th ta="right">Puntaje</Table.Th><Table.Th>Duración</Table.Th>
              <Table.Th>Géneros</Table.Th><Table.Th ta="right">Pases</Table.Th>
            </Table.Tr>
          </Table.Thead>
          <Table.Tbody>
            {elenco.map((p) => (
              <Table.Tr key={p.id}>
                <Table.Td fw={500}>{p.titulo}</Table.Td>
                <Table.Td ta="right" fw={500}>{conDecimal(p.puntaje)}</Table.Td>
                <Table.Td c="dimmed" style={{ whiteSpace: "nowrap" }}>{duracion(p.duracionMinutos)}</Table.Td>
                <Table.Td fz="xs" c="dimmed">{p.generos.map(etiqueta).join(" · ")}</Table.Td>
                <Table.Td ta="right" fw={500}>{p.pases}</Table.Td>
              </Table.Tr>
            ))}
          </Table.Tbody>
        </Table>
      </Table.ScrollContainer>
      <Nota p="sm" style={{ borderTop: "1px solid var(--mantine-color-default-border)" }}>
        La primera entró por puntaje; las siguientes, por lo que <strong>agregan</strong> a lo ya elegido — por eso puede
        entrar una comedia de 7,0 antes que la cuarta de acción de 8,5. Los pases se reparten proporcionalmente al puntaje.
      </Nota>
    </Paper>
  );
}

/**
 * La grilla propuesta, agrupada por día y sala. Plana serían ciento cincuenta filas
 * ordenadas por hora, donde no se ve ni qué pasa en una sala ni qué se da un día.
 */
function Pases({ pases }) {
  const dias = new Map();
  for (const pase of pases) {
    const clave = pase.inicio.slice(0, 10);
    if (!dias.has(clave)) dias.set(clave, new Map());
    const salas = dias.get(clave);
    if (!salas.has(pase.sala)) salas.set(pase.sala, []);
    salas.get(pase.sala).push(pase);
  }
  const fechas = [...dias.keys()];
  return (
    <Paper withBorder p="md">
      <Title order={3} size="h5" mb="sm">La semana, sala por sala</Title>
      <Accordion variant="separated" defaultValue={fechas[0]}>
        {[...dias.entries()].map(([fecha, salas]) => (
          <Accordion.Item key={fecha} value={fecha}>
            <Accordion.Control>
              <Text size="sm" fw={500}>
                {dia(`${fecha}T00:00:00`)}{" "}
                <Text span size="sm" c="dimmed">· {[...salas.values()].reduce((suma, p) => suma + p.length, 0)} pases</Text>
              </Text>
            </Accordion.Control>
            <Accordion.Panel>
              <Stack gap="xs">
                {[...salas.entries()].map(([sala, deLaSala]) => (
                  <Group key={sala} gap={6} align="baseline">
                    <Text size="xs" fw={500} c="dimmed" w={64}>{sala}</Text>
                    {deLaSala.map((p) => (
                      <Text key={p.inicio + p.titulo} size="xs" px={8} py={2} bg="var(--mantine-color-default-hover)"
                        style={{ borderRadius: 4 }}>
                        <strong>{hora(p.inicio)}</strong> {p.titulo}
                      </Text>
                    ))}
                  </Group>
                ))}
              </Stack>
            </Accordion.Panel>
          </Accordion.Item>
        ))}
      </Accordion>
    </Paper>
  );
}

function Propuesta({ grilla, anterior }) {
  const aplicada = grilla.funcionesCreadas > 0;
  if (!grilla.pases.length) {
    return <Alert color="yellow">Con estos criterios no entra ninguna función: revisá la ventana horaria o los días.</Alert>;
  }
  return (
    <Stack gap="md">
      <Alert color={aplicada ? "green" : "gray"}
        title={aplicada ? `Se crearon ${grilla.funcionesCreadas} funciones` : `Así quedaría la semana: ${grilla.pases.length} funciones`}>
        {aplicada
          ? "Las funciones ya están cargadas y se pueden ver en Funciones y en la Agenda."
          : "Todavía no se escribió nada. Cambiá los criterios y volvé a previsualizar para comparar."}
      </Alert>
      <Indicadores indicadores={grilla.indicadores} pases={grilla.pases.length} anterior={anterior} />
      <Elenco elenco={grilla.elenco} />
      <Generos pasesPorGenero={grilla.indicadores.pasesPorGenero} />
      <Pases pases={grilla.pases} />
    </Stack>
  );
}

/**
 * Lo que se ve mientras el servidor arma la grilla: el mismo andamiaje que el resultado,
 * en gris. Contar las dos etapas del algoritmo mientras corren es la única parte de la
 * espera que le sirve a quien mira: para cuando aparecen los números, ya sabe de dónde
 * salieron.
 */
function Calculando({ aplicando }) {
  return (
    <Stack gap="md">
      <Alert color="gray" title={<Group gap="xs"><Loader size="xs" />{aplicando ? "Creando las funciones…" : "Armando la grilla…"}</Group>}>
        {aplicando
          ? "Cada pase de la propuesta se programa como una función de verdad."
          : "Primero elige el elenco por puntaje y géneros; después llena cada sala día por día, preguntando en cada horario si está libre."}
      </Alert>
      <SimpleGrid cols={{ base: 1, sm: 2, xl: 4 }}>
        {[0, 1, 2, 3].map((i) => (
          <Paper key={i} withBorder p="sm"><Skeleton h={10} w={96} /><Skeleton h={26} w={64} mt="xs" /><Skeleton h={10} mt="xs" /></Paper>
        ))}
      </SimpleGrid>
      <Paper withBorder p="md"><Skeleton h={14} w={160} />{[0, 1, 2, 3, 4].map((i) => <Skeleton key={i} h={22} mt="xs" />)}</Paper>
    </Stack>
  );
}

/**
 * El planificador: qué se da esta semana.
 *
 * La pantalla no es un listado de funciones sino los indicadores primero. Sin ellos,
 * «armame la grilla» es un botón que escupe ciento cincuenta filas que nadie puede
 * juzgar; con ellos, el encargado corre la propuesta con seis títulos y con diez,
 * compara ocupación y variedad, y recién ahí aplica.
 *
 * Mismo par que las programaciones: «Previsualizar» no escribe nada y «Aplicar» crea las
 * funciones con los mismos criterios. Como el planificador es determinista, lo que se ve
 * es lo que se va a crear.
 */
export function Planificador() {
  const avisar = useAvisar();
  const carga = useCargar(() => Promise.all([api.obtenerIdiomas(), api.obtenerProyecciones()]), []);
  const [criterios, setCriterios] = useState({
    desde: hoyISO(), dias: 7, apertura: "14:00", cierre: "00:00", cuantasPeliculas: 8, precio: 5000,
    idioma: null, proyeccion: null,
  });
  const [resultado, setResultado] = useState(null);
  const [corriendo, setCorriendo] = useState(null);
  const [error, setError] = useState(null);
  // Los criterios de ahora, para descartar una respuesta que llegó después de tocarlos.
  const vigentes = useRef(criterios);
  vigentes.current = criterios;

  if (!carga.datos) return <EsperaOError carga={carga} />;
  const [idiomas, proyecciones] = carga.datos;
  const pedido = { ...criterios, idioma: criterios.idioma || idiomas[0], proyeccion: criterios.proyeccion || proyecciones[0] };

  /** Una propuesta vale solo para los criterios con los que se pidió. */
  const cambiar = (clave) => (valor) => { setCriterios((c) => ({ ...c, [clave]: valor })); setResultado(null); };

  async function correr(evento, aplicar) {
    const formulario = evento.currentTarget.form || evento.currentTarget;
    if (!formulario.reportValidity()) return;
    setError(null);
    setCorriendo(aplicar ? "aplicar" : "previsualizar");
    const pedidos = criterios;
    try {
      const grilla = await (aplicar ? api.armarGrilla : api.proponerGrilla)(pedido);
      // Si tocó un criterio mientras calculaba, lo que llegó ya no describe lo que está en
      // pantalla: pintarlo sería mostrar una grilla que no es la de estos criterios.
      if (vigentes.current !== pedidos) return;
      setResultado({ grilla, anterior: corridaAnterior, aplicada: aplicar });
      corridaAnterior = { titulos: Number(pedido.cuantasPeliculas), indicadores: grilla.indicadores };
      if (aplicar) avisar(`Se crearon ${grilla.funcionesCreadas} funciones`);
    } catch (e) {
      setResultado(null);
      setError(e.message);
    } finally {
      setCorriendo(null);
    }
  }

  // funcionesCreadas viene en 0 al previsualizar y con el número real en el alta: es lo
  // único que distingue «así quedaría» de «así quedó», porque los pases son los mismos.
  const pases = resultado?.grilla.pases.length || 0;
  const textoAplicar = resultado?.aplicada ? "Aplicada" : resultado ? `Crear ${pases} funciones` : "Aplicar";

  return (
    <>
      <Encabezado titulo="Planificador de la semana">
        Elige el elenco con un criterio que mira <strong>puntaje y géneros a la vez</strong> —la primera es la mejor a secas,
        y de ahí en adelante cada una suma un bono por los géneros que todavía faltan— y después reparte los pases entre las
        salas de forma proporcional al puntaje: la mejor de la semana se lleva cuatro o cinco funciones diarias y la última,
        una. No pisa funciones ya cargadas.
      </Encabezado>

      <Grid gap="md" align="flex-start">
        <Grid.Col span={{ base: 12, lg: 4 }}>
          <Paper withBorder p="md">
            <Title order={2} size="h4" mb="sm">Criterios</Title>
            <form onSubmit={(e) => { e.preventDefault(); correr(e, true); }}>
              <Stack gap="sm">
                <Group grow>
                  <TextInput label="Desde" type="date" required value={criterios.desde}
                    onChange={(e) => cambiar("desde")(e.currentTarget.value)} />
                  <NumberInput label="Días" required min={1} max={31} value={criterios.dias} onChange={cambiar("dias")} />
                </Group>
                <Group grow>
                  <TextInput label="Apertura" type="time" required value={criterios.apertura}
                    onChange={(e) => cambiar("apertura")(e.currentTarget.value)} />
                  <TextInput label="Cierre" type="time" required value={criterios.cierre}
                    onChange={(e) => cambiar("cierre")(e.currentTarget.value)} />
                </Group>
                <Nota>
                  El cierre a las 00:00 se lee como el final del día. Es hasta cuándo tiene que <em>haber terminado</em> la
                  última función, no cuándo puede empezar.
                </Nota>
                <NumberInput label="Cuántas películas" required min={1} max={30} value={criterios.cuantasPeliculas}
                  onChange={cambiar("cuantasPeliculas")}
                  description="Cuántos títulos distintos entran en la semana. Solo se eligen entre las confirmadas: lo que espera en el buzón no se puede programar." />
                <NumberInput label="Precio base" required min={100} step={100} value={criterios.precio} onChange={cambiar("precio")} />
                <Group grow>
                  <Select label="Idioma" allowDeselect={false} value={pedido.idioma} onChange={cambiar("idioma")}
                    data={opcionesDe(idiomas, etiqueta)} />
                  <Select label="Proyección" allowDeselect={false} value={pedido.proyeccion} onChange={cambiar("proyeccion")}
                    data={opcionesDe(proyecciones, etiqueta)} />
                </Group>
                <Stack gap="xs">
                  <Button variant="default" loading={corriendo === "previsualizar"} disabled={!!corriendo}
                    onClick={(e) => correr(e, false)}>Previsualizar</Button>
                  <Button type="submit" loading={corriendo === "aplicar"}
                    disabled={!!corriendo || !resultado || resultado.aplicada || !pases}>{textoAplicar}</Button>
                </Stack>
                {error && <ErrorCaja>{error}</ErrorCaja>}
              </Stack>
            </form>
            <Nota mt="md">
              Previsualizar no escribe nada. Al aplicar, el servidor <strong>vuelve a calcular</strong> la propuesta con estos
              mismos criterios: no recibe la que estás viendo, así que si alguien programó algo en el medio, lo respeta.
            </Nota>
          </Paper>
        </Grid.Col>
        <Grid.Col span={{ base: 12, lg: 8 }}>
          {corriendo ? <Calculando aplicando={corriendo === "aplicar"} />
            : resultado && <Propuesta grilla={resultado.grilla} anterior={resultado.anterior} />}
        </Grid.Col>
      </Grid>
    </>
  );
}
