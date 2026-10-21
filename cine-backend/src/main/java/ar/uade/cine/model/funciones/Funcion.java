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

    // Que la película esté confirmada, que no empiece en el pasado (R20) y que no pise a otra (R3)
    // no son invariantes de la función sino del alta: dependen del buzón, del reloj y de las otras
    // funciones de la sala. Una función que quedó en el pasado sigue siendo válida (R12).
    // El formato llega ya validado, como Value Object; acá queda R8, que cruza el formato con la sala.
    public Funcion(Pelicula pelicula, Sala sala, LocalDateTime inicio, FormatoDeFuncion formato,
                   Integer programacionId) {
        formato.exigirProyectableEn(sala);
        ValidadorFuncion.inicio(inicio);
        this.pelicula = pelicula;
        this.sala = sala;
        this.inicio = inicio;
        this.formato = formato;
        this.programacionId = programacionId;
    }

    // R19 y R20 con un solo corte: la que empieza en este instante ya empezó, y nacería sin poder
    // venderse. Estática para preguntar por un horario antes de que la función exista, como hacen la
    // programación y la grilla al saltear lo que ya pasó.
    public static boolean yaPaso(LocalDateTime inicio, LocalDateTime ahora) {
        return !inicio.isAfter(ahora);
    }

    public boolean yaEmpezo(LocalDateTime ahora) {
        return yaPaso(inicio, ahora);
    }

    // R20 y el horizonte miran el reloj, así que no son invariantes: los exige el alta, con la hora que
    // le pasa el gestor.
    public void exigirProgramableA(LocalDateTime ahora) {
        if (yaEmpezo(ahora)) {
            throw new DatoInvalido("La función no puede empezar en el pasado");
        }
        ValidadorFuncion.dentroDelHorizonte(inicio.toLocalDate(), ahora.toLocalDate(),
                "La función tiene que empezar dentro del próximo año");
    }

    // No inicializa el proxy: sirve fuera de la transacción, donde se arman las vistas.
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

    public Dinero getPrecio() {
        return formato.precio();
    }

    @Override
    public String toString() {
        return "[" + id + "] película " + getPeliculaId() + " en sala " + getSalaId() + " - " + inicio
                + " - " + formato.proyeccion() + " " + formato.version() + " - desde $" + formato.precio();
    }

    public LocalDateTime getFin(int duracionMinutos) {
        return inicio.plusMinutes(duracionMinutos);
    }

    // Experto en el precio: la función tiene el base, y cada tipo (sala, butaca, tarifa) aplica su parte.
    // Base × tipo de sala es el «desde» de la cartelera. Recibe la sala aunque es la suya porque this.sala
    // es LAZY: las vistas corren fuera de la transacción y la leen por su cuenta.
    public Dinero precioEn(Sala sala) {
        return sala.getTipo().aplicarA(formato.precio());
    }

    // Base × sala × butaca: lo que muestra el mapa, con la tarifa general. La entrada le aplica la suya.
    public Dinero precioDe(Asiento asiento, Sala sala) {
        return asiento.getTipo().aplicarA(precioEn(sala));
    }
}
