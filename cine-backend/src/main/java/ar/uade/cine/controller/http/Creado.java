package ar.uade.cine.controller.http;

import java.net.URI;

import org.springframework.http.ResponseEntity;

public final class Creado {

    private Creado() {
    }

    public static <T> ResponseEntity<T> en(String ruta, T cuerpo) {
        return ResponseEntity.created(URI.create(ruta)).body(cuerpo);
    }
}
