package ar.uade.cine.swing.api;

import ar.uade.cine.swing.api.dto.promociones.PedidoPromocion;
import ar.uade.cine.swing.api.dto.promociones.Promocion;
import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.Map;

// Promociones (CU-17): alta, baja y reactivación. No hay borrado: explican por qué un cobro salió ese monto.
@RequiredArgsConstructor
public final class ApiPromociones {

    private final ClienteHttp http;

    public List<Promocion> obtenerPromociones() {
        return http.lista("/promociones", Promocion.class);
    }

    public Promocion crearPromocion(PedidoPromocion promocion) {
        return http.post("/promociones", promocion, Promocion.class);
    }

    public Promocion cambiarActivacionPromocion(int id, boolean activa) {
        return http.patch("/promociones/" + id, Map.of("activa", activa), Promocion.class);
    }
}
