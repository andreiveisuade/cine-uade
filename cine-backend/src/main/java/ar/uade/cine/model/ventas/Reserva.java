package ar.uade.cine.model.ventas;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

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

@Entity
public class Reserva {

    public static final int MINUTOS_PARA_PAGAR = 30;

    // Sin O, I, 0 ni 1: el código se tipea a mano cuando el escáner no lee.
    private static final String ALFABETO_CODIGO = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final int LARGO_CODIGO = 8;
    private static final SecureRandom AZAR = new SecureRandom();

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

    protected Reserva() {
    }

    public Reserva(Funcion funcion, Cliente cliente, List<Entrada> entradas, LocalDateTime creadaEn) {
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

    public Funcion getFuncion() {
        return funcion;
    }

    public Cliente getCliente() {
        return cliente;
    }

    public int getFuncionId() {
        return funcion.getId();
    }

    public int getClienteId() {
        return cliente.getId();
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

    public Dinero getTotal() {
        return Dinero.sumar(entradas.stream().map(Entrada::precio).toList());
    }

    public EstadoReserva getEstado() {
        return estado;
    }

    public void pagar() {
        exigirEsperandoPago("no se puede cobrar");
        estado = EstadoReserva.PAGADA;
    }

    public void cancelar() {
        exigirEsperandoPago("solo se puede cancelar una reserva sin cobrar");
        pasarA(EstadoReserva.CANCELADA);
    }

    public void expirar() {
        exigirEsperandoPago("no puede expirar");
        pasarA(EstadoReserva.EXPIRADA);
    }

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

    public String getCodigo() {
        return codigo;
    }

    public LocalDateTime getIngresadaEn() {
        return ingresadaEn;
    }

    @Override
    public String toString() {
        return "[" + id + "] función " + getFuncionId() + " - cliente " + getClienteId()
                + " - butacas " + entradas + " - " + estado;
    }
}
