package ar.uade.cine.model.ventas;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import ar.uade.cine.model.dinero.Dinero;
import ar.uade.cine.model.funciones.Funcion;
import ar.uade.cine.model.usuarios.Cliente;
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
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Version;
import lombok.AccessLevel;
import lombok.Getter;

// Reserva de butacas sin repetir (R5, R6, R13, R17-R19); Experto en sus transiciones, @Version por carreras.
@Entity
@Getter
public class Reserva {

    public static final int MINUTOS_PARA_PAGAR = 30;

    // Sin O, I, 0 ni 1: el código se tipea a mano cuando el escáner no lee.
    private static final String ALFABETO_CODIGO = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final int LARGO_CODIGO = 8;
    private static final SecureRandom AZAR = new SecureRandom();

    // Como la lee el acomodador en la puerta: el toString de LocalDateTime traía segundos y nanos.
    private static final DateTimeFormatter DIA_Y_HORA = DateTimeFormatter.ofPattern("dd/MM HH:mm");

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "funcion_id", nullable = false)
    private Funcion funcion;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "cliente_id", nullable = false)
    private Cliente cliente;

    private LocalDateTime creadaEn;

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @JoinColumn(name = "reserva_id", nullable = false)
    private List<Entrada> entradas = new ArrayList<>();

    @Column(unique = true)
    private String codigo;

    @Enumerated(EnumType.STRING)
    private EstadoReserva estado;

    private LocalDateTime ingresadaEn;

    // Cobrar, cancelar y expirar leen la reserva, miran su estado y lo escriben. Sin esto,
    // cobrar y cancelar a la vez pasaban los dos el chequeo y ganaba el último en escribir:
    // una reserva CANCELADA con su pago adentro. Con la versión, el segundo UPDATE no
    // encuentra la fila que leyó y falla, y ManejadorErrores lo contesta como 409.
    // Solo acá: es la única entidad con transiciones de estado que compiten entre sí.
    @Version
    @Getter(AccessLevel.NONE)
    private int version;

    protected Reserva() {
    }

    public Reserva(Funcion funcion, Cliente cliente, List<Entrada> entradas, LocalDateTime creadaEn) {
        if (entradas.isEmpty()) {
            throw new IllegalArgumentException("Hay que elegir al menos una butaca");
        }
        exigirSinRepetidas(entradas);
        this.funcion = funcion;
        this.cliente = cliente;
        this.creadaEn = creadaEn;
        this.codigo = generarCodigo();
        this.estado = EstadoReserva.RESERVADA;
        entradas.forEach(entrada -> {
            entrada.ocupar(funcion.getId());
            this.entradas.add(entrada);
        });
    }

    // "a1" y "A1" son dos claves del pedido y la misma butaca. Sin esto las dos entradas
    // chocaban contra el UNIQUE y el cliente leía un 409 de butaca tomada por otro.
    private static void exigirSinRepetidas(List<Entrada> entradas) {
        Set<Integer> asientos = new HashSet<>();
        for (Entrada entrada : entradas) {
            if (!asientos.add(entrada.asientoId())) {
                throw new IllegalArgumentException(
                        "La butaca " + entrada.codigoAsiento() + " está repetida en el pedido");
            }
        }
    }

    private static String generarCodigo() {
        StringBuilder codigo = new StringBuilder(LARGO_CODIGO);
        for (int i = 0; i < LARGO_CODIGO; i++) {
            codigo.append(ALFABETO_CODIGO.charAt(AZAR.nextInt(ALFABETO_CODIGO.length())));
        }
        return codigo.toString();
    }

    // Se tipea a mano cuando el escáner no lee: se busca como se generó, sin espacios y en mayúsculas.
    public static String normalizarCodigo(String codigo) {
        return codigo.trim().toUpperCase();
    }

    public int getFuncionId() {
        return funcion.getId();
    }

    public int getClienteId() {
        return cliente.getId();
    }

    public List<Entrada> getEntradas() {
        return new ArrayList<>(entradas);
    }

    public int getCantidadEntradas() {
        return entradas.size();
    }

    public Dinero getTotal() {
        return Dinero.sumar(entradas.stream().map(Entrada::precio).toList());
    }

    public void pagar() {
        exigirEsperandoPago("no se puede cobrar");
        estado = EstadoReserva.PAGADA;
    }

    // Por qué no se puede cobrar ahora, o vacío si se puede: R5 (solo una RESERVADA), R17 (la
    // vencida ya soltó sus butacas) y R19 (función empezada). Lo usan GestorPagos para rechazar y
    // la vista para habilitar el cobro, así que el botón y el rechazo no pueden diferir.
    public Optional<String> impedimentoParaCobrar(LocalDateTime ahora) {
        if (estado != EstadoReserva.RESERVADA) {
            return Optional.of("La reserva está " + estado.etiqueta() + ": no se puede cobrar");
        }
        if (estaVencida(ahora)) {
            return Optional.of("La reserva " + id + " venció: sus butacas volvieron a estar disponibles");
        }
        if (funcion.yaEmpezo(ahora)) {
            return Optional.of("La función ya empezó: no se puede cobrar la reserva " + id);
        }
        return Optional.empty();
    }

    public boolean esCobrable(LocalDateTime ahora) {
        return impedimentoParaCobrar(ahora).isEmpty();
    }

    // R13: se cancela solo lo que todavía no se cobró. Es la misma condición que cancelar().
    public boolean esCancelable() {
        return estado == EstadoReserva.RESERVADA;
    }

    public void cancelar() {
        if (!esCancelable()) {
            throw new IllegalArgumentException("La reserva está " + estado.etiqueta()
                    + ": solo se puede cancelar una reserva sin cobrar");
        }
        pasarA(EstadoReserva.CANCELADA);
    }

    public void expirar() {
        exigirEsperandoPago("no puede expirar");
        pasarA(EstadoReserva.EXPIRADA);
    }

    public void registrarIngreso(LocalDateTime cuando) {
        if (estado != EstadoReserva.PAGADA) {
            throw new IllegalArgumentException("La reserva está " + estado.etiqueta()
                    + ": solo se ingresa con una reserva pagada");
        }
        if (ingresadaEn != null) {
            throw new IllegalArgumentException("Esa entrada ya se usó el " + ingresadaEn.format(DIA_Y_HORA));
        }
        ingresadaEn = cuando;
    }

    private void exigirEsperandoPago(String queNoSePuede) {
        if (estado != EstadoReserva.RESERVADA) {
            throw new IllegalArgumentException("La reserva está " + estado.etiqueta() + ": " + queNoSePuede);
        }
    }

    private void pasarA(EstadoReserva nuevo) {
        estado = nuevo;
        entradas.forEach(Entrada::liberar);
    }

    public boolean estaVigente() {
        return estado == EstadoReserva.RESERVADA || estado == EstadoReserva.PAGADA;
    }

    // Si debería expirar: el estado lo escribe la primera operación que se cruza con ella.
    public boolean estaVencida(LocalDateTime ahora) {
        return estado == EstadoReserva.RESERVADA
                && creadaEn.plusMinutes(MINUTOS_PARA_PAGAR).isBefore(ahora);
    }

    @Override
    public String toString() {
        return "[" + id + "] función " + getFuncionId() + " - cliente " + getClienteId()
                + " - butacas " + entradas + " - " + estado;
    }
}
