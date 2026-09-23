import { useEffect } from "react";
import { Button, Divider, Grid, Group, Paper, ScrollArea, Stack, Text, Title } from "@mantine/core";
import { useNavigate, useParams } from "react-router";
import * as api from "../api/api-http.js";
import { etiqueta } from "../api/etiquetas.js";
import { dia, hora, precio } from "../api/formato.js";
import { useAvisar } from "../componentes/Avisos.jsx";
import { EsperaOError } from "../componentes/Estado.jsx";
import { ESTILO, estiloTipo, MapaButacas, Referencia } from "../componentes/MapaButacas.jsx";
import { useCargar } from "../componentes/useCargar.js";
import { Volver } from "../componentes/Volver.jsx";
import { catalogoTarifas, precioConTarifa, SelectorTarifa, sesionDeCompra, sinButacas, sostenerSeleccion,
         useCompra, useRenovarBloqueo } from "./compra.jsx";

// El fondo dice el estado; el borde y el símbolo, el tipo de butaca.
function pintarParaComprar(asiento, elegidas) {
  const titulo = `${asiento.codigo} · ${etiqueta(asiento.tipo)} · ${precio(asiento.precio)}`;
  if (asiento.estado === "FUERA_DE_SERVICIO") {
    return { estilo: ESTILO.fueraDeServicio, deshabilitado: true, titulo: `${asiento.codigo} · fuera de servicio` };
  }
  if (asiento.ocupado) return { estilo: ESTILO.ocupada, deshabilitado: true, titulo: `${asiento.codigo} · ocupada` };
  if (elegidas[asiento.codigo]) return { estilo: ESTILO.elegida, deshabilitado: false, titulo };
  return { estilo: estiloTipo(asiento.tipo), deshabilitado: false, titulo };
}

export function Funcion() {
  const { id } = useParams();
  const navegar = useNavigate();
  const avisar = useAvisar();
  const { seleccion, setSeleccion } = useCompra();
  // La sesión va en el pedido para que las butacas que uno mismo bloqueó no le vuelvan
  // marcadas como ocupadas: "ocupado" lo decide el backend, no esta pantalla.
  const carga = useCargar(() => Promise.all([api.obtenerFuncion(id, sesionDeCompra()), catalogoTarifas()]), [id]);
  const funcion = carga.datos?.[0];

  // Otra función es otra compra: lo elegido para la anterior no se arrastra.
  useEffect(() => {
    if (funcion && seleccion.funcionId !== funcion.id) setSeleccion({ funcionId: funcion.id, butacas: {} });
  }, [funcion, seleccion.funcionId, setSeleccion]);

  useRenovarBloqueo(funcion?.id);

  if (!funcion) return <EsperaOError carga={carga} />;
  const butacas = seleccion.funcionId === funcion.id ? seleccion.butacas : {};
  const elegidas = funcion.asientos.filter((a) => butacas[a.codigo]);
  const total = elegidas.reduce((suma, a) => suma + precioConTarifa(a, butacas[a.codigo]), 0);

  async function alternar(asiento) {
    // Arranca en GENERAL: la tarifa reducida hay que elegirla a propósito, porque
    // después hay que acreditarla en la puerta.
    const nuevas = butacas[asiento.codigo]
      ? sinButacas(butacas, [asiento.codigo])
      : { ...butacas, [asiento.codigo]: "GENERAL" };
    // Se pinta antes de pedir el bloqueo: la butaca se ve elegida en el acto y no
    // después de un ida y vuelta al servidor.
    setSeleccion({ funcionId: funcion.id, butacas: nuevas });

    const rechazadas = await sostenerSeleccion(funcion.id, nuevas).catch(() => []);
    if (rechazadas.length) {
      // Que se escape una butaca es que otro llegó primero, no una falla. El mapa se
      // vuelve a pedir porque el que teníamos ya está diciendo algo que no es cierto.
      setSeleccion((s) => ({ ...s, butacas: sinButacas(s.butacas, rechazadas) }));
      avisar(`${rechazadas.join(", ")}: alguien las está comprando`, "error");
      carga.recargar();
    }
  }

  const cambiarTarifa = (codigo, tarifa) =>
    setSeleccion((s) => ({ ...s, butacas: { ...s.butacas, [codigo]: tarifa } }));

  return (
    <Stack gap="md">
      <Volver a={`/pelicula/${funcion.peliculaId}`}>{funcion.pelicula.titulo}</Volver>
      <div>
        <Title order={1}>{funcion.pelicula.titulo}</Title>
        <Text size="sm">
          {dia(funcion.inicio)} {hora(funcion.inicio)} · {funcion.sala.nombre} ({etiqueta(funcion.sala.tipo)}) ·{" "}
          {etiqueta(funcion.proyeccion)} · {etiqueta(funcion.idioma)}
        </Text>
        <Text size="sm" c="dimmed">
          {funcion.libres} butacas libres de {funcion.sala.capacidadSala} · precio base {precio(funcion.precio)}
        </Text>
      </div>

      <Grid gap="md" align="flex-start">
        <Grid.Col span={{ base: 12, md: 8 }}>
          <MapaButacas sala={funcion.sala} asientos={funcion.asientos}
            pintar={(a) => pintarParaComprar(a, butacas)} alElegir={alternar} />
          <Referencia items={[
            [estiloTipo("ESTANDAR"), "libre"],
            [ESTILO.elegida, "elegida"],
            [ESTILO.ocupada, "ocupada"],
            [ESTILO.fueraDeServicio, "fuera de servicio"],
            [estiloTipo("VIP"), "* VIP"],
            [estiloTipo("PAREJA"), "& pareja"],
            [estiloTipo("ACCESIBLE"), "+ accesible"],
          ]} />
        </Grid.Col>

        {/* Cada butaca lleva su propia tarifa: quien compra elige acá y ve el precio cambiar,
            en vez de enterarse del descuento recién en el ticket. */}
        <Grid.Col span={{ base: 12, md: 4 }} style={{ position: "sticky", top: 76 }}>
          <Paper withBorder p="md">
            <Text fw={600} mb="xs">Tu selección</Text>
            {elegidas.length ? (
              <ScrollArea.Autosize mah={260}>
                {elegidas.map((a) => (
                  <Group key={a.codigo} justify="space-between" py={4} wrap="nowrap">
                    <Text fw={500} size="sm">{a.codigo}</Text>
                    <Group gap="xs" wrap="nowrap">
                      <SelectorTarifa valor={butacas[a.codigo]} alCambiar={(t) => cambiarTarifa(a.codigo, t)} />
                      <Text size="sm" w={80} ta="right">{precio(precioConTarifa(a, butacas[a.codigo]))}</Text>
                    </Group>
                  </Group>
                ))}
              </ScrollArea.Autosize>
            ) : (
              <Text size="sm" c="dimmed">Elegí una o más butacas</Text>
            )}
            <Divider my="sm" />
            <Group justify="space-between" mb="sm">
              <Text size="sm" fw={600}>
                {elegidas.length ? `${elegidas.length} butaca${elegidas.length > 1 ? "s" : ""}` : "Total"}
              </Text>
              <Text fw={700}>{precio(total)}</Text>
            </Group>
            <Button fullWidth disabled={!elegidas.length} onClick={() => navegar(`/confirmar/${funcion.id}`)}>
              Continuar
            </Button>
          </Paper>
        </Grid.Col>
      </Grid>
    </Stack>
  );
}
