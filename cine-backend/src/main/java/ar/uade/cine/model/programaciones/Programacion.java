package ar.uade.cine.model.programaciones;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import ar.uade.cine.model.cartelera.Pelicula;
import ar.uade.cine.model.dinero.Dinero;
import ar.uade.cine.model.funciones.Funcion;
import ar.uade.cine.model.funciones.Proyeccion;
import ar.uade.cine.model.funciones.Version;
import ar.uade.cine.model.funciones.validacion.ValidadorFuncion;
import ar.uade.cine.model.programaciones.validacion.ValidadorProgramacion;
import ar.uade.cine.model.rechazos.DatoInvalido;
import ar.uade.cine.model.salas.Sala;
import ar.uade.cine.model.tiempo.Periodo;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import lombok.AccessLevel;
import lombok.Getter;

// Película repetida en una sala a una hora por un período; Experto: nace válida y sabe qué falta generar.
@Entity
@Getter
public class Programacion {

    private static final int HORIZONTE_DIAS = 14;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "pelicula_id", nullable = false)
    private Pelicula pelicula;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "sala_id", nullable = false)
    private Sala sala;

    // Sus dos columnas se llaman como los campos del record: desde y hasta.
    @Embedded
    private Periodo periodo;

    private LocalTime horaInicio;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "programacion_dia",
            joinColumns = @JoinColumn(name = "programacion_id"))
    @Column(name = "dia")
    @Enumerated(EnumType.STRING)
    private Set<DayOfWeek> diasSemana = EnumSet.noneOf(DayOfWeek.class);

    @Enumerated(EnumType.STRING)
    private Version version;

    @Enumerated(EnumType.STRING)
    private Proyeccion proyeccion;

    private Dinero precio;

    @Getter(AccessLevel.NONE)
    private boolean activa = true;

    // Se guarda y no se deriva de la última función: si esa se borra o mueve, se regenerarían fechas.
    private LocalDate generadaHasta;

    protected Programacion() {
    }

    // La película y la sala llegan ya buscadas y con su 404, como en el alta de una función suelta:
    // hacen falta para R8. Se valida todo antes de asignar nada.
    public Programacion(Pelicula pelicula, Sala sala, LocalDate desde, LocalDate hasta,
                        LocalTime horaInicio, Set<DayOfWeek> diasSemana, Version version,
                        Proyeccion proyeccion, Dinero precio) {
        Periodo validado = ValidadorProgramacion.periodo(desde, hasta);
        ValidadorProgramacion.hora(horaInicio);
        Set<DayOfWeek> dias = ValidadorProgramacion.dias(diasSemana, validado);
        ValidadorFuncion.formato(sala, version, proyeccion, precio);
        this.pelicula = pelicula;
        this.sala = sala;
        this.periodo = validado;
        this.horaInicio = horaInicio;
        this.diasSemana = dias;
        this.version = version;
        this.proyeccion = proyeccion;
        this.precio = precio;
    }

    // R20 y el horizonte miran el reloj, así que los exige el alta, con la hora que le pasa el gestor.
    // Solo en una cerrada: una abierta genera de a catorce días, siempre por delante.
    public void exigirGenerableA(LocalDateTime ahora) {
        LocalDate hasta = periodo.hasta();
        if (hasta == null) {
            return;
        }
        // R20: un rango cerrado que ya pasó entero se daría de alta vacío, sin nada que extender.
        if (horarios(hasta).stream().allMatch(inicio -> Funcion.yaPaso(inicio, ahora))) {
            throw new DatoInvalido(
                    "Todos los horarios del rango ya pasaron: la grilla no generaría funciones");
        }
        ValidadorFuncion.dentroDelHorizonte(hasta, ahora.toLocalDate(),
                "El rango tiene que terminar dentro del próximo año");
    }

    public List<LocalDateTime> horarios(LocalDate tope) {
        LocalDate hasta = periodo.hasta();
        LocalDate fin = hasta == null || tope.isBefore(hasta) ? tope : hasta;
        List<LocalDateTime> momentos = new ArrayList<>();
        for (LocalDate dia = periodo.desde(); !dia.isAfter(fin); dia = dia.plusDays(1)) {
            if (diasSemana.isEmpty() || diasSemana.contains(dia.getDayOfWeek())) {
                momentos.add(LocalDateTime.of(dia, horaInicio));
            }
        }
        return momentos;
    }

    public LocalDate topePara(LocalDate hoy) {
        return periodo.hasta() != null ? periodo.hasta() : hoy.plusDays(HORIZONTE_DIAS);
    }

    public boolean estaAlDia(LocalDate hoy) {
        return generadaHasta != null && !generadaHasta.isBefore(topePara(hoy));
    }

    // Por fecha procesada y no por función existente: una que chocó se reintentaría siempre.
    public List<LocalDateTime> horariosSinGenerar(LocalDate tope) {
        return horarios(tope).stream()
                .filter(inicio -> generadaHasta == null || inicio.toLocalDate().isAfter(generadaHasta))
                .toList();
    }

    // El tope y no la última generada: las que chocaron también quedan procesadas.
    public void marcarGeneradaHasta(LocalDate tope) {
        this.generadaHasta = tope;
    }

    // No inicializa el proxy: sirve fuera de la transacción, donde se arman las vistas.
    public int getPeliculaId() {
        return pelicula.getId();
    }

    public int getSalaId() {
        return sala.getId();
    }

    public Set<DayOfWeek> getDiasSemana() {
        return Collections.unmodifiableSet(
                diasSemana.isEmpty() ? EnumSet.noneOf(DayOfWeek.class) : EnumSet.copyOf(diasSemana));
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

    @Override
    public String toString() {
        LocalDate hasta = periodo.hasta();
        return "[" + id + "] película " + getPeliculaId() + " en sala " + getSalaId() + " - " + horaInicio
                + " del " + periodo.desde() + (hasta == null ? " en adelante" : " al " + hasta)
                + " - generada hasta " + (generadaHasta == null ? "nunca" : generadaHasta);
    }
}
