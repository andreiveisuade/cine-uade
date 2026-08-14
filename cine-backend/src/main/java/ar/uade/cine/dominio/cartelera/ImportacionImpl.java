package ar.uade.cine.dominio.cartelera;

import java.time.LocalDateTime;

/**
 * Nace EN_CURSO: una importación existe porque alguien la pidió y ya se está haciendo.
 *
 * <p>El momento entra por parámetro en vez de tomarlo de {@code LocalDateTime.now()} acá
 * adentro. Es lo que deja probar la caducidad de una corrida colgada sin esperar cinco
 * minutos de reloj.
 */
public class ImportacionImpl implements Importacion {

    private int id;
    private final int paginas;
    private final LocalDateTime pedidaEn;
    private EstadoImportacion estado = EstadoImportacion.EN_CURSO;
    private LocalDateTime terminoEn;
    private int nuevas;
    private int salteadas;
    private int fallidas;
    private String detalle;

    public ImportacionImpl(int paginas, LocalDateTime pedidaEn) {
        this.paginas = paginas;
        this.pedidaEn = pedidaEn;
    }

    /** La que rearma el DAO con lo que hay en la base. */
    public ImportacionImpl(int id, int paginas, LocalDateTime pedidaEn, EstadoImportacion estado,
                           LocalDateTime terminoEn, int nuevas, int salteadas, int fallidas,
                           String detalle) {
        this(paginas, pedidaEn);
        this.id = id;
        this.estado = estado;
        this.terminoEn = terminoEn;
        this.nuevas = nuevas;
        this.salteadas = salteadas;
        this.fallidas = fallidas;
        this.detalle = detalle;
    }

    @Override
    public void terminar(int nuevas, int salteadas, int fallidas, String detalle,
                         LocalDateTime cuando) {
        this.estado = EstadoImportacion.TERMINADA;
        this.nuevas = nuevas;
        this.salteadas = salteadas;
        this.fallidas = fallidas;
        this.detalle = detalle;
        this.terminoEn = cuando;
    }

    @Override
    public void fallar(String motivo, LocalDateTime cuando) {
        this.estado = EstadoImportacion.FALLIDA;
        this.detalle = motivo;
        this.terminoEn = cuando;
    }

    @Override
    public int getId() {
        return id;
    }

    @Override
    public void setId(int id) {
        this.id = id;
    }

    @Override
    public int getPaginas() {
        return paginas;
    }

    @Override
    public LocalDateTime getPedidaEn() {
        return pedidaEn;
    }

    @Override
    public LocalDateTime getTerminoEn() {
        return terminoEn;
    }

    @Override
    public EstadoImportacion getEstado() {
        return estado;
    }

    @Override
    public int getNuevas() {
        return nuevas;
    }

    @Override
    public int getSalteadas() {
        return salteadas;
    }

    @Override
    public int getFallidas() {
        return fallidas;
    }

    @Override
    public String getDetalle() {
        return detalle;
    }
}
