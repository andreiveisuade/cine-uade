package ar.uade.cine.model.funciones;

import java.time.LocalDateTime;

import ar.uade.cine.model.cartelera.Pelicula;
import ar.uade.cine.model.dinero.Dinero;
import ar.uade.cine.model.funciones.validacion.ValidadorFuncion;
import ar.uade.cine.model.rechazos.DatoInvalido;
import ar.uade.cine.model.salas.Asiento;
import ar.uade.cine.model.salas.Sala;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import lombok.AccessLevel;
import lombok.Getter;

// Pase de una película en una sala a una hora; Experto: nace con formato, R8 y precio válidos, y decide R19.
@Entity
@Getter
public class Funcion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "pelicula_id", nullable = false)
    private Pelicula pelicula;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "sala_id", nullable = false)
    private Sala sala;

    @Column(name = "programacion_id")
    @Getter(AccessLevel.NONE)
    private Integer programacionId;

    private LocalDateTime inicio;

    @Embedded
    private FormatoDeFuncion formato;

    protected Funcion() {
    }

    public Funcion(Pelicula pelicula, Sala sala, LocalDateTime inicio, FormatoDeFuncion formato) {
        this(pelicula, sala, inicio, formato, null);
    }

    // R20 y R3 no son invariantes sino del alta: dependen del reloj y de las otras funciones de la sala.
    public Funcion(Pelicula pelicula, Sala sala, LocalDateTime inicio, FormatoDeFuncion formato,
                   Integer programacionId) {
        asignar(pelicula, sala, inicio, formato);
        this.programacionId = programacionId;
    }

    // Lo mismo que exige el alta. Se desprende de su grilla: ya no es el pase que ella generó.
    public void reprogramar(Pelicula pelicula, Sala sala, LocalDateTime inicio, FormatoDeFuncion formato) {
        asignar(pelicula, sala, inicio, formato);
        this.programacionId = null;
    }

    private void asignar(Pelicula pelicula, Sala sala, LocalDateTime inicio, FormatoDeFuncion formato) {
        formato.exigirProyectableEn(sala);
        ValidadorFuncion.inicio(inicio);
        this.pelicula = pelicula;
        this.sala = sala;
        this.inicio = inicio;
        this.formato = formato;
    }

    // R19 y R20 con un solo corte: la que empieza en este instante ya empezó. Estática para usarla antes de crearla.
    public static boolean yaPaso(LocalDateTime inicio, LocalDateTime ahora) {
        return !inicio.isAfter(ahora);
    }

    public boolean yaEmpezo(LocalDateTime ahora) {
        return yaPaso(inicio, ahora);
    }

    // R20 y el horizonte miran el reloj: los exige el alta, con la hora que pasa el gestor.
    public void exigirProgramableA(LocalDateTime ahora) {
        if (yaEmpezo(ahora)) {
            throw new DatoInvalido("La función no puede empezar en el pasado");
        }
        ValidadorFuncion.dentroDelHorizonte(inicio.toLocalDate(), ahora.toLocalDate(),
                "La función tiene que empezar dentro del próximo año");
    }

    public int getPeliculaId() {
        return pelicula.getId();
    }

    public int getSalaId() {
        return sala.getId();
    }

    public Version getVersion() {
        return formato.version();
    }

    public Proyeccion getProyeccion() {
        return formato.proyeccion();
    }

    public FormatoDeFuncion getFormato() {
        return formato;
    }

    @Override
    public String toString() {
        return "[" + id + "] película " + getPeliculaId() + " en sala " + getSalaId() + " - " + inicio
                + " - " + formato.proyeccion() + " " + formato.version() + " - desde $" + formato.precio();
    }

    public LocalDateTime getFin(int duracionMinutos) {
        return inicio.plusMinutes(duracionMinutos);
    }

    // Experto en el precio: cada tipo aplica su parte. Recibe la sala porque this.sala es LAZY y las vistas corren fuera de la transacción.
    public Dinero precioEn(Sala sala) {
        return sala.getTipo().aplicarA(formato.precio());
    }

    // Base × sala × butaca, con la tarifa general; la entrada aplica la suya.
    public Dinero precioDe(Asiento asiento, Sala sala) {
        return asiento.getTipo().aplicarA(precioEn(sala));
    }
}
