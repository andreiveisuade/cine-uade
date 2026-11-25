import { useState } from "react";
import { Button, Group, Input, Paper, Rating, Stack, Text, Textarea } from "@mantine/core";
import { IconBrandGoogle } from "@tabler/icons-react";
import { Link } from "react-router";
import * as api from "@/api/api-http.js";
import { useAvisar } from "@/compartido/estado/Avisos.jsx";
import { Cargando } from "@/compartido/estado/Cargando.jsx";
import { ErrorCaja } from "@/compartido/estado/ErrorCaja.jsx";
import { Vacio } from "@/compartido/estado/Vacio.jsx";
import { enfocarPrimero, faltantes } from "@/compartido/formularios/faltantes.js";
import { useEnviar } from "@/compartido/hooks/useEnviar.js";
import { useSesion } from "@/cuenta/SesionDelCliente.jsx";

const LARGO_MAXIMO = 1000;

export function FormularioComentario({ alPublicar }) {
  const { usuario } = useSesion();
  const avisar = useAvisar();
  const [puntaje, setPuntaje] = useState(0);
  const [texto, setTexto] = useState("");
  const [faltan, setFaltan] = useState({});
  const { enviar, enviando, error } = useEnviar();

  if (usuario === undefined) return <Cargando />;
  if (!usuario) {
    return (
      <Vacio accion={(
        <Button component={Link} to="/ingresar" state={{ volver: "/comentarios" }} variant="default"
          leftSection={<IconBrandGoogle size={18} />}>Ingresar con Google</Button>
      )}>Para dejar tu reseña, ingresá con tu cuenta de Google</Vacio>
    );
  }

  async function publicar(evento) {
    evento.preventDefault();
    const vacios = {
      ...(puntaje ? {} : { puntaje: "Elegí un puntaje" }),
      ...faltantes({ texto: { valor: texto, etiqueta: "tu reseña" } }),
    };
    setFaltan(vacios);
    if (Object.keys(vacios).length) {
      if (puntaje) enfocarPrimero(evento.currentTarget, vacios);
      return;
    }
    await enviar(async () => {
      await api.publicarComentario({ puntaje, texto: texto.trim() });
      setPuntaje(0);
      setTexto("");
      avisar("Publicamos tu comentario");
      alPublicar();
    });
  }

  return (
    <Paper withBorder p={{ base: "md", sm: "lg" }}>
      <Stack component="form" gap="lg" onSubmit={publicar} noValidate>
        <Input.Wrapper label="Puntaje" required labelElement="div" error={faltan.puntaje}>
          <Rating mt={4} size="xl" color="var(--cine-foco)" value={puntaje} onChange={setPuntaje} getSymbolLabel={(n) => `${n} de 5`} />
        </Input.Wrapper>
        <Textarea label="Reseña" name="texto" required rows={5} resize="vertical" maxLength={LARGO_MAXIMO}
          placeholder="Contanos cómo fue tu experiencia en el cine" value={texto} error={faltan.texto}
          description={`${texto.length} de ${LARGO_MAXIMO} caracteres`}
          inputWrapperOrder={["label", "input", "description", "error"]}
          onChange={(e) => setTexto(e.currentTarget.value)} />
        <Group justify="space-between" gap="md">
          <Text size="sm" c="dimmed">Se publica con tu nombre: {usuario.nombre}</Text>
          <Button type="submit" loading={enviando}>Publicar</Button>
        </Group>
        {error && <ErrorCaja>{error}</ErrorCaja>}
      </Stack>
    </Paper>
  );
}
