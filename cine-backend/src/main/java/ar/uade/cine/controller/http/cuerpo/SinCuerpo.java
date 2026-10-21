package ar.uade.cine.controller.http.cuerpo;

import java.util.Optional;

import org.springframework.http.converter.HttpMessageNotReadableException;

// Eslabón del pedido sin cuerpo: Spring lo lee como null y lo rechaza él, sin pasar por Jackson, así que no hay causa.
public class SinCuerpo implements LecturaDelCuerpo {

    @Override
    public Optional<String> mensaje(HttpMessageNotReadableException e) {
        return e.getCause() == null ? Optional.of("Falta el cuerpo del pedido") : Optional.empty();
    }
}
