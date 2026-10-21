package ar.uade.cine.model.validacion;

import ar.uade.cine.model.rechazos.DatoInvalido;

// Guardas de un texto: obligatorio, recortado y con largo máximo; Fluent Interface que arranca en Regla.
public final class ReglaDeTexto {

    private final String valor;

    ReglaDeTexto(String valor) {
        this.valor = valor;
    }

    public ReglaDeTexto obligatorio(String mensaje) {
        if (valor == null || valor.isBlank()) {
            throw new DatoInvalido(mensaje);
        }
        return this;
    }

    // Sin espacios de las puntas, " Matrix" pasaría por otro título al buscar repetidos.
    public ReglaDeTexto recortado() {
        return valor == null ? this : new ReglaDeTexto(valor.strip());
    }

    // Largo de la columna de schema.sql: pasado, MySQL rechaza el INSERT con un 500.
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
