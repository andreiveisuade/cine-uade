package ar.uade.cine.model.promociones;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import ar.uade.cine.model.dinero.Dinero;
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

@Entity
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "tipo")
@Getter
public abstract class Promocion {

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
        if (nombre == null || nombre.isBlank()) {
            throw new IllegalArgumentException("La promoción necesita un nombre");
        }
        // El VARCHAR(60) de la tabla: pasado, MySQL rechaza el INSERT con un 500.
        if (nombre.length() > 60) {
            throw new IllegalArgumentException("El nombre no puede tener más de 60 caracteres");
        }
        LocalDate desde = condiciones.desde();
        LocalDate hasta = condiciones.hasta();
        if (desde == null || hasta == null || hasta.isBefore(desde)) {
            throw new IllegalArgumentException("La vigencia tiene que empezar antes de terminar");
        }
        this.nombre = nombre;
        this.vigenciaDesde = desde;
        this.vigenciaHasta = hasta;
        Set<DayOfWeek> dias = condiciones.dias();
        this.diasSemana = dias == null || dias.isEmpty()
                ? EnumSet.noneOf(DayOfWeek.class) : EnumSet.copyOf(dias);
        this.horaDesde = condiciones.horaDesde();
        this.horaHasta = condiciones.horaHasta();
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

    public void setActiva(boolean activa) {
        this.activa = activa;
    }

    @Override
    public String toString() {
        return "[" + id + "] " + nombre + " (" + getTipo() + ")" + (activa ? "" : " - inactiva");
    }
}
