package ar.uade.cine.dominio.ventas;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Reservar y comprar son el mismo registro en distinto estado: nace RESERVADA
 * y pasa a PAGADA cuando el cliente abona.
 */
public class ReservaImpl implements Reserva {

    /**
     * Sin O, I, 0 ni 1: el código se lee de un ticket impreso y se tipea a mano cuando
     * el escáner no lee, y ahí esos cuatro caracteres se confunden entre sí.
     */
    private static final String ALFABETO_CODIGO = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final int LARGO_CODIGO = 8;
    private static final SecureRandom AZAR = new SecureRandom();

    private int id;
    private final int funcionId;
    private final int clienteId;
    private final LocalDateTime creadaEn;
    private final List<Entrada> entradas = new ArrayList<>();
    private final String codigo;
    private EstadoReserva estado;
    private LocalDateTime ingresadaEn;

    public ReservaImpl(int funcionId, int clienteId, List<Entrada> entradas, LocalDateTime creadaEn) {
        this(funcionId, clienteId, entradas, creadaEn, generarCodigo());
    }

    private ReservaImpl(int funcionId, int clienteId, List<Entrada> entradas,
                        LocalDateTime creadaEn, String codigo) {
        this.funcionId = funcionId;
        this.clienteId = clienteId;
        this.creadaEn = creadaEn;
        this.entradas.addAll(entradas);
        this.codigo = codigo;
        this.estado = EstadoReserva.RESERVADA;
    }

    public ReservaImpl(int id, int funcionId, int clienteId, List<Entrada> entradas,
                       EstadoReserva estado, LocalDateTime creadaEn, String codigo) {
        this(funcionId, clienteId, entradas, creadaEn, codigo);
        this.id = id;
        this.estado = estado;
    }

    /**
     * Aleatorio y no correlativo: es la única credencial del cliente, que no inicia
     * sesión. Con un id autoincremental, quien tiene la reserva 5 imprime la 6.
     */
    private static String generarCodigo() {
        StringBuilder codigo = new StringBuilder(LARGO_CODIGO);
        for (int i = 0; i < LARGO_CODIGO; i++) {
            codigo.append(ALFABETO_CODIGO.charAt(AZAR.nextInt(ALFABETO_CODIGO.length())));
        }
        return codigo.toString();
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
    public int getFuncionId() {
        return funcionId;
    }

    @Override
    public int getClienteId() {
        return clienteId;
    }

    @Override
    public LocalDateTime getCreadaEn() {
        return creadaEn;
    }

    @Override
    public List<Entrada> getEntradas() {
        return new ArrayList<>(entradas);
    }

    @Override
    public void agregarEntrada(Entrada entrada) {
        entradas.add(entrada);
    }

    @Override
    public int getCantidadEntradas() {
        return entradas.size();
    }

    @Override
    public double getTotal() {
        return entradas.stream().mapToDouble(Entrada::precio).sum();
    }

    @Override
    public EstadoReserva getEstado() {
        return estado;
    }

    @Override
    public void setEstado(EstadoReserva estado) {
        this.estado = estado;
    }

    @Override
    public boolean estaVigente() {
        return estado == EstadoReserva.RESERVADA || estado == EstadoReserva.PAGADA;
    }

    @Override
    public boolean estaVencida(LocalDateTime ahora) {
        return estado == EstadoReserva.RESERVADA
                && creadaEn.plusMinutes(MINUTOS_PARA_PAGAR).isBefore(ahora);
    }

    @Override
    public String getCodigo() {
        return codigo;
    }

    @Override
    public LocalDateTime getIngresadaEn() {
        return ingresadaEn;
    }

    @Override
    public void setIngresadaEn(LocalDateTime ingresadaEn) {
        this.ingresadaEn = ingresadaEn;
    }

    @Override
    public String toString() {
        return "[" + id + "] función " + funcionId + " - cliente " + clienteId
                + " - butacas " + entradas + " - " + estado;
    }
}
