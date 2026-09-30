package ar.uade.cine.swing.api;

import ar.uade.cine.swing.api.dto.salas.Asiento;
import ar.uade.cine.swing.api.dto.salas.PedidoSala;
import ar.uade.cine.swing.api.dto.salas.Sala;
import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.Map;

// Salas y el estado físico de sus butacas (R9), como las expone SalaController.
@RequiredArgsConstructor
public final class ApiSalas {

    private final ClienteHttp http;

    public List<Sala> obtenerSalas() {
        return http.lista("/salas", Sala.class);
    }

    public Sala obtenerSala(int id) {
        return http.get("/salas/" + id, Sala.class);
    }

    public Sala crearSala(PedidoSala sala) {
        return http.post("/salas", sala, Sala.class);
    }

    public void eliminarSala(int id) {
        http.delete("/salas/" + id);
    }

    public Asiento cambiarEstadoAsiento(int salaId, String codigo, String estado) {
        return http.patch("/salas/" + salaId + "/asientos/" + Parametros.segmento(codigo.trim().toUpperCase()),
                Map.of("estado", Parametros.oVacio(estado)), Asiento.class);
    }
}
