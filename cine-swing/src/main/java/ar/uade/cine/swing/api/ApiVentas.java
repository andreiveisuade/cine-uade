package ar.uade.cine.swing.api;

import ar.uade.cine.swing.api.dto.ventas.Checkout;
import ar.uade.cine.swing.api.dto.ventas.Pago;
import ar.uade.cine.swing.api.dto.ventas.Reserva;
import lombok.RequiredArgsConstructor;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

// Reservas del lado del encargado: buscar, cancelar, cobrar en caja o por checkout, y validar en la puerta.
@RequiredArgsConstructor
public final class ApiVentas {

    private final ClienteHttp http;

    public List<Reserva> obtenerReservas(Map<String, String> filtros) {
        return http.lista("/reservas" + Parametros.consulta(filtros), Reserva.class);
    }

    public Reserva obtenerReserva(int id) {
        return http.get("/reservas/" + id, Reserva.class);
    }

    public Reserva cancelarReserva(int id) {
        return http.post("/reservas/" + id + "/cancelacion", null, Reserva.class);
    }

    /** El monto no viaja: el descuento depende del medio y lo resuelve el backend al cobrar. */
    public Pago cobrar(int reservaId, String medio, String codigoAutorizacion) {
        Map<String, String> cuerpo = new LinkedHashMap<>();
        cuerpo.put("medio", medio);
        cuerpo.put("codigoAutorizacion", codigoAutorizacion);
        return http.post("/reservas/" + reservaId + "/pago", cuerpo, Pago.class);
    }

    public Checkout abrirCheckout(int reservaId, String medio) {
        return http.post("/reservas/" + reservaId + "/checkout", Map.of("medio", Parametros.oVacio(medio)),
                Checkout.class);
    }

    public Pago confirmarCheckout(String checkoutId) {
        return http.post("/checkouts/" + Parametros.segmento(checkoutId) + "/confirmacion", null, Pago.class);
    }

    public Reserva validarEntrada(String codigo) {
        return http.post("/acceso", Map.of("codigo", Parametros.oVacio(codigo)), Reserva.class);
    }
}
