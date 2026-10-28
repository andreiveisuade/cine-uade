import { useState } from "react";
import { Anchor, Button, Paper, Stack, Text, TextInput, Title } from "@mantine/core";
import { Link, useLocation } from "react-router";
import { ErrorCaja } from "@/compartido/estado/ErrorCaja.jsx";
import { faltantes } from "@/compartido/formularios/faltantes.js";

export function FormularioCliente({ inicial, usuario, enviando, error, alConfirmar }) {
  const [nombre, setNombre] = useState(inicial?.nombre || "");
  const [email, setEmail] = useState(inicial?.email || "");
  const [faltan, setFaltan] = useState({});
  const { pathname } = useLocation();

  function confirmar(evento) {
    evento.preventDefault();
    const vacios = faltantes({
      nombre: { valor: nombre, etiqueta: "tu nombre" },
      email: { valor: email, etiqueta: "tu email" },
    });
    setFaltan(vacios);
    if (Object.keys(vacios).length) return;
    alConfirmar({ nombre: nombre.trim(), email: email.trim() });
  }

  return (
    <Paper withBorder p="lg">
      <Title order={2} fz="h4" mb="md">Tus datos</Title>
      <form onSubmit={confirmar}>
        <Stack gap="sm">
          <TextInput label="Nombre" required value={nombre} error={faltan.nombre}
            onChange={(e) => setNombre(e.currentTarget.value)} />
          <TextInput label="Email" type="email" required value={email} error={faltan.email}
            onChange={(e) => setEmail(e.currentTarget.value)} />
          <Button type="submit" size="md" mt="xs" loading={enviando}>Confirmar reserva</Button>
          {error && <ErrorCaja>{error}</ErrorCaja>}
          {usuario === null && (
            <Text size="xs" c="dimmed">
              Para pagar online hace falta una cuenta:{" "}
              <Anchor component={Link} to="/ingresar" state={{ volver: pathname }} size="xs">ingresá con Google</Anchor>, antes o después de reservar.
            </Text>
          )}
        </Stack>
      </form>
    </Paper>
  );
}
