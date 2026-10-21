import { useCompra } from "./CompraEnCurso.jsx";

export function useCambiarTarifa() {
  const { setSeleccion } = useCompra();
  return (codigo, tarifa) => setSeleccion((s) => ({ ...s, butacas: { ...s.butacas, [codigo]: tarifa } }));
}
