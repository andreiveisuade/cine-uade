package ar.uade.cine.infrastructure.importador;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import ar.uade.cine.model.cartelera.Clasificacion;
import ar.uade.cine.model.cartelera.Genero;
import ar.uade.cine.service.cartelera.DatosPelicula;

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
