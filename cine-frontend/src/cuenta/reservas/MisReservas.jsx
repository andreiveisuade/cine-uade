import { Cargando } from "@/compartido/estado/Cargando.jsx";
import { useTitulo } from "@/compartido/hooks/useTitulo.js";
import { useSesion } from "@/cuenta/SesionDelCliente.jsx";
import { BuscarPorCodigo } from "./BuscarPorCodigo.jsx";
import { ReservasDelCliente } from "./ReservasDelCliente.jsx";

// Con sesión, la lista de la cuenta; sin sesión, buscar un ticket por su código.
export function MisReservas() {
  useTitulo("Mis reservas");
  const { usuario } = useSesion();
  if (usuario === undefined) return <Cargando />;
  return usuario ? <ReservasDelCliente /> : <BuscarPorCodigo />;
}
