package ar.uade.cine.servicio.cartelera;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

import ar.uade.cine.dominio.cartelera.EstadoImportacion;
import ar.uade.cine.dominio.cartelera.Importacion;
import ar.uade.cine.dominio.cartelera.ImportacionImpl;
import ar.uade.cine.importador.ImportadorCartelera;
import ar.uade.cine.importador.ImportadorError;
import ar.uade.cine.persistencia.ImportacionDAO;

/**
 * Pedir cartelera nueva, ahora.
 *
 * <p>Hasta acá el importador corría solo cada seis horas y solo se lo podía adelantar desde
 * una terminal. Este gestor es la puerta para que el encargado lo pida desde el panel, que
 * es donde está parado cuando se da cuenta de que falta cartelera.
 *
 * <p><strong>No sabe qué es TMDB.</strong> Le habla a un {@link ImportadorCartelera} y lo
 * que ese importador cargue va a entrar por {@code POST /api/peliculas/importadas} como
 * cualquier cliente, o sea pasando por {@link GestorRevisionCartelera} y por las reglas de
 * {@link GestorCartelera}. Este gestor no escribe una sola película: solo registra que se
 * pidió una corrida y cómo terminó.
 *
 * <p>Es el vecino del buzón y no del catálogo por lo mismo que
 * {@link GestorRevisionCartelera} está separado de {@link GestorCartelera}: acá no se
 * administra qué películas hay, se administra de dónde vienen.
 */
public class GestorImportaciones {

    /** Cuántas corridas muestra la pantalla. El historial crece para siempre; la tabla no. */
    private static final int HISTORIAL = 20;

    /** Una página de TMDB son veinte títulos. Más de tres es pedirle a un botón un trabajo
     * de cron: tarda demasiado y trae cosas cada vez menos parecidas a la cartelera de hoy. */
    private static final int PAGINAS_MAXIMAS = 3;

    private final ImportacionDAO importacionDAO;
    private final ImportadorCartelera importador;
    private final Duration corridaMaxima;
    private final Duration esperaEntreCorridas;

    public GestorImportaciones(ImportacionDAO importacionDAO, ImportadorCartelera importador) {
        this(importacionDAO, importador, Duration.ofMinutes(5), Duration.ofSeconds(60));
    }

    /**
     * Las dos duraciones entran por constructor para poder probarlas: esperar cinco minutos
     * de reloj para ver que una corrida colgada caduca no es un test, es una siesta.
     *
     * @param corridaMaxima cuánto puede estar EN_CURSO antes de darla por perdida
     * @param esperaEntreCorridas el mínimo entre dos corridas seguidas
     */
    public GestorImportaciones(ImportacionDAO importacionDAO, ImportadorCartelera importador,
                               Duration corridaMaxima, Duration esperaEntreCorridas) {
        this.importacionDAO = importacionDAO;
        this.importador = importador;
        this.corridaMaxima = corridaMaxima;
        this.esperaEntreCorridas = esperaEntreCorridas;
    }

    /**
     * Corre una importación y vuelve cuando terminó.
     *
     * <p>Bloquea el hilo del pedido diez o quince segundos, que es lo que tarda el
     * importador. Es a propósito: el encargado está esperando la respuesta y devolverle un
     * «después te aviso» obligaría a que el navegador pregunte cada dos segundos si ya
     * terminó, o sea a inventar tráfico para simular una espera que ya existe.
     *
     * <p>Que el importador falle <strong>no</strong> hace fallar esto: la corrida queda
     * registrada como FALLIDA con el motivo, que es un resultado y no un error del sistema.
     * Lo único que tira es lo que el encargado puede corregir: pedir mal las páginas, o
     * pedir una corrida cuando no corresponde.
     *
     * @param paginas cuántas páginas de TMDB traer, o {@code null} para una
     */
    public Importacion ejecutar(Integer paginas) {
        Importacion importacion = reservarTurno(validarPaginas(paginas));
        try {
            ImportadorCartelera.Resumen resumen = importador.importar(importacion.getPaginas());
            importacion.terminar(resumen.nuevas(), resumen.salteadas(), resumen.fallidas(),
                    resumen.detalle(), LocalDateTime.now());
        } catch (ImportadorError e) {
            importacion.fallar(e.getMessage(), LocalDateTime.now());
        }
        importacionDAO.actualizar(importacion);
        return importacion;
    }

