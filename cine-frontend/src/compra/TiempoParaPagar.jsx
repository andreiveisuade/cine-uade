import { Badge } from "@mantine/core";
import { useCuentaRegresiva } from "@/compartido/hooks/useCuentaRegresiva.js";

const POCO_TIEMPO = 5 * 60;
const dosCifras = (n) => String(n).padStart(2, "0");

// Cuánto queda lo dice el backend (segundosParaPagar) y, al llegar a cero, quien lo usa le vuelve a preguntar.
// `key`: una respuesta nueva arranca la cuenta de nuevo.
export function TiempoParaPagar({ segundos, alVencer }) {
  const restantes = useCuentaRegresiva(segundos, alVencer);
  const reloj = `${dosCifras(Math.floor(restantes / 60))}:${dosCifras(restantes % 60)}`;
  return (
    <Badge color={restantes <= POCO_TIEMPO ? "red" : "ambar"} variant="light" size="lg" tt="none" h="auto" py={4}
      className="cine-cifras" style={{ alignSelf: "flex-start" }} styles={{ label: { whiteSpace: "normal" } }}>
      Te quedan <span role="timer">{reloj}</span> para pagar. Si no, se liberan las butacas
    </Badge>
  );
}
