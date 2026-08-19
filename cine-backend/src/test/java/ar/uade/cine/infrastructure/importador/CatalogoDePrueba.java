package ar.uade.cine.infrastructure.importador;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import ar.uade.cine.model.cartelera.Clasificacion;
import ar.uade.cine.model.cartelera.Genero;
import ar.uade.cine.service.cartelera.DatosPelicula;

/**
 * Un catálogo externo que no sale a ningún lado: contesta lo que el test le haya dicho que
 * conteste.
 *
 * <p>Es el equivalente de {@code persistencia/memoria} para el otro lado del sistema. Sin
 * esto, probar el circuito de importaciones necesitaría un token de TMDB de verdad, y cada
 * corrida de {@code mvn test} gastaría cuota y traería películas distintas según el día.
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

    /** Una película válida, que el alta va a aceptar. Lo que se prueba es el circuito. */
    public static DatosPelicula pelicula(String titulo) {
        return new DatosPelicula(titulo, 120, List.of(Genero.DRAMA), Clasificacion.ATP,
                "", "", 2026, "Inglés", "", false, 7.5, 100);
    }

    public CatalogoDePrueba queTraiga(DatosPelicula... peliculas) {
        this.candidatas = Arrays.asList(peliculas);
        this.motivoDeFalla = null;
        return this;
    }

    /** Por título, que es el caso normal: lo que importa del alta es que R1 las hace únicas. */
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

    /** Cuántas veces se lo consultó: es lo que prueba que un pedido rechazado no corrió. */
    /** Lo deja como recién creado: es un bean compartido entre las clases de test. */
    public void reiniciar() {
        candidatas = List.of();
        motivoDeFalla = null;
        estado = new Estado(true, "Listo para traer cartelera");
        consultas = 0;
        paginasPedidas = 0;
    }

    public int consultas() {
        return consultas;
    }

    public int paginasPedidas() {
        return paginasPedidas;
    }
}
