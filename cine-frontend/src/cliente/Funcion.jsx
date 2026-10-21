import { Button, Grid, Group, Paper, ScrollArea, Skeleton, Stack, Text, Title } from "@mantine/core";
import { useNavigate, useParams } from "react-router";
import * as api from "@/api/api-http.js";
import { etiqueta } from "@/api/etiquetas.js";
import { precio } from "@/api/formato.js";
import { useAvisar } from "@/componentes/Avisos.jsx";
import { EsperaOError } from "@/componentes/Estado.jsx";
import { ESTILO, estiloTipo, MapaButacas, Referencia } from "@/componentes/MapaButacas.jsx";
import { MontoAnimado } from "@/componentes/MontoAnimado.jsx";
import { useCargar } from "@/componentes/useCargar.js";
import { useTitulo } from "@/componentes/useTitulo.js";
import { Encabezado } from "@/componentes/Encabezado.jsx";
import { catalogoTarifas, DatosFuncion, precioConTarifa, resumenCompra, SelectorTarifa, sesionDeCompra, sinButacas,
         sostenerSeleccion, useCambiarTarifa, useCompra, useRenovarBloqueo } from "./compra.jsx";

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

function FuncionEsqueleto() {
  return (
    <Stack gap="md" mt="xl">
      <Skeleton h={36} w="50%" />
      <Skeleton h={14} w="35%" />
      <Grid gap="md">
        <Grid.Col span={{ base: 12, lg: 9 }}><Skeleton h={320} /></Grid.Col>
        <Grid.Col span={{ base: 12, lg: 3 }}><Skeleton h={200} /></Grid.Col>
      </Grid>
    </Stack>
  );
}

export function Funcion() {
  const { id } = useParams();
  const navegar = useNavigate();
  const avisar = useAvisar();
  const { seleccion, setSeleccion } = useCompra();
  const cambiarTarifa = useCambiarTarifa();
  // Con la sesión, las butacas que uno mismo bloqueó no vuelven como ocupadas.
  const carga = useCargar(() => Promise.all([api.obtenerFuncion(id, sesionDeCompra()), catalogoTarifas()]), [id]);
  const funcion = carga.datos?.[0];

  useRenovarBloqueo(funcion?.id);
  useTitulo(funcion && `Butacas de ${funcion.pelicula.titulo}`);

  if (!funcion) return <EsperaOError carga={carga} esqueleto={<FuncionEsqueleto />} />;
  // Otra función es otra compra: lo elegido para la anterior no se arrastra. Se deriva acá y no con un efecto que
  // la vacíe; el primer clic la reemplaza porque `alternar` escribe la selección entera con este funcionId.
  const butacas = seleccion.funcionId === funcion.id ? seleccion.butacas : {};
  const { elegidas, total } = resumenCompra(funcion, butacas);

  async function alternar(asiento) {
    // Arranca en GENERAL: una tarifa reducida hay que acreditarla en la puerta.
    const nuevas = butacas[asiento.codigo]
      ? sinButacas(butacas, [asiento.codigo])
      : { ...butacas, [asiento.codigo]: "GENERAL" };
    // Se pinta antes del bloqueo para que se vea elegida en el acto.
    setSeleccion({ funcionId: funcion.id, butacas: nuevas });

    let rechazadas;
    try {
      rechazadas = await sostenerSeleccion(funcion.id, nuevas);
    } catch (e) {
      // Un 400 es una selección que el backend no aparta entera (tope, butaca fuera de servicio): se suelta y se dice por qué.
      // Otro error no cambia nada: la doble venta la impide la base.
      if (e.status !== 400) return;
      if (!butacas[asiento.codigo]) {
        setSeleccion((s) => ({ ...s, butacas: sinButacas(s.butacas, [asiento.codigo]) }));
      }
      avisar(e.message, "error");
      carga.recargar();
      return;
    }
    if (rechazadas.length) {
      // Que se escape una butaca es que otro llegó primero: el mapa se vuelve a pedir.
      setSeleccion((s) => ({ ...s, butacas: sinButacas(s.butacas, rechazadas) }));
      avisar(`${rechazadas.join(", ")}: alguien las está comprando`, "error");
      carga.recargar();
    }
  }

  return (
    <div>
      <Encabezado titulo={funcion.pelicula.titulo} volver={{ a: `/pelicula/${funcion.peliculaId}`, texto: "Otras funciones" }}>
        <DatosFuncion funcion={funcion} size="md" fw={500} />
      </Encabezado>

      <Grid gap="lg" align="flex-start">
        <Grid.Col span={{ base: 12, lg: 8 }}>
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

        <Grid.Col span={{ base: 12, lg: 4 }} style={{ position: "sticky", top: 88 }}>
          <Paper withBorder p="lg">
            <Title order={2} fz="h4" mb="sm">Tu selección</Title>
            {elegidas.length ? (
              <ScrollArea.Autosize mah={280}>
                {elegidas.map((a) => (
                  <Group key={a.codigo} justify="space-between" py={6} wrap="nowrap"
                    style={{ borderBottom: "1px solid var(--cine-borde)" }}>
                    <Text fw={700} className="cine-cifras">{a.codigo}</Text>
                    <Group gap="xs" wrap="nowrap">
                      <SelectorTarifa valor={butacas[a.codigo]} alCambiar={(t) => cambiarTarifa(a.codigo, t)} />
                      <Text size="sm" w={80} ta="right" className="cine-cifras">{precio(precioConTarifa(a, butacas[a.codigo]))}</Text>
                    </Group>
                  </Group>
                ))}
              </ScrollArea.Autosize>
            ) : (
              <Text size="sm" c="dimmed">Elegí una o más butacas</Text>
            )}
            <Group justify="space-between" align="baseline" mt="md" mb="md">
              <Text size="sm" fw={600}>
                {elegidas.length ? `${elegidas.length} butaca${elegidas.length > 1 ? "s" : ""}` : "Total"}
              </Text>
              <Text className="cine-display cine-cifras" fz={34} fw={850} lh={1}><MontoAnimado valor={total} /></Text>
            </Group>
            <Button fullWidth size="md" disabled={!elegidas.length} onClick={() => navegar(`/confirmar/${funcion.id}`)}>
              Continuar
            </Button>
          </Paper>
        </Grid.Col>
      </Grid>
    </div>
  );
}
