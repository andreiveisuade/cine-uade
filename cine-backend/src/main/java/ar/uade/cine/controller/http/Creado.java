package ar.uade.cine.controller.http;

import java.net.URI;

import org.springframework.http.ResponseEntity;

// Respuesta 201 con Location para los POST que crean; utilidad estática compartida por los controllers.
// Política de respuestas de la API: 200 lee y acciona, 201 + Location alta, 204 borra, {"error"} en 400/404/409/500.
// Sin Location: checkout, grilla, importación y venta de candy (lo creado no tiene GET por id).
public final class Creado {

    private Creado() {
    }

    public static <T> ResponseEntity<T> en(String ruta, T cuerpo) {
        return ResponseEntity.created(URI.create(ruta)).body(cuerpo);
    }
}
