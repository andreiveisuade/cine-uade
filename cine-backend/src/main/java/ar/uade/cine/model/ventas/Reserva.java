package ar.uade.cine.model.ventas;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import ar.uade.cine.model.dinero.Dinero;
import ar.uade.cine.model.funciones.Funcion;
import ar.uade.cine.model.rechazos.DatoInvalido;
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
// Contexto del patrón State: no pregunta en qué estado está. Cada operación le pide la transición a
// EstadoReserva (`estado = estado.pagar()`), que rechaza si no corresponde, y las preguntas (¿espera el
// pago?, ¿ocupa butacas?) también las contesta el estado.
@Entity
@Getter
public class Reserva {

    public static final int MINUTOS_PARA_PAGAR = 30;

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
            throw new DatoInvalido("Hay que elegir al menos una butaca");
        }
        exigirSinRepetidas(entradas);
        this.funcion = funcion;
        this.cliente = cliente;
        this.creadaEn = creadaEn;
        this.codigo = CodigoDeAcceso.generar().valor();
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
                throw new DatoInvalido(
                        "La butaca " + entrada.codigoAsiento() + " está repetida en el pedido");
            }
        }
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
        estado = estado.pagar();
    }

    // Por qué no se puede cobrar ahora, o vacío si se puede: R5 (solo una que espera el pago), R17 (la
    // vencida ya soltó sus butacas) y R19 (función empezada). Lo usan GestorPagos para rechazar y
    // la vista para habilitar el cobro, así que el botón y el rechazo no pueden diferir.
    public Optional<String> impedimentoParaCobrar(LocalDateTime ahora) {
        if (!estado.esperaPago()) {
            return Optional.of(estado.porQueNo(EstadoReserva.NO_SE_PUEDE_COBRAR));
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

    public boolean estaPagada() {
        return estado == EstadoReserva.PAGADA;
    }

    // R13: se cancela solo lo que todavía no se cobró. Es la misma condición que cancelar().
    public boolean esCancelable() {
        return estado.esperaPago();
    }

    public boolean estaPagada() {
        return estado.estaPagada();
    }

    public void cancelar() {
        soltarButacasAl(estado.cancelar());
    }

    public void expirar() {
        soltarButacasAl(estado.expirar());
    }

    public void registrarIngreso(LocalDateTime cuando) {
        EstadoReserva siguiente = estado.ingresar();
        if (ingresadaEn != null) {
            throw new DatoInvalido("Esa entrada ya se usó el " + ingresadaEn.format(DIA_Y_HORA));
        }
        estado = siguiente;
        ingresadaEn = cuando;
    }

    // R6: cancelada o vencida, sus butacas vuelven a la venta.
    private void soltarButacasAl(EstadoReserva nuevo) {
        estado = nuevo;
        entradas.forEach(Entrada::liberar);
    }

    public boolean estaVigente() {
        return estado.ocupaButacas();
    }

    // Si debería expirar: el estado lo escribe la primera operación que se cruza con ella.
    public boolean estaVencida(LocalDateTime ahora) {
        return estado.esperaPago() && creadaEn.plusMinutes(MINUTOS_PARA_PAGAR).isBefore(ahora);
    }

    @Override
    public String toString() {
        return "[" + id + "] función " + getFuncionId() + " - cliente " + getClienteId()
                + " - butacas " + entradas + " - " + estado;
    }
}
