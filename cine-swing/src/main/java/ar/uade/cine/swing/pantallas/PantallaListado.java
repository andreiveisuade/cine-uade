package ar.uade.cine.swing.pantallas;

import ar.uade.cine.swing.comun.BarraFiltros;
import ar.uade.cine.swing.comun.Tabla;

import java.util.List;
import java.util.Map;

// Un listado con filtros y su conteo; Template Method: cada pantalla dice cómo pedir y qué contar, no cuándo.
/**
 * Películas, funciones, grillas y reservas se listaban igual y cada una lo reescribía: al entrar se piden todas (para
 * el "mostrando 3 de 40") y después lo filtrado, cada vez que cambia un filtro. Todas se vuelven a pedir solo al entrar
 * o después de un cambio, no en cada filtro.
 */
public abstract class PantallaListado<T> extends Pantalla {

    protected final BarraFiltros filtros = new BarraFiltros();
    private List<T> todas = List.of();

    protected PantallaListado(String titulo, String descripcion) {
        super(titulo, descripcion);
        filtros.alCambiar(this::buscar);
    }

    /** El pedido al backend. Sin filtros ({@code null}) trae todas. */
    protected abstract List<T> obtener(Map<String, String> filtros);

    /** La tabla donde se muestra lo filtrado. */
    protected abstract Tabla<T> tabla();

    /** Lo que dice el conteo cuando ningún filtro deja nada afuera, como "40 programadas". */
    protected abstract String sinFiltrar(List<T> todas);

    protected String conteo(List<T> todas, List<T> visibles) {
        return visibles.size() == todas.size() ? sinFiltrar(todas)
                : "mostrando " + visibles.size() + " de " + todas.size();
    }

    /** Para lo que se calcula sobre todas, como un resumen arriba del listado. */
    protected void alRecargar(List<T> todas) {
    }

    /** Para lo que depende de lo que quedó a la vista, como qué botones se habilitan. */
    protected void alMostrar(List<T> visibles) {
    }

    /** Al entrar y después de un cambio: vuelve a contar todas y aplica los filtros. */
    protected final void recargar() {
        cargar(() -> obtener(null), lista -> {
            todas = lista;
            alRecargar(lista);
            buscar();
        });
    }

    protected final void buscar() {
        Map<String, String> elegidos = filtros.valores();
        cargar(() -> obtener(elegidos), visibles -> {
            tabla().mostrar(visibles);
            filtros.mostrarConteo(conteo(todas, visibles));
            alMostrar(visibles);
        });
    }
}
