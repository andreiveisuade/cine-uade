package ar.uade.cine.swing.api;

import ar.uade.cine.swing.api.dto.programaciones.PedidoGrilla;
import ar.uade.cine.swing.api.dto.programaciones.PedidoProgramacion;
import ar.uade.cine.swing.api.dto.programaciones.Plan;
import ar.uade.cine.swing.api.dto.programaciones.Programacion;
import ar.uade.cine.swing.api.dto.programaciones.PropuestaGrilla;
import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.Map;

// Lo que genera funciones de a muchas: grillas de un rango (CU-03b) y el planificador de la semana.
@RequiredArgsConstructor
public final class ApiProgramaciones {

    private final ClienteHttp http;

    public List<Programacion> obtenerProgramaciones(Map<String, String> filtros) {
        return http.lista("/programaciones" + Parametros.consulta(filtros), Programacion.class);
    }

    public Programacion obtenerProgramacion(int id) {
        return http.get("/programaciones/" + id, Programacion.class);
    }

    public Plan previsualizarProgramacion(PedidoProgramacion programacion) {
        return http.post("/programaciones/previsualizacion", programacion, Plan.class);
    }

    public Plan crearProgramacion(PedidoProgramacion programacion) {
        return http.post("/programaciones", programacion, Plan.class);
    }

    public Programacion cambiarActivacionProgramacion(int id, boolean activa) {
        return http.patch("/programaciones/" + id, Map.of("activa", activa), Programacion.class);
    }

    public PropuestaGrilla proponerGrilla(PedidoGrilla criterios) {
        return http.post("/grilla/propuesta", criterios, PropuestaGrilla.class);
    }

    public PropuestaGrilla armarGrilla(PedidoGrilla criterios) {
        return http.post("/grilla", criterios, PropuestaGrilla.class);
    }
}
