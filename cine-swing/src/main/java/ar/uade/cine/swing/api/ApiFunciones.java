package ar.uade.cine.swing.api;

import ar.uade.cine.swing.api.dto.funciones.Borrado;
import ar.uade.cine.swing.api.dto.funciones.Funcion;
import ar.uade.cine.swing.api.dto.funciones.PedidoBorrado;
import ar.uade.cine.swing.api.dto.funciones.PedidoFuncion;
import lombok.RequiredArgsConstructor;

import java.time.Duration;
import java.util.List;
import java.util.Map;

// Funciones sueltas: listado con filtros, detalle, alta, edición y baja, también en lote; como FuncionController.
@RequiredArgsConstructor
public final class ApiFunciones {

    // Cientos de borrados contra Render y Aiven pueden pasar la espera normal.
    private static final Duration ESPERA_BORRADO = Duration.ofSeconds(120);

    private final ClienteHttp http;

    public List<Funcion> obtenerFunciones(Map<String, String> filtros) {
        return http.lista("/funciones" + Parametros.consulta(filtros), Funcion.class);
    }

    public Funcion obtenerFuncion(int id) {
        return http.get("/funciones/" + id, Funcion.class);
    }

    public Funcion programarFuncion(PedidoFuncion funcion) {
        return http.post("/funciones", funcion, Funcion.class);
    }

    public Funcion editarFuncion(int id, PedidoFuncion funcion) {
        return http.put("/funciones/" + id, funcion, Funcion.class);
    }

    public void eliminarFuncion(int id) {
        http.delete("/funciones/" + id);
    }

    public Borrado borrarFunciones(List<Integer> ids) {
        return http.post("/funciones/borrado", new PedidoBorrado(ids), Borrado.class, ESPERA_BORRADO);
    }
}
