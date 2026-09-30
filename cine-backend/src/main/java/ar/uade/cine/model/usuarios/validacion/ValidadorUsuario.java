package ar.uade.cine.model.usuarios.validacion;

import ar.uade.cine.model.validacion.Regla;

// Valida el nombre de un cliente o empleado; Pure Fabrication que llama Usuario (el email lo valida Email).
public final class ValidadorUsuario {

    // El VARCHAR(100) de la tabla: pasado, MySQL rechaza el INSERT con un 500.
    private static final int LARGO_MAXIMO = 100;

    private ValidadorUsuario() {
    }

    // Se guarda y se mide sin los espacios de las puntas: con ellos, «Ana » se mostraría distinto.
    public static String nombre(String nombre) {
        return Regla.texto(nombre).obligatorio("Falta el nombre").recortado()
                .hasta(LARGO_MAXIMO, "El nombre").valor();
    }
}
