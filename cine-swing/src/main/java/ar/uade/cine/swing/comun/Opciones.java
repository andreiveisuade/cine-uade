package ar.uade.cine.swing.comun;

import ar.uade.cine.swing.api.dto.cartelera.Pelicula;
import ar.uade.cine.swing.api.dto.catalogos.MedioPago;
import ar.uade.cine.swing.api.dto.salas.Sala;

import java.util.List;

import static ar.uade.cine.swing.comun.Etiquetas.etiqueta;

// Las opciones de combo que se repiten entre pantallas: películas, salas y constantes con su etiqueta.
public final class Opciones {

    private Opciones() {
    }

    /** Para filtrar: el título alcanza. */
    public static List<Opcion<Integer>> peliculas(List<Pelicula> peliculas) {
        return peliculas.stream().map(p -> new Opcion<>(p.id(), p.titulo())).toList();
    }

    /** Para programar: la duración dice si entra en el hueco. */
    public static List<Opcion<Integer>> peliculasConDuracion(List<Pelicula> peliculas) {
        return peliculas.stream().map(p -> new Opcion<>(p.id(), p.titulo() + " (" + p.duracionMinutos() + "′)"))
                .toList();
    }

    public static List<Opcion<Integer>> salas(List<Sala> salas) {
        return salas.stream().map(s -> new Opcion<>(s.id(), s.nombre())).toList();
    }

    /** Para programar: el tipo dice si admite 3D. */
    public static List<Opcion<Integer>> salasConTipo(List<Sala> salas) {
        return salas.stream().map(s -> new Opcion<>(s.id(), s.nombre() + " — " + etiqueta(s.tipo()))).toList();
    }

    /** El medio entero y no su nombre: quien lo elige pregunta si pide autorización, sin buscarlo de nuevo. */
    public static List<Opcion<MedioPago>> medios(List<MedioPago> medios) {
        return medios.stream().map(m -> new Opcion<>(m, etiqueta(m.nombre()))).toList();
    }

    /** Constantes de un catálogo (idiomas, proyecciones, géneros) con la etiqueta que ve el encargado. */
    public static List<Opcion<String>> etiquetadas(List<String> valores) {
        return Opcion.de(valores, v -> etiqueta(v));
    }
}
