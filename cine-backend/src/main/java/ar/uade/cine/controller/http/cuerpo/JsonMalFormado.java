package ar.uade.cine.controller.http.cuerpo;

import java.util.Optional;

import org.springframework.http.converter.HttpMessageNotReadableException;

// Último eslabón: lo que ninguno reconoció es texto que no se pudo leer como JSON. Siempre contesta.
public class JsonMalFormado implements LecturaDelCuerpo {

    @Override
    public Optional<String> mensaje(HttpMessageNotReadableException e) {
        return Optional.of("El cuerpo del pedido no es un JSON válido");
    }
}
