package ar.uade.cine.swing.api;

import ar.uade.cine.swing.api.dto.funciones.Funcion;
import ar.uade.cine.swing.api.dto.funciones.PedidoFuncion;
import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.Map;

// Funciones sueltas: listado con filtros, detalle, alta y baja, como las expone FuncionController.
@RequiredArgsConstructor
public final class ApiFunciones {

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

    public void eliminarFuncion(int id) {
        http.delete("/funciones/" + id);
    }
}
