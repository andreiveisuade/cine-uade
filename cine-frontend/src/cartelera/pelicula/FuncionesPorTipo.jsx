import { Box } from "@mantine/core";
import { useSearchParams } from "react-router";
import { PildorasFiltro } from "../PildorasFiltro.jsx";
import { ListaFunciones } from "./ListaFunciones.jsx";

// Filtra con los tipos que trae cada función; el catálogo solo da el orden. Con un tipo solo no hay qué elegir.
export function FuncionesPorTipo({ funciones, catalogo }) {
  const [consulta] = useSearchParams();
  const tipos = catalogo.filter((t) => funciones.some((f) => f.tipos.includes(t)));
  const pedido = consulta.get("tipo");
  // Un enlace viejo con un tipo que ya no está no deja la lista vacía.
  const tipo = tipos.includes(pedido) ? pedido : "";
  const visibles = tipo ? funciones.filter((f) => f.tipos.includes(tipo)) : funciones;
  return (
    <>
      {tipos.length > 1 && (
        <Box mb="md">
          <PildorasFiltro ariaLabel="Filtrar por tipo" opciones={tipos} activa={tipo}
            urlDe={(t) => ({ search: t ? `?tipo=${t}` : "" })} />
        </Box>
      )}
      {/* La key vuelve a abrir el primer día de lo filtrado: el acordeón abre solo al montarse. */}
      <ListaFunciones key={tipo} funciones={visibles} />
    </>
  );
}
