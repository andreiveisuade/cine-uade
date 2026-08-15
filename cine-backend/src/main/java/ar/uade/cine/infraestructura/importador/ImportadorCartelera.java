package ar.uade.cine.infraestructura.importador;

/**
 * Quien sale a buscar la cartelera real, que nunca es el cine.
 *
 * <p>Atrás de esta interfaz vive un proceso aparte —{@code cine-pelis-parser}, escrito en
 * Python— que le pregunta a TMDB qué se está dando hoy en Argentina y carga cada película
 * por HTTP contra {@code POST /api/peliculas/importadas}. Sigue siendo un cliente más del
 * sistema: entra por la misma puerta que cualquiera y pasa por las mismas reglas.
 *
 * <p>Es una interfaz por el mismo motivo que {@link ar.uade.cine.infraestructura.pasarelas.PasarelaPagos}:
 * lo que hay del otro lado es un tercero, y el gestor no tiene por qué saber cómo se le
 * habla. Acá adentro no aparece la palabra TMDB, ni una clave de API, ni una URL: el
 * backend pide «corré una importación» y el que sabe de dónde sacar las películas es el
 * importador.
 *
 * <p>Y no vive en {@code persistencia} aunque se le hable por red, por lo mismo que la
 * pasarela: no guarda ni recupera nada nuestro. Lo que el importador produce entra al
 * sistema por la API, no por acá; esto solo lo despierta.
 */
public interface ImportadorCartelera {

    /**
     * Corre una importación y vuelve cuando terminó, con lo que trajo.
     *
     * <p>Es sincrónica a propósito. Una corrida tarda diez o quince segundos y el encargado
     * está mirando la pantalla: devolverle un «ya te aviso» obligaría a inventar un
     * mecanismo para avisarle —consulta periódica, o el importador reportando de vuelta—,
     * que es mucha máquina para una espera que entra en un spinner.
     *
     * @param paginas páginas de TMDB a traer, de veinte títulos cada una
     * @throws ImportadorError si no se pudo llegar, si tardó demasiado, o si el importador
     *                         no puede trabajar (le falta el token, ya está corriendo)
     */
    Resumen importar(int paginas);

    /**
     * Si el importador está levantado y en condiciones de correr.
     *
     * <p>No tira: que no esté disponible es una respuesta, no un error. La pantalla la usa
     * para avisar antes de que alguien apriete el botón y espere en vano.
     */
    Estado consultar();

    /**
     * Lo que trajo una corrida. Los tres contadores son los mismos que el importador
     * imprime en su log, y {@code detalle} es ese log: sirve para leerlo, no para
     * consultarlo.
     *
     * @param salteadas las que ya estaban en el catálogo o que TMDB trajo sin duración
     * @param fallidas las que el backend rechazó por una regla, o que TMDB no pudo completar
     */
    record Resumen(int nuevas, int salteadas, int fallidas, double segundos, String detalle) {
    }

    /** @param detalle por qué no está disponible, o qué versión está corriendo si sí */
    record Estado(boolean disponible, String detalle) {
    }
}
