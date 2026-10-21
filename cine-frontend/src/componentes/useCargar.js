import { useCallback, useEffect, useState } from "react";

// `vigente` descarta la respuesta de un pedido viejo: si «Matri» llega después de «Matrix», no lo pisa.
export function useCargar(pedir, dependencias) {
  const [vuelta, setVuelta] = useState(0);
  // La respuesta guarda con qué deps y en qué vuelta se pidió: así `cargando` y los datos de otro id se derivan
  // en el render, sin un setState extra dentro del efecto.
  const clave = JSON.stringify(dependencias);
  const [respuesta, setRespuesta] = useState({ clave: null, vuelta: -1, datos: null, error: null });

  useEffect(() => {
    let vigente = true;
    pedir().then(
      (datos) => vigente && setRespuesta({ clave, vuelta, datos, error: null }),
      (e) => vigente && setRespuesta({ clave, vuelta, datos: null, error: e.message }),
    );
    return () => {
      vigente = false;
    };
    // `pedir` es un closure nuevo en cada render; lo que decide si se vuelve a pedir son las deps que manda la
    // pantalla, resumidas en `clave`.
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [clave, vuelta]);

  const recargar = useCallback(() => setVuelta((v) => v + 1), []);
  // Al pasar de la película 3 a la 4 la pantalla no se remonta: lo de la 3 no se muestra como si fuera la 4.
  // Recargar el mismo id sí deja los datos a la vista, para no hacer parpadear la pantalla.
  const propia = respuesta.clave === clave;
  return {
    datos: propia ? respuesta.datos : null,
    error: propia ? respuesta.error : null,
    // Lo último que llegó aunque sea de otras deps: la cartelera lo usa para reordenar al cambiar de género.
    previos: respuesta.datos,
    cargando: !propia || respuesta.vuelta !== vuelta,
    recargar,
  };
}
