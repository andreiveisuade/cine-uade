package ar.uade.cine.model.promociones;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import ar.uade.cine.model.dinero.Dinero;
import ar.uade.cine.model.imagenes.ImagenUrl;
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
// Esta decide si aplica (vigencia, días, franja, medio); TipoPromocion crea la subclase y ValidadorPromocion valida.
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

    private String imagenUrl;

    protected Promocion() {
    }

    // Lo común se valida acá y lo propio en cada subclase, así no se arma una inválida. FranjaHoraria rechaza la que cruza medianoche.
    protected Promocion(String nombre, CondicionesPromocion condiciones, LocalDate hoy) {
        asignar(Comunes.validar(nombre, condiciones, hoy));
    }

    // Mismo orden que el alta: lo común antes que lo propio del tipo, y nada se asigna si algo falla.
    // El tipo queda: cambiarlo sería otra subclase, y la fila guarda el discriminador.
    public void editar(TipoPromocion tipo, String nombre, ParametrosPromocion parametros,
                       CondicionesPromocion condiciones, LocalDate hoy) {
        ValidadorPromocion.exigirMismoTipo(getTipo(), tipo);
        Comunes comunes = Comunes.validar(nombre, condiciones, hoy);
        cambiarParametros(parametros);
        asignar(comunes);
    }

    // Lo propio del tipo al editar: cada subclase valida todo lo suyo antes de asignar.
    protected abstract void cambiarParametros(ParametrosPromocion parametros);

    public void cambiarImagen(ImagenUrl imagen) {
        this.imagenUrl = imagen.valor();
    }

    private void asignar(Comunes comunes) {
        this.nombre = comunes.nombre();
        this.vigencia = comunes.vigencia();
        this.franja = comunes.franja();
        this.diasSemana = comunes.dias();
        this.mediosPago = comunes.medios();
    }

    private record Comunes(String nombre, Periodo vigencia, FranjaHoraria franja, Set<DayOfWeek> dias,
                           Set<MedioPago> medios) {

        static Comunes validar(String nombre, CondicionesPromocion condiciones, LocalDate hoy) {
            String nombreValido = ValidadorPromocion.nombre(nombre);
            Periodo vigenciaValida = ValidadorPromocion.vigencia(condiciones.desde(), condiciones.hasta());
            FranjaHoraria franjaValida = new FranjaHoraria(condiciones.horaDesde(), condiciones.horaHasta());
            Set<DayOfWeek> dias = copia(condiciones.dias(), DayOfWeek.class);
            ValidadorPromocion.exigirQueAlgunaVezAplique(vigenciaValida, dias, hoy);
            return new Comunes(nombreValido, vigenciaValida, franjaValida, dias,
                    copia(condiciones.mediosPago(), MedioPago.class));
        }
    }

    public abstract TipoPromocion getTipo();

    // Polimorfismo: cada subclase describe sus parámetros y deja en null los ajenos; reemplaza el instanceof de la vista.
    public abstract ParametrosPromocion getParametros();

    public abstract Dinero calcularDescuento(List<Entrada> entradas);

    // Lo que se anuncia en grande («2x1», «30 % OFF»): lo dice cada subclase, el front no lo arma.
    public abstract String beneficio();

    // Se publica mientras no termine, aunque empiece más adelante: así se anuncia antes de arrancar.
    public boolean sigueVigente(LocalDate hoy) {
        return activa && !vigencia.hasta().isBefore(hoy);
    }

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
