// Router por hash: #/pelicula/3 llama a la vista "pelicula" con el parámetro "3".

import { cargando, error } from "./ui.js";

function partesDelHash() {
  return location.hash.replace(/^#\/?/, "").split("/").filter(Boolean);
}

/**
 * @param rutas    objeto { nombre: async (contenedor, ...parametros) => void }
 * @param inicial  ruta a usar cuando el hash está vacío
 * @param guardia  opcional: devuelve el hash al que redirigir, o null para seguir
 */
export function iniciarRouter({ contenedor, rutas, inicial, guardia }) {
  async function navegar() {
    const partes = partesDelHash();
    const nombre = partes[0] || inicial;

    if (guardia) {
      const redirigir = guardia(nombre);
      if (redirigir && redirigir !== location.hash) {
        location.hash = redirigir;
        return;
      }
    }

    const vista = rutas[nombre];
    if (!vista) {
      contenedor.innerHTML = error(`No existe la pantalla "${nombre}"`);
      return;
    }

    contenedor.innerHTML = cargando();
    window.scrollTo(0, 0);
    try {
      await vista(contenedor, ...partes.slice(1));
    } catch (e) {
      contenedor.innerHTML = error(e.message);
    }
  }

  window.addEventListener("hashchange", navegar);
  navegar();
}

export function ir(hash) {
  location.hash = hash;
}
