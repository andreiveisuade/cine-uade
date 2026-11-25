package ar.uade.cine.controller.http;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

// Qué aplicación del cine llama a cada operación; es documentación para Swagger, no un permiso.
enum ClienteDeLaApi {
    WEB("Web",
            "GET /api/generos", "GET /api/tipos-pelicula",
            "GET /api/cartelera", "GET /api/peliculas/{id}", "GET /api/peliculas/{id}/funciones",
            "GET /api/funciones/{id}", "POST /api/funciones/{id}/bloqueos",
            "POST /api/reservas", "GET /api/reservas/mias", "GET /api/reservas/codigo/{codigo}",
            "POST /api/reservas/codigo/{codigo}/cancelacion", "PUT /api/reservas/codigo/{codigo}/candy",
            "GET /api/reservas/codigo/{codigo}/cotizacion", "POST /api/reservas/codigo/{codigo}/checkout",
            "GET /api/checkouts/{id}/retorno",
            "GET /api/candy/productos", "GET /api/candy/compras/mias",
            "GET /api/promociones/vigentes", "GET /api/imagenes/{nombre}",
            "POST /api/sesion", "GET /api/sesion/google", "PUT /api/cuenta", "DELETE /api/cuenta"),
    SWING("Swing",
            "GET /api/generos", "GET /api/clasificaciones", "GET /api/tipos-sala", "GET /api/idiomas",
            "GET /api/proyecciones", "GET /api/medios-pago", "GET /api/tarifas",
            "GET /api/tipos-producto", "GET /api/tipos-promocion",
            "GET /api/peliculas", "POST /api/peliculas", "PUT /api/peliculas/{id}", "DELETE /api/peliculas/{id}",
            "GET /api/peliculas/pendientes", "POST /api/peliculas/{id}/confirmacion",
            "POST /api/peliculas/{id}/descarte",
            "GET /api/importaciones", "POST /api/importaciones", "GET /api/importaciones/estado",
            "GET /api/salas", "GET /api/salas/{id}", "POST /api/salas", "DELETE /api/salas/{id}",
            "PATCH /api/salas/{salaId}/asientos/{codigo}",
            "GET /api/funciones", "GET /api/funciones/{id}", "POST /api/funciones", "PUT /api/funciones/{id}",
            "POST /api/funciones/borrado",
            "GET /api/programaciones", "GET /api/programaciones/{id}", "POST /api/programaciones",
            "POST /api/programaciones/previsualizacion", "PATCH /api/programaciones/{id}",
            "POST /api/grilla", "POST /api/grilla/propuesta",
            "GET /api/promociones", "POST /api/promociones", "PUT /api/promociones/{id}",
            "PATCH /api/promociones/{id}",
            "GET /api/candy/productos", "POST /api/candy/productos", "PUT /api/candy/productos/{id}",
            "PATCH /api/candy/productos/{id}", "POST /api/candy/combos",
            "GET /api/candy/compras", "POST /api/candy/compras", "POST /api/candy/retiros",
            "GET /api/reservas", "GET /api/reservas/{id}", "POST /api/reservas/{id}/cancelacion",
            "POST /api/reservas/{id}/pago", "POST /api/reservas/{id}/checkout",
            "POST /api/checkouts/{id}/confirmacion", "POST /api/acceso",
            "GET /api/clientes", "GET /api/clientes/{id}/transacciones",
            "GET /api/arqueo", "GET /api/candy/arqueo", "GET /api/funciones/{id}/bordero",
            "GET /api/funciones/{id}/informe", "GET /api/declaracion-jurada",
            "POST /api/imagenes", "GET /api/imagenes/{nombre}", "POST /api/sesion"),
    ANDROID("Android", "POST /api/sesion", "POST /api/acceso", "POST /api/candy/retiros");

    private static final String SIN_USO = "Sin uso";

    private final String nombre;
    private final Set<String> operaciones;

    ClienteDeLaApi(String nombre, String... operaciones) {
        this.nombre = nombre;
        this.operaciones = Set.of(operaciones);
    }

    static String etiquetaDe(String operacion) {
        String clientes = Arrays.stream(values())
                .filter(cliente -> cliente.operaciones.contains(operacion))
                .map(cliente -> cliente.nombre)
                .collect(Collectors.joining(" + "));
        return "[" + (clientes.isEmpty() ? SIN_USO : clientes) + "]";
    }

    Set<String> operaciones() {
        return operaciones;
    }
}
