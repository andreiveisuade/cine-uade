package ar.uade.cine.model.funciones;

import ar.uade.cine.model.dinero.Dinero;
import ar.uade.cine.model.rechazos.DatoInvalido;
import ar.uade.cine.model.salas.Sala;
import ar.uade.cine.model.validacion.Regla;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;

// Idioma, proyección y precio de un pase; Value Object @Embeddable que no se arma incompleto.
// Introduce Parameter Object: los tres viajaban sueltos y juntos por la función, la programación, la
// grilla y el gestor (Data Clump), y alargaban sus firmas (Long Parameter List).
// R8 no está en el constructor: cruza con la sala, que no es del formato. Lo pide la entidad que la tiene.
// Record: Hibernate 6 lo arma con su constructor canónico; sus columnas se llaman como los componentes,
// las mismas que tenían los campos sueltos.
@Embeddable
public record FormatoDeFuncion(@Enumerated(EnumType.STRING) Version version,
                               @Enumerated(EnumType.STRING) Proyeccion proyeccion,
                               Dinero precio) {

    // La API le dice idioma a lo que acá es la versión: el mensaje usa la palabra del formulario.
    public FormatoDeFuncion {
        Regla.objeto(version).obligatorio("Falta el idioma");
        Regla.objeto(proyeccion).obligatorio("Falta la proyección");
        Dinero.importeValido(precio, "precio");
    }

    // R8. Experto: el formato sabe su proyección, y la proyección qué tipos de sala la soportan.
    public void exigirProyectableEn(Sala sala) {
        if (!proyeccion.sePuedeProyectarEn(sala.getTipo())) {
            throw new DatoInvalido("La sala " + sala.getNombre() + " no puede proyectar en 3D");
        }
    }
}
