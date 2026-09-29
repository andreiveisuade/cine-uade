package ar.uade.cine.controller.http;

import java.net.URI;

import org.springframework.http.ResponseEntity;

// Respuesta 201 con Location para los POST que crean; utilidad estática compartida por los controllers.
// Vive acá la política de respuestas de toda la API, que cumple cada endpoint:
//
//   Operación                   Status           Cuerpo
//   Leer                        200              el *VistaDTO, o la lista (un filtro sin resultados: [])
//   Alta                        201 + Location   el recurso creado
//   Acción o cambio de estado   200              el recurso como quedó
//   Borrar                      204              vacío
//   Rechazo                     400 / 404 / 409  {"error"}, con el texto del rechazo tal cual
//   Inesperado                  500              {"error"} genérico; el detalle, al log
//
// Cuatro altas van sin Location porque lo creado no tiene GET por id: el checkout, la grilla, la
// importación y la venta de candy. Cada una lo explica donde se declara. Los rechazos y el 500
// los arma ManejadorErrores; ningún endpoint contesta el literal null.
public final class Creado {

    private Creado() {
    }

    public static <T> ResponseEntity<T> en(String ruta, T cuerpo) {
        return ResponseEntity.created(URI.create(ruta)).body(cuerpo);
    }
}
