import { useEffect, useState } from "react";
import { Anchor, Button, Text } from "@mantine/core";
import { IconBrandGoogle } from "@tabler/icons-react";
import { Navigate, useLocation, useSearchParams } from "react-router";
import * as api from "@/api/api-http.js";
import { useTitulo } from "@/compartido/hooks/useTitulo.js";
import { useCargar } from "@/compartido/hooks/useCargar.js";
import { ErrorCaja } from "@/compartido/estado/ErrorCaja.jsx";
import { FormularioCentrado } from "./FormularioCentrado.jsx";
import { useSesion } from "./SesionDelCliente.jsx";

// Ir a Google recarga la página entera y el state del router se pierde: la compra a la que volver queda acá.
const VUELTA = "cine.vuelta";

export function Ingresar() {
  useTitulo("Ingresar");
  const { usuario } = useSesion();
  const { state } = useLocation();
  // El backend vuelve a #/ingresar con ?error= si Google o el cine rechazaron el ingreso.
  const rechazo = useSearchParams()[0].get("error");
  const [volver] = useState(() => sessionStorage.getItem(VUELTA) ?? state?.volver);
  const { datos: google, error } = useCargar(api.ingresoConGoogle, []);

  useEffect(() => sessionStorage.removeItem(VUELTA), []);

  function irAGoogle(evento) {
    evento.preventDefault();
    if (volver) sessionStorage.setItem(VUELTA, volver);
    api.irAGoogle();
  }

  // Ya con sesión (recién vuelto de Google o entrando de nuevo): a la compra que dejó, o a sus reservas.
  if (usuario) return <Navigate to={volver || "/mis-reservas"} replace />;
  return (
    <FormularioCentrado titulo="Ingresar" alEnviar={irAGoogle} bajada={volver?.startsWith("/funcion/")
      ? "Ingresá con Google para terminar la compra: tus butacas te esperan unos minutos."
      : "Con tu cuenta de Google comprás entradas, ves tus reservas, las pagás online o las cancelás. Si ya compraste antes, usá el mismo email."}>
      <Button type="submit" size="md" variant="default" leftSection={<IconBrandGoogle size={18} />}
        disabled={!google?.disponible}>Ingresar con Google</Button>
      <Text size="xs" c="dimmed" ta="center">
        Al ingresar aceptás la <Anchor href="/privacidad.html" inherit>política de privacidad</Anchor>
      </Text>
      {google && !google.disponible && (
        <Text size="sm" c="dimmed" ta="center">El ingreso con Google no está disponible en este momento: probá más tarde o comprá en la boletería</Text>
      )}
      {(rechazo || error) && <ErrorCaja>{rechazo || error}</ErrorCaja>}
    </FormularioCentrado>
  );
}
