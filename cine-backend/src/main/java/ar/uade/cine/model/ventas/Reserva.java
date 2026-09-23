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
 * Butacas de una función a nombre de un cliente. Nace RESERVADA y solo avanza a PAGADA,
 * CANCELADA o EXPIRADA, por métodos que exigen el estado previo: no hay {@code setEstado}.
 * Es un agregado: sus entradas se guardan y se borran con ella.
 */
@Entity
public class Reserva {

    /** Minutos que una reserva sin pagar retiene sus butacas (R17). */
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

    /** Fijadas al crear: cambiarlas alteraría el total de algo ya cobrado. */
    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @JoinColumn(name = "reserva_id", nullable = false)
    private List<Entrada> entradas = new ArrayList<>();

    /** Código del QR y única credencial del cliente: aleatorio para que no se adivine desde el id. */
    @Column(unique = true)
    private String codigo;

    @Enumerated(EnumType.STRING)
    private EstadoReserva estado;

    private LocalDateTime ingresadaEn;

    protected Reserva() {
    }

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

    public List<Entrada> getEntradas() {
        return new ArrayList<>(entradas);
    }

    public int getCantidadEntradas() {
        return entradas.size();
    }

    /** Subtotal de lista: el descuento depende del medio de pago y se aplica al cobrar. */
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

    /** R6: dejar de retener butacas y liberarlas es un solo paso. */
    private void pasarA(EstadoReserva nuevo) {
        estado = nuevo;
        entradas.forEach(Entrada::liberar);
    }

    public boolean estaVigente() {
        return estado == EstadoReserva.RESERVADA || estado == EstadoReserva.PAGADA;
    }

    /** Si <em>debería</em> expirar: el estado lo escribe la primera operación que se cruza con ella. */
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
