import { Alert, Stack, Title } from "@mantine/core";
import { Navigate } from "react-router";
import { Cargando } from "@/compartido/estado/Cargando.jsx";
import { useTitulo } from "@/compartido/hooks/useTitulo.js";
import { CambiarClave } from "./mi-cuenta/CambiarClave.jsx";
import { DarDeBaja } from "./mi-cuenta/DarDeBaja.jsx";
import { DatosDeCuenta } from "./mi-cuenta/DatosDeCuenta.jsx";
import { useSesion } from "./SesionDelCliente.jsx";

export function MiCuenta() {
  useTitulo("Mi cuenta");
  const { usuario } = useSesion();
  if (usuario === undefined) return <Cargando />;
  if (!usuario) return <Navigate to="/ingresar" replace />;
  return (
    <Stack gap="lg" maw={480} mx="auto" pt={{ base: "md", sm: "xl" }}>
      <Title order={1}>Mi cuenta</Title>
      {usuario.debeCambiarClave && (
        <Alert color="yellow" variant="light">Entraste con una clave temporal: cambiala para seguir</Alert>
      )}
      <DatosDeCuenta />
      <CambiarClave />
      <DarDeBaja />
    </Stack>
  );
}
