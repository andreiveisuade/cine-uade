package ar.uade.cine.model.promociones;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import ar.uade.cine.model.dinero.Dinero;
import ar.uade.cine.model.rechazos.DatoInvalido;
import ar.uade.cine.model.ventas.Entrada;
import ar.uade.cine.model.ventas.MedioPago;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorColumn;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.JoinColumn;
import lombok.AccessLevel;
import lombok.Getter;

// Promoción sobre entradas; Polimorfismo: cada subclase calcula su descuento, esta decide si aplica.
@Entity
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "tipo")
@Getter
public abstract class Promocion {

    // El VARCHAR(60) de la tabla: pasado, MySQL rechaza el INSERT con un 500.
    private static final int LARGO_MAXIMO_DEL_NOMBRE = 60;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    @Column(unique = true)
    private String nombre;

    private LocalDate vigenciaDesde;

    private LocalDate vigenciaHasta;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "promocion_dia", joinColumns = @JoinColumn(name = "promocion_id"))
    @Column(name = "dia")
    @Enumerated(EnumType.STRING)
    private Set<DayOfWeek> diasSemana = EnumSet.noneOf(DayOfWeek.class);

    private LocalTime horaDesde;

    private LocalTime horaHasta;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "promocion_medio", joinColumns = @JoinColumn(name = "promocion_id"))
    @Column(name = "medio")
    @Enumerated(EnumType.STRING)
    private Set<MedioPago> mediosPago = EnumSet.noneOf(MedioPago.class);

    @Getter(AccessLevel.NONE)
    private boolean activa = true;

    protected Promocion() {
    }

    // Lo común a las tres clases se valida acá y lo propio de cada una en su constructor:
    // así no se puede armar una promoción inválida, venga del gestor o de un test.
    protected Promocion(String nombre, CondicionesPromocion condiciones) {
        this.nombre = nombreValido(nombre);
        LocalDate desde = condiciones.desde();
        LocalDate hasta = condiciones.hasta();
        // Los mismos textos que el pedido: una punta que no vino falta, no está al revés.
        if (desde == null) {
            throw new DatoInvalido("Falta el inicio de la vigencia");
        }
        if (hasta == null) {
            throw new DatoInvalido("Falta el fin de la vigencia");
        }
        if (hasta.isBefore(desde)) {
            throw new DatoInvalido("La vigencia tiene que empezar antes de terminar");
        }
        LocalTime horaDesde = condiciones.horaDesde();
        LocalTime horaHasta = condiciones.horaHasta();
        // aplicaA pide desde ≤ hora ≤ hasta, así que una franja que cruza la medianoche no correría
        // nunca: se rechaza en vez de guardarla muerta. Con una sola punta, la otra queda abierta.
        if (horaDesde != null && horaHasta != null && !horaHasta.isAfter(horaDesde)) {
            throw new DatoInvalido("La franja horaria tiene que empezar antes de terminar");
        }
        this.vigenciaDesde = desde;
        this.vigenciaHasta = hasta;
        Set<DayOfWeek> dias = condiciones.dias();
        this.diasSemana = dias == null || dias.isEmpty()
                ? EnumSet.noneOf(DayOfWeek.class) : EnumSet.copyOf(dias);
        this.horaDesde = horaDesde;
        this.horaHasta = horaHasta;
        Set<MedioPago> medios = condiciones.mediosPago();
        this.mediosPago = medios == null || medios.isEmpty()
                ? EnumSet.noneOf(MedioPago.class) : EnumSet.copyOf(medios);
    }

    public abstract TipoPromocion getTipo();

    public abstract Dinero calcularDescuento(List<Entrada> entradas);

    public boolean aplicaA(LocalDateTime inicioFuncion, MedioPago medio) {
        if (!activa || inicioFuncion == null) {
            return false;
        }
        LocalDate dia = inicioFuncion.toLocalDate();
        if (dia.isBefore(vigenciaDesde) || dia.isAfter(vigenciaHasta)) {
            return false;
        }
        if (!diasSemana.isEmpty() && !diasSemana.contains(dia.getDayOfWeek())) {
            return false;
        }
        if (!mediosPago.isEmpty() && !mediosPago.contains(medio)) {
            return false;
        }
        LocalTime hora = inicioFuncion.toLocalTime();
        if (horaDesde != null && hora.isBefore(horaDesde)) {
            return false;
        }
        return horaHasta == null || !hora.isAfter(horaHasta);
    }

    protected static Dinero topear(Dinero descuento, List<Entrada> entradas) {
        return descuento.sinBajarDeCero().acotadoA(subtotalDe(entradas));
    }

    protected static Dinero subtotalDe(List<Entrada> entradas) {
        return Dinero.sumar(entradas.stream().map(Entrada::precio).toList());
    }

    public Set<DayOfWeek> getDiasSemana() {
        return diasSemana.isEmpty() ? EnumSet.noneOf(DayOfWeek.class) : EnumSet.copyOf(diasSemana);
    }

    public Set<MedioPago> getMediosPago() {
        return mediosPago.isEmpty() ? EnumSet.noneOf(MedioPago.class) : EnumSet.copyOf(mediosPago);
    }

    public boolean estaActiva() {
        return activa;
    }

    public void activar() {
        this.activa = true;
    }

    public void desactivar() {
        this.activa = false;
    }

    // Recortado acá y no en el gestor: así el nombre repetido se busca con el mismo valor que se guarda.
    private static String nombreValido(String nombre) {
        if (nombre == null || nombre.isBlank()) {
            throw new DatoInvalido("El nombre no puede estar vacío");
        }
        String limpio = nombre.trim();
        if (limpio.length() > LARGO_MAXIMO_DEL_NOMBRE) {
            throw new DatoInvalido("El nombre no puede tener más de 60 caracteres");
        }
        return limpio;
    }

    @Override
    public String toString() {
        return "[" + id + "] " + nombre + " (" + getTipo() + ")" + (activa ? "" : " - inactiva");
    }
}
