package ar.uade.cine.model.funciones;

import ar.uade.cine.model.cartelera.Pelicula;
import ar.uade.cine.model.dinero.Dinero;
import ar.uade.cine.model.rechazos.DatoInvalido;
import ar.uade.cine.model.salas.Sala;
import ar.uade.cine.model.validacion.Regla;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;

// Idioma, proyección y precio de un pase; Value Object @Embeddable, Parameter Object de la función.
// R8, R31 y R35 no van en el constructor: cruzan con la sala y la película, que no son del formato.
@Embeddable
public record FormatoDeFuncion(@Enumerated(EnumType.STRING) Version version,
                               @Enumerated(EnumType.STRING) Proyeccion proyeccion,
                               Dinero precio) {

    public FormatoDeFuncion {
        Regla.objeto(version).obligatorio("Falta el idioma");
        Regla.objeto(proyeccion).obligatorio("Falta la proyección");
        Dinero.importeValido(precio, "precio");
    }

    // R8 y R35 los decide el tipo de sala, R31 la película; la sala primero. Todos los caminos de alta pasan por acá.
    public void exigirProgramable(Pelicula pelicula, Sala sala) {
        if (!sala.getTipo().admite(proyeccion)) {
            throw new DatoInvalido("La sala " + sala.getNombre() + " no puede proyectar en " + proyeccion.getEtiqueta());
        }
        pelicula.exigirFormato(proyeccion);
    }
}
