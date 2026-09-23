package ar.uade.cine.model.ventas;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import ar.uade.cine.model.dinero.Dinero;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;

/**
 * Butacas de una función a nombre de un cliente. Nace RESERVADA y desde ahí solo avanza:
 * PAGADA al cobrar, CANCELADA o EXPIRADA al liberarse sin cobrar. Las transiciones son
 * métodos con nombre ({@link #pagar}, {@link #cancelar}...) y cada uno exige el estado
 * que corresponde: no hay un {@code setEstado} que permita saltos.
 *
 * <p>Es el agregado más claro del sistema y por eso sus entradas van como relación: una
 * entrada no existe sin su reserva, se guarda y se borra con ella.
 */
@Entity
public class Reserva {

    /** Minutos que una reserva sin pagar retiene sus butacas. Regla de negocio, no detalle técnico. */
    public static final int MINUTOS_PARA_PAGAR = 30;

    /** Sin O, I, 0 ni 1: el código se tipea a mano cuando el escáner no lee. */
    private static final String ALFABETO_CODIGO = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final int LARGO_CODIGO = 8;
    private static final SecureRandom AZAR = new SecureRandom();

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    @Column(name = "funcion_id")
    private int funcionId;

    @Column(name = "cliente_id")
    private int clienteId;

    private LocalDateTime creadaEn;

    /**
     * Una entrada por butaca, fijadas al crear: cambiarlas cambiaría el total de algo ya
     * cobrado. EAGER porque nada se hace con una reserva sin sus butacas.
     */
    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @JoinColumn(name = "reserva_id", nullable = false)
    private List<Entrada> entradas = new ArrayList<>();

    /**
     * El código del QR. Es la única credencial del cliente, que no inicia sesión: por eso
     * es aleatorio y no el id, o con la reserva 5 en la mano se imprime la 6.
     */
    @Column(unique = true)
    private String codigo;

    @Enumerated(EnumType.STRING)
    private EstadoReserva estado;

    /** Cuándo entraron al cine, o {@code null} si todavía no lo hicieron. */
    private LocalDateTime ingresadaEn;

    protected Reserva() {
    }

    /** Reserva nueva: arranca RESERVADA, todavía no tiene id. */
    public Reserva(int funcionId, int clienteId, List<Entrada> entradas, LocalDateTime creadaEn) {
        this.funcionId = funcionId;
        this.clienteId = clienteId;
        this.creadaEn = creadaEn;
        this.codigo = generarCodigo();
        this.estado = EstadoReserva.RESERVADA;
        entradas.forEach(entrada -> {
            entrada.ocupar(funcionId);
            this.entradas.add(entrada);
        });
    }

    private static String generarCodigo() {
        StringBuilder codigo = new StringBuilder(LARGO_CODIGO);
        for (int i = 0; i < LARGO_CODIGO; i++) {
            codigo.append(ALFABETO_CODIGO.charAt(AZAR.nextInt(ALFABETO_CODIGO.length())));
        }
        return codigo.toString();
    }

    public int getId() {
        return id;
    }

    public int getFuncionId() {
        return funcionId;
    }

    public int getClienteId() {
        return clienteId;
    }

    public LocalDateTime getCreadaEn() {
        return creadaEn;
    }

    /** Copia defensiva: la lista de butacas de una reserva no se toca desde afuera. */
    public List<Entrada> getEntradas() {
        return new ArrayList<>(entradas);
    }

    /** Derivada de las entradas: no se guarda por separado. */
    public int getCantidadEntradas() {
        return entradas.size();
    }

    /** Suma de los precios de lista. Es un subtotal: el descuento se sabe recién al cobrar. */
    public Dinero getTotal() {
        return Dinero.sumar(entradas.stream().map(Entrada::precio).toList());
    }

    public EstadoReserva getEstado() {
        return estado;
    }

    /** R5: se cobra una vez, y solo lo que está esperando pago. */
    public void pagar() {
        exigirEsperandoPago("no se puede cobrar");
        estado = EstadoReserva.PAGADA;
    }

    /** R6 y R13: cancelar libera las butacas; una reserva cobrada no se cancela sin más. */
    public void cancelar() {
        exigirEsperandoPago("solo se puede cancelar una reserva sin cobrar");
        pasarA(EstadoReserva.CANCELADA);
    }

    /** R17: venció sin pagar; las butacas vuelven a la venta. */
    public void expirar() {
        exigirEsperandoPago("no puede expirar");
        pasarA(EstadoReserva.EXPIRADA);
    }

    /** R18: entra al cine una reserva pagada, y una sola vez. */
    public void registrarIngreso(LocalDateTime cuando) {
        if (estado != EstadoReserva.PAGADA) {
            throw new IllegalArgumentException("La reserva está " + estado
                    + ": solo se ingresa con una reserva pagada");
        }
        if (ingresadaEn != null) {
            throw new IllegalArgumentException("Esa entrada ya se usó el " + ingresadaEn);
        }
        ingresadaEn = cuando;
    }

    private void exigirEsperandoPago(String queNoSePuede) {
        if (estado != EstadoReserva.RESERVADA) {
            throw new IllegalArgumentException("La reserva está " + estado + ", " + queNoSePuede);
        }
    }

    /**
     * Los estados que dejan de retener butacas las liberan en el mismo movimiento (R6):
     * es la clase de regla que no se puede confiar a que quien actualice se acuerde.
     */
    private void pasarA(EstadoReserva nuevo) {
        estado = nuevo;
        entradas.forEach(Entrada::liberar);
    }

    /** Si sigue reteniendo sus butacas: esperando pago o ya cobrada. */
    public boolean estaVigente() {
        return estado == EstadoReserva.RESERVADA || estado == EstadoReserva.PAGADA;
    }

    /**
     * Si <em>debería</em> expirar: espera pago desde hace más de {@link #MINUTOS_PARA_PAGAR}.
     * El estado lo escribe la primera operación que se cruza con ella.
     */
    public boolean estaVencida(LocalDateTime ahora) {
        return estado == EstadoReserva.RESERVADA
                && creadaEn.plusMinutes(MINUTOS_PARA_PAGAR).isBefore(ahora);
    }

    public String getCodigo() {
        return codigo;
    }

    public LocalDateTime getIngresadaEn() {
        return ingresadaEn;
    }

    @Override
    public String toString() {
        return "[" + id + "] función " + funcionId + " - cliente " + clienteId
                + " - butacas " + entradas + " - " + estado;
    }
}
