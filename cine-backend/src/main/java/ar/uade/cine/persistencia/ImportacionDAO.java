package ar.uade.cine.persistencia;

import java.util.List;

import ar.uade.cine.dominio.cartelera.Importacion;

/**
 * El historial de corridas del importador.
 *
 * <p>No hay {@code buscarPorId} ni {@code eliminar}: a una importación nadie la busca de a
 * una —no tiene pantalla propia ni URL— y borrar el registro de lo que pasó sería borrar la
 * única respuesta a «¿cuándo trajimos cartelera por última vez?».
 */
public interface ImportacionDAO {

    void guardar(Importacion importacion);

    /** Para cerrarla cuando el importador vuelve: es lo único que cambia después del alta. */
    void actualizar(Importacion importacion);

    /**
     * Las últimas, de la más nueva a la más vieja.
     *
     * <p>Lleva tope porque el historial crece para siempre y la pantalla muestra una tabla
     * corta. Es además lo que necesita el gestor para saber si hay una corrida en curso: la
     * que importa siempre es la primera.
     */
    List<Importacion> listarUltimas(int cuantas);
}
