package ar.uade.cine.model.funciones;

import ar.uade.cine.model.dinero.Dinero;
import ar.uade.cine.model.rechazos.DatoInvalido;
import ar.uade.cine.model.salas.Sala;
import ar.uade.cine.model.validacion.Regla;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;

// Idioma, proyección y precio de un pase; Value Object @Embeddable, Parameter Object de la función.
// R8 no va en el constructor: cruza con la sala, que no es del formato.
@Embeddable
public record FormatoDeFuncion(@Enumerated(EnumType.STRING) Version version,
                               @Enumerated(EnumType.STRING) Proyeccion proyeccion,
                               Dinero precio) {

    public FormatoDeFuncion {
        Regla.objeto(version).obligatorio("Falta el idioma");
        Regla.objeto(proyeccion).obligatorio("Falta la proyección");
        Dinero.importeValido(precio, "precio");
    }

    // R8. Experto: el formato sabe su proyección, y la proyección qué salas la soportan.
    public void exigirProyectableEn(Sala sala) {
        if (!proyeccion.sePuedeProyectarEn(sala.getTipo())) {
            throw new DatoInvalido("La sala " + sala.getNombre() + " no puede proyectar en 3D");
        }
    }
}
