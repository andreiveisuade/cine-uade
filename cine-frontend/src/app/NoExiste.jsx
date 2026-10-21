import { useLocation } from "react-router";
import { ErrorCaja } from "@/compartido/estado/ErrorCaja.jsx";

export function NoExiste() {
  const { pathname } = useLocation();
  return <ErrorCaja>No existe la pantalla "{pathname.split("/")[1]}"</ErrorCaja>;
}
