package ar.uade.cine.infrastructure.importador;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import ar.uade.cine.model.cartelera.Clasificacion;
import ar.uade.cine.model.cartelera.Genero;
import ar.uade.cine.service.cartelera.DatosPelicula;

/**
 * Catálogo externo que contesta lo que el test le diga: sin él, cada {@code mvn test} gastaría
 * cuota de TMDB y traería películas distintas según el día.
 */
public class CatalogoDePrueba implements CatalogoExterno {

    private List<DatosPelicula> candidatas = List.of();
    private String motivoDeFalla;
    private Estado estado = new Estado(true, "Listo para traer cartelera");
    private int consultas;
    private int paginasPedidas;

    @Override
    public List<DatosPelicula> enCartelera(int paginas) {
        consultas++;
        paginasPedidas = paginas;
        if (motivoDeFalla != null) {
            throw new ImportadorError(motivoDeFalla);
        }
        return new ArrayList<>(candidatas);
    }

    @Override
    public Estado consultar() {
        return estado;
    }

    public static DatosPelicula pelicula(String titulo) {
        return new DatosPelicula(titulo, 120, List.of(Genero.DRAMA), Clasificacion.ATP,
                "", "", 2026, "Inglés", "", false, 7.5, 100);
    }

    public CatalogoDePrueba queTraiga(DatosPelicula... peliculas) {
        this.candidatas = Arrays.asList(peliculas);
        this.motivoDeFalla = null;
        return this;
    }

    /** R1 las hace únicas por título. */
    public CatalogoDePrueba queTraiga(String... titulos) {
        return queTraiga(Arrays.stream(titulos).map(CatalogoDePrueba::pelicula)
                .toArray(DatosPelicula[]::new));
    }

    public CatalogoDePrueba queFalleCon(String motivo) {
        this.motivoDeFalla = motivo;
        return this;
    }

    public CatalogoDePrueba queEste(boolean disponible, String detalle) {
        this.estado = new Estado(disponible, detalle);
        return this;
    }

    /** Es un bean compartido entre las clases de test. */
    public void reiniciar() {
        candidatas = List.of();
        motivoDeFalla = null;
        estado = new Estado(true, "Listo para traer cartelera");
        consultas = 0;
        paginasPedidas = 0;
    }

    /** Prueba que un pedido rechazado no llegó a correr. */
    public int consultas() {
        return consultas;
    }

    public int paginasPedidas() {
        return paginasPedidas;
    }
}
