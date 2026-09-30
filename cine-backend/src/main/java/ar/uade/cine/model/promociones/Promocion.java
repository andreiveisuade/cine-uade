package ar.uade.cine.model.promociones;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import ar.uade.cine.model.dinero.Dinero;
import ar.uade.cine.model.promociones.validacion.ValidadorPromocion;
import ar.uade.cine.model.tiempo.FranjaHoraria;
import ar.uade.cine.model.tiempo.Periodo;
import ar.uade.cine.model.ventas.Entrada;
import ar.uade.cine.model.ventas.MedioPago;
import jakarta.persistence.AttributeOverride;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorColumn;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Embedded;
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

// Promoción sobre entradas; Polimorfismo: cada subclase calcula su descuento y dice sus parámetros.
// Esta decide si aplica (vigencia, días, franja y medio). Cada subclase la crea su TipoPromocion
// (Factory Method) y los datos los valida ValidadorPromocion, que llaman el constructor de acá y el
// de cada subclase.
@Entity
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "tipo")
@Getter
public abstract class Promocion {

    // Sin franja es todo el día: con las dos horas en null, Hibernate deja el campo en null.
    private static final FranjaHoraria TODO_EL_DIA = new FranjaHoraria(null, null);

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    @Column(unique = true)
    private String nombre;

    // Value Objects sobre las columnas de siempre: el schema no se mueve.
    @Embedded
    @AttributeOverride(name = "desde", column = @Column(name = "vigencia_desde"))
    @AttributeOverride(name = "hasta", column = @Column(name = "vigencia_hasta"))
    private Periodo vigencia;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "promocion_dia", joinColumns = @JoinColumn(name = "promocion_id"))
    @Column(name = "dia")
    @Enumerated(EnumType.STRING)
    private Set<DayOfWeek> diasSemana = EnumSet.noneOf(DayOfWeek.class);

    @Embedded
    @AttributeOverride(name = "desde", column = @Column(name = "hora_desde"))
    @AttributeOverride(name = "hasta", column = @Column(name = "hora_hasta"))
    private FranjaHoraria franja;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "promocion_medio", joinColumns = @JoinColumn(name = "promocion_id"))
    @Column(name = "medio")
    @Enumerated(EnumType.STRING)
    private Set<MedioPago> mediosPago = EnumSet.noneOf(MedioPago.class);

    @Getter(AccessLevel.NONE)
    private boolean activa = true;

    protected Promocion() {
    }

    // Lo común a las tres clases se valida acá y lo propio de cada una en su constructor: así no se
    // puede armar una promoción inválida, venga del gestor o de un test. Por eso lo común sale primero.
    // La franja que cruza la medianoche la rechaza FranjaHoraria: aplicaA no la correría nunca.
    protected Promocion(String nombre, CondicionesPromocion condiciones, LocalDate hoy) {
        String nombreValido = ValidadorPromocion.nombre(nombre);
        Periodo vigenciaValida = ValidadorPromocion.vigencia(condiciones.desde(), condiciones.hasta());
        FranjaHoraria franjaValida = new FranjaHoraria(condiciones.horaDesde(), condiciones.horaHasta());
        Set<DayOfWeek> dias = copia(condiciones.dias(), DayOfWeek.class);
        ValidadorPromocion.exigirQueAlgunaVezAplique(vigenciaValida, dias, hoy);
        this.nombre = nombreValido;
        this.vigencia = vigenciaValida;
        this.franja = franjaValida;
        this.diasSemana = dias;
        this.mediosPago = copia(condiciones.mediosPago(), MedioPago.class);
    }

    public abstract TipoPromocion getTipo();

    // Polimorfismo: cada subclase describe sus propios parámetros y deja en null los ajenos. Reemplaza la
    // cadena de instanceof con la que la vista averiguaba qué clase tenía enfrente.
    public abstract ParametrosPromocion getParametros();

    public abstract Dinero calcularDescuento(List<Entrada> entradas);

    public boolean aplicaA(LocalDateTime inicioFuncion, MedioPago medio) {
        if (!activa || inicioFuncion == null) {
            return false;
        }
        LocalDate dia = inicioFuncion.toLocalDate();
        return vigencia.incluye(dia)
                && (diasSemana.isEmpty() || diasSemana.contains(dia.getDayOfWeek()))
                && (mediosPago.isEmpty() || mediosPago.contains(medio))
                && getFranja().incluye(inicioFuncion.toLocalTime());
    }

    public FranjaHoraria getFranja() {
        return franja == null ? TODO_EL_DIA : franja;
    }

    protected static Dinero topear(Dinero descuento, List<Entrada> entradas) {
        return descuento.sinBajarDeCero().acotadoA(subtotalDe(entradas));
    }

    protected static Dinero subtotalDe(List<Entrada> entradas) {
        return Dinero.sumar(entradas.stream().map(Entrada::precio).toList());
    }

    public Set<DayOfWeek> getDiasSemana() {
        return copia(diasSemana, DayOfWeek.class);
    }

    public Set<MedioPago> getMediosPago() {
        return copia(mediosPago, MedioPago.class);
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

    // Vacío o sin venir es «todos»: EnumSet.copyOf no acepta una colección vacía.
    private static <E extends Enum<E>> Set<E> copia(Set<E> valores, Class<E> tipo) {
        return valores == null || valores.isEmpty() ? EnumSet.noneOf(tipo) : EnumSet.copyOf(valores);
    }

    @Override
    public String toString() {
        return "[" + id + "] " + nombre + " (" + getTipo() + ")" + (activa ? "" : " - inactiva");
    }
}
