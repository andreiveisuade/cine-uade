package ar.uade.cine.importador;

/**
 * Un importador que no sale a ningún lado: contesta lo que el test le haya dicho que
 * conteste.
 *
 * <p>Es el equivalente de {@code persistencia/memoria} para el otro lado del sistema. Sin
 * esto, probar el circuito de importaciones necesitaría el contenedor de Python levantado y
 * un token de TMDB de verdad, y cada corrida de {@code mvn test} gastaría cuota y traería
 * películas distintas según el día.
 */
public class ImportadorDePrueba implements ImportadorCartelera {

    private Resumen resumen = new Resumen(0, 0, 0, 0, null);
    private String motivoDeFalla;
    private Estado estado = new Estado(true, "Listo para traer cartelera");
    private int corridas;
    private int paginasPedidas;

    @Override
    public Resumen importar(int paginas) {
        corridas++;
        paginasPedidas = paginas;
        if (motivoDeFalla != null) {
            throw new ImportadorError(motivoDeFalla);
        }
        return resumen;
    }

    @Override
    public Estado consultar() {
        return estado;
    }

    public ImportadorDePrueba queTraiga(int nuevas, int salteadas, int fallidas) {
        this.resumen = new Resumen(nuevas, salteadas, fallidas, 1.5, "+ Una película");
        this.motivoDeFalla = null;
        return this;
    }

    public ImportadorDePrueba queFalleCon(String motivo) {
        this.motivoDeFalla = motivo;
        return this;
    }

    public ImportadorDePrueba queEste(boolean disponible, String detalle) {
        this.estado = new Estado(disponible, detalle);
        return this;
    }

    /** Cuántas veces se lo llamó: es lo que prueba que un pedido rechazado no corrió. */
    public int corridas() {
        return corridas;
    }

    public int paginasPedidas() {
        return paginasPedidas;
    }
}
