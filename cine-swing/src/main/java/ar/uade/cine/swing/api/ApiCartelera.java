package ar.uade.cine.swing.api;

import ar.uade.cine.swing.api.dto.cartelera.EstadoImportador;
import ar.uade.cine.swing.api.dto.cartelera.Importacion;
import ar.uade.cine.swing.api.dto.cartelera.PedidoPelicula;
import ar.uade.cine.swing.api.dto.cartelera.Pelicula;
import lombok.RequiredArgsConstructor;

import java.time.Duration;
import java.util.List;
import java.util.Map;

// Películas, el buzón de lo que trajo el importador y el importador mismo («Cartelera» e «Importador»).
@RequiredArgsConstructor
public final class ApiCartelera {

    // La importación contesta al terminar (10-15 s, hasta 120 s en el backend): la espera normal la cortaría.
    private static final Duration ESPERA_IMPORTACION = Duration.ofSeconds(180);

    private final ClienteHttp http;

    public List<Pelicula> obtenerPeliculas(Map<String, String> filtros) {
        return http.lista("/peliculas" + Parametros.consulta(filtros), Pelicula.class);
    }

    public Pelicula crearPelicula(PedidoPelicula pelicula) {
        return http.post("/peliculas", pelicula, Pelicula.class);
    }

    public Pelicula actualizarPelicula(int id, PedidoPelicula cambios) {
        return http.put("/peliculas/" + id, cambios, Pelicula.class);
    }

    public void eliminarPelicula(int id) {
        http.delete("/peliculas/" + id);
    }

    public List<Pelicula> obtenerPeliculasPendientes() {
        return http.lista("/peliculas/pendientes", Pelicula.class);
    }

    public Pelicula confirmarPelicula(int id) {
        return http.post("/peliculas/" + id + "/confirmacion", Map.of(), Pelicula.class);
    }

    public Pelicula descartarPelicula(int id) {
        return http.post("/peliculas/" + id + "/descarte", Map.of(), Pelicula.class);
    }

    public Importacion importarAhora(int paginas) {
        return http.post("/importaciones", Map.of("paginas", paginas), Importacion.class, ESPERA_IMPORTACION);
    }

    public List<Importacion> obtenerImportaciones() {
        return http.lista("/importaciones", Importacion.class);
    }

    public EstadoImportador estadoImportador() {
        return http.get("/importaciones/estado", EstadoImportador.class);
    }
}
