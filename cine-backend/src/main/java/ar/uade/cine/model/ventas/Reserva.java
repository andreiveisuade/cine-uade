package ar.uade.cine.model.ventas;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import ar.uade.cine.model.dinero.Dinero;
import ar.uade.cine.model.funciones.Funcion;
import ar.uade.cine.model.rechazos.DatoInvalido;
import ar.uade.cine.model.usuarios.Cliente;
import ar.uade.cine.model.ventas.validacion.ValidadorReserva;
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
// Contexto del patrón State: cada operación le pide la transición a EstadoReserva (`estado = estado.pagar()`).
@Entity
@Getter
public class Reserva {

    public static final int MINUTOS_PARA_PAGAR = 30;

    private static final DateTimeFormatter DIA_Y_HORA = DateTimeFormatter.ofPattern("dd/MM HH:mm");
    private static final DateTimeFormatter DIA = DateTimeFormatter.ofPattern("dd/MM");

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

    // Cobrar y cancelar a la vez pasaban los dos el chequeo y quedaba una CANCELADA con su pago;
    // con la versión el segundo UPDATE falla y ManejadorErrores lo contesta como 409.
    @Version
    @Getter(AccessLevel.NONE)
    private int version;

    protected Reserva() {
    }

    // Patrón Creador (GRASP): la reserva contiene las entradas y conoce la función que les pone precio, así que las crea ella.
    public Reserva(Funcion funcion, Cliente cliente, List<ButacaPedida> butacas, LocalDateTime creadaEn) {
        ValidadorReserva.validarFuncion(funcion);
        List<Entrada> entradas = butacas.stream()
                .map(b -> new Entrada(b.asiento(), b.tarifa(), funcion.precioDe(b.asiento(), funcion.getSala())))
                .toList();
        ValidadorReserva.validar(funcion, cliente, entradas, creadaEn);
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

    // Estático porque el bloqueo lo aplica antes de que la reserva exista.
    public static void validarTopeDeButacas(Collection<?> butacas) {
        ValidadorReserva.validarTope(butacas);
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

    // Por qué no se puede cobrar, o vacío: R5, R17 (vencida) y R19 (función empezada). Lo usan el gestor y la vista.
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

    // R13: se cancela solo lo que no se cobró. R17: la vencida ya soltó sus butacas aunque figure RESERVADA.
    public boolean esCancelable(LocalDateTime ahora) {
        return estado.esperaPago() && !estaVencida(ahora);
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

    // R18. El momento lo pone el gestor: la reserva no sabe qué hora es.
    public void registrarIngreso(LocalDateTime cuando) {
        EstadoReserva siguiente = estado.ingresar();
        if (ingresadaEn != null) {
            throw new DatoInvalido("Esa entrada ya se usó el " + ingresadaEn.format(DIA_Y_HORA));
        }
        exigirElDiaDeLaFuncion(cuando);
        estado = siguiente;
        ingresadaEn = cuando;
    }

    // Se entra solo el día de la función: la de mañana o la de ayer no deja pasar.
    private void exigirElDiaDeLaFuncion(LocalDateTime cuando) {
        LocalDate dia = funcion.getInicio().toLocalDate();
        if (!dia.equals(cuando.toLocalDate())) {
            throw new DatoInvalido("La función es el " + dia.format(DIA) + ": se entra solo ese día");
        }
    }

    // R6: cancelada o vencida, sus butacas vuelven a la venta.
    private void soltarButacasAl(EstadoReserva nuevo) {
        estado = nuevo;
        entradas.forEach(Entrada::liberar);
    }

    public boolean estaVigente() {
        return estado.ocupaButacas();
    }

    public boolean estaVencida(LocalDateTime ahora) {
        return estado.esperaPago() && creadaEn.plusMinutes(MINUTOS_PARA_PAGAR).isBefore(ahora);
    }

    @Override
    public String toString() {
        return "[" + id + "] función " + getFuncionId() + " - cliente " + getClienteId()
                + " - butacas " + entradas + " - " + estado;
    }
}
