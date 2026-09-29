package ar.uade.cine.model.validacion;

import ar.uade.cine.model.rechazos.DatoInvalido;

// Guardas de un texto: obligatorio, recortado y con largo máximo; Fluent Interface que arranca en Regla.
public final class ReglaDeTexto {

    private final String valor;

    ReglaDeTexto(String valor) {
        this.valor = valor;
    }

    // En blanco también falta: un campo con espacios no le dice nada a nadie.
    public ReglaDeTexto obligatorio(String mensaje) {
        if (valor == null || valor.isBlank()) {
            throw new DatoInvalido(mensaje);
        }
        return this;
    }

    // Sin los espacios de las puntas: con ellos, " Matrix" pasaría por otro título al buscar repetidos.
    public ReglaDeTexto recortado() {
        return valor == null ? this : new ReglaDeTexto(valor.strip());
    }

    // El largo de la columna de schema.sql: pasado, MySQL rechaza el INSERT y el usuario vería un 500.
    // sujeto es qué es, con artículo y mayúscula («El título»): arranca el mensaje.
    public ReglaDeTexto hasta(int maximo, String sujeto) {
        if (valor != null && valor.length() > maximo) {
            throw new DatoInvalido(sujeto + " no puede tener más de " + maximo + " caracteres");
        }
        return this;
    }

    public String valor() {
        return valor;
    }
}
