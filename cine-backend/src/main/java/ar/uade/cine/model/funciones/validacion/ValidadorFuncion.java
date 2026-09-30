package ar.uade.cine.model.funciones.validacion;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;

import ar.uade.cine.model.dinero.Dinero;
import ar.uade.cine.model.funciones.Proyeccion;
import ar.uade.cine.model.funciones.Version;
import ar.uade.cine.model.rechazos.DatoInvalido;
import ar.uade.cine.model.salas.Sala;
import ar.uade.cine.model.validacion.Regla;

// Reglas de los datos de una función, compartidas con la programación y la grilla; Fabricación pura.
// Las llaman Funcion, Programacion y CriteriosGrilla: las dos últimas generan funciones con estos mismos
// datos, así que los rechazan antes de generar la primera y con el mismo texto que el alta de una suelta.
public final class ValidadorFuncion {

    // La cartelera se arma con semanas de anticipación: a más de un año es un error de tipeo (2062 por
    // 2026) y quedaría una función donde nadie la mira. Se mide en días y no en horas, así una
    // programación de un año que arranca hoy genera también su último día, a cualquier hora.
    private static final int ANIOS_DE_HORIZONTE = 1;

    private ValidadorFuncion() {
    }

    public static void formato(Sala sala, Version version, Proyeccion proyeccion, Dinero precio) {
        idiomaYProyeccion(version, proyeccion);
        // R8
        if (!proyeccion.sePuedeProyectarEn(sala.getTipo())) {
            throw new DatoInvalido("La sala " + sala.getNombre() + " no puede proyectar en 3D");
        }
        Dinero.importeValido(precio, "precio");
    }

    // Sin sala: la grilla automática la elige pase por pase, y ahí mira R8.
    public static void formato(Version version, Proyeccion proyeccion, Dinero precio) {
        idiomaYProyeccion(version, proyeccion);
        Dinero.importeValido(precio, "precio");
    }

    public static void inicio(LocalDateTime inicio) {
        Regla.objeto(inicio).obligatorio("Falta la fecha y hora de la función");
        sinSegundos(inicio.toLocalTime(), "La hora de la función");
    }

    // Una función empieza en un minuto exacto: las 20:30:46 no se anuncian en ninguna cartelera. Lo
    // piden también la hora de la programación y la apertura de la grilla, que generan las funciones.
    // sujeto es qué hora es, con artículo y mayúscula («La hora de apertura»): arranca el mensaje.
    public static void sinSegundos(LocalTime hora, String sujeto) {
        if (hora != null && !hora.equals(hora.truncatedTo(ChronoUnit.MINUTES))) {
            throw new DatoInvalido(sujeto + " tiene que ir sin segundos");
        }
    }

    // El mensaje lo pone quien pregunta: la función dice cuándo empieza; la programación y la grilla,
    // cuándo terminan, porque su último día es el de la última función que generan.
    public static void dentroDelHorizonte(LocalDate dia, LocalDate hoy, String mensaje) {
        if (dia.isAfter(hoy.plusYears(ANIOS_DE_HORIZONTE))) {
            throw new DatoInvalido(mensaje);
        }
    }

    // La API le dice idioma a lo que acá es la versión: el mensaje usa la palabra del formulario.
    private static void idiomaYProyeccion(Version version, Proyeccion proyeccion) {
        Regla.objeto(version).obligatorio("Falta el idioma");
        Regla.objeto(proyeccion).obligatorio("Falta la proyección");
    }
}