    /**
     * Deja anotado que esta corrida arrancó, si es que puede arrancar.
     *
     * <p>Está sincronizado y aparte del método de arriba por la misma razón: es lo único
     * que dos pedidos simultáneos no pueden hacer a la vez. La corrida en sí queda afuera
     * del candado porque dura quince segundos, y encerrarla haría que el segundo pedido
     * esperara todo ese rato para recibir el rechazo que se merecía enseguida.
     *
     * <p>Alcanza con un candado porque hay un solo backend. Con dos haría falta que la
     * exclusión la garantice la base, y MySQL no puede expresarla acá: sería un UNIQUE
     * sobre "las filas EN_CURSO", que es un índice parcial y no existe.
     */
    private synchronized Importacion reservarTurno(int paginas) {
        List<Importacion> ultimas = listar();
        if (!ultimas.isEmpty()) {
            exigirQueNoHayaOtraEnCurso(ultimas.get(0));
            exigirQueHayaPasadoUnRato(ultimas.get(0));
        }
        Importacion importacion = new ImportacionImpl(paginas, LocalDateTime.now());
        importacionDAO.guardar(importacion);
        return importacion;
    }

    private static void exigirQueNoHayaOtraEnCurso(Importacion ultima) {
        if (ultima.getEstado() == EstadoImportacion.EN_CURSO) {
            throw new IllegalArgumentException(
                    "Ya hay una importación en curso: esperá a que termine");
        }
    }

    /**
     * El botón está a un clic de distancia y cada corrida son sesenta llamadas a TMDB, que
     * tiene cuota. Apretarlo dos veces seguidas no trae nada nuevo —la cartelera no cambió
     * en veinte segundos— y sí gasta el doble.
     */
    private void exigirQueHayaPasadoUnRato(Importacion ultima) {
        LocalDateTime desde = ultima.getTerminoEn();
        if (desde != null && desde.plus(esperaEntreCorridas).isAfter(LocalDateTime.now())) {
            throw new IllegalArgumentException("El importador corrió recién: esperá "
                    + esperaEntreCorridas.toSeconds() + " segundos antes de volver a pedirlo");
        }
    }

    /**
     * Las últimas corridas, de la más nueva a la más vieja.
     *
     * <p>De paso da por perdidas las que quedaron EN_CURSO de más. Se hace acá, en la
     * consulta, y no con un proceso de fondo: es el mismo criterio con el que
     * {@link GestorCartelera#listarEnCartelera()} extiende las programaciones vencidas —el
     * trabajo lo hace quien consulta—, y evita tener el único hilo de fondo del sistema
     * para vigilar una fila que casi nunca existe.
     *
     * <p>Sin esto, un backend reiniciado a mitad de corrida dejaría una importación EN_CURSO
     * para siempre, y con ella el sistema no aceptaría ninguna importación nueva nunca más.
     */
    public List<Importacion> listar() {
        List<Importacion> ultimas = importacionDAO.listarUltimas(HISTORIAL);
        LocalDateTime ahora = LocalDateTime.now();
        for (Importacion importacion : ultimas) {
            if (quedoColgada(importacion, ahora)) {
                importacion.fallar("La corrida no terminó a tiempo. Puede haber cargado "
                        + "algunas películas igual: mirá el buzón.", ahora);
                importacionDAO.actualizar(importacion);
            }
        }
        return ultimas;
    }

    private boolean quedoColgada(Importacion importacion, LocalDateTime ahora) {
        return importacion.getEstado() == EstadoImportacion.EN_CURSO
                && importacion.getPedidaEn().plus(corridaMaxima).isBefore(ahora);
    }

    /**
     * Si el importador está levantado. La pantalla lo pregunta al abrirse para poder avisar
     * antes de que alguien apriete el botón y espere una respuesta que no va a llegar.
     */
    public ImportadorCartelera.Estado estadoDelImportador() {
        return importador.consultar();
    }

    private static int validarPaginas(Integer paginas) {
        if (paginas == null) {
            return 1;
        }
        if (paginas < 1 || paginas > PAGINAS_MAXIMAS) {
            throw new IllegalArgumentException(
                    "Las páginas a importar van de 1 a " + PAGINAS_MAXIMAS);
        }
        return paginas;
    }
}
