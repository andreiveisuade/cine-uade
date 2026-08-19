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
 * Butacas de una función a nombre de un cliente. Nace {@link EstadoReserva#RESERVADA} y
 * desde ahí solo avanza: a PAGADA cuando se cobra, o a CANCELADA cuando se libera sin
 * cobrar. No hay vuelta atrás entre esos dos estados finales.
 *
 * <p>Reservar y comprar son el mismo registro en distinto estado.
 *
 * <p>Es el agregado más claro del sistema y por eso sus entradas sí van mapeadas como
 * relación: una entrada no existe sin su reserva, se guarda con ella y se borra con ella.
 * {@code cascade} y {@code orphanRemoval} son lo que antes hacía a mano la transacción del
 * DAO, que insertaba la cabecera y el detalle o no insertaba ninguno.
 */
@Entity
public class Reserva {

    /**
     * Minutos que una reserva sin pagar retiene sus butacas. Es una regla de negocio, no un
     * detalle de implementación: en un cine una reserva abandonada no puede dejar una función
     * sin lugares que en realidad nadie compró.
     */
    public static final int MINUTOS_PARA_PAGAR = 30;

    /**
     * Sin O, I, 0 ni 1: el código se lee de un ticket impreso y se tipea a mano cuando el
     * escáner no lee, y ahí esos cuatro caracteres se confunden entre sí.
     */
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

    /** Cuándo se hizo. Sin esto no se puede ordenar el historial ni auditar una venta. */
    private LocalDateTime creadaEn;

    /**
     * Una entrada por butaca elegida. Se fijan al crear la reserva y no se tocan más: una
     * butaca de menos o de más cambiaría el total de algo que ya se cobró.
     *
     * <p>EAGER porque no hay nada que se haga con una reserva sin sus butacas: el ticket, el
     * total, el borderó y el mapa de la sala las piden todos.
     */
    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @JoinColumn(name = "reserva_id", nullable = false)
    private List<Entrada> entradas = new ArrayList<>();

    /**
     * El código que va en el QR de la entrada. Como el cliente no inicia sesión, es la única
     * credencial que existe en el sistema: por eso no puede ser el id, o con la reserva 5 en
     * la mano se imprime la 6.
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

    /**
     * Aleatorio y no correlativo: es la única credencial del cliente, que no inicia sesión.
     * Con un id autoincremental, quien tiene la reserva 5 imprime la 6.
     */
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

    public void setId(int id) {
        this.id = id;
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

    /**
     * Suma de los precios de lista de las butacas. Es un <em>subtotal</em>: el total
     * definitivo aparece recién al cobrar, cuando se sabe el medio de pago y con él qué
     * promoción aplica.
     */
    public Dinero getTotal() {
        return Dinero.sumar(entradas.stream().map(Entrada::precio).toList());
    }

    public EstadoReserva getEstado() {
        return estado;
    }

    /**
     * Pasa de RESERVADA a PAGADA, CANCELADA o EXPIRADA. Cuando el estado nuevo deja de
     * retener butacas, las libera en el mismo movimiento (R6): eso lo hacía el DAO con un
     * UPDATE aparte, y era la clase de regla que no se puede confiar a que quien actualice
     * se acuerde de correr también la otra sentencia.
     */
    public void setEstado(EstadoReserva estado) {
        this.estado = estado;
        if (!estaVigente()) {
            entradas.forEach(Entrada::liberar);
        }
    }

    /**
     * Si sigue reteniendo sus butacas. Lo son la que está esperando pago y la ya cobrada; no
     * lo son la cancelada ni la expirada.
     *
     * <p>Existe para no repetir la doble negación {@code estado != CANCELADA} en cada
     * consulta, que además se volvió incorrecta al aparecer EXPIRADA.
     */
    public boolean estaVigente() {
        return estado == EstadoReserva.RESERVADA || estado == EstadoReserva.PAGADA;
    }

    /**
     * Si está esperando pago desde hace más de {@link #MINUTOS_PARA_PAGAR}. Recibe el
     * instante por parámetro y no lo pide al reloj para que se pueda probar sin esperar
     * media hora.
     *
     * <p>Vencida no es lo mismo que EXPIRADA: esto dice que <em>debería</em> expirar. El
     * estado lo escribe la primera operación que se cruza con ella.
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

    public void setIngresadaEn(LocalDateTime ingresadaEn) {
        this.ingresadaEn = ingresadaEn;
    }

    @Override
    public String toString() {
        return "[" + id + "] función " + funcionId + " - cliente " + clienteId
                + " - butacas " + entradas + " - " + estado;
    }
}
