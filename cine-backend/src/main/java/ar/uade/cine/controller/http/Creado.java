package ar.uade.cine.controller.http;

import java.net.URI;

import org.springframework.http.ResponseEntity;

// Respuesta 201 con Location para los POST que crean; utilidad estática compartida por los controllers.
public final class Creado {

    private Creado() {
    }

    public static <T> ResponseEntity<T> en(String ruta, T cuerpo) {
        return ResponseEntity.created(URI.create(ruta)).body(cuerpo);
    }
}
