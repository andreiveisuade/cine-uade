package ar.uade.cine.swing.api;

import ar.uade.cine.swing.api.dto.catalogos.Clasificacion;
import ar.uade.cine.swing.api.dto.catalogos.MedioPago;
import ar.uade.cine.swing.api.dto.catalogos.Tarifa;
import ar.uade.cine.swing.api.dto.catalogos.TipoProducto;
import ar.uade.cine.swing.api.dto.catalogos.TipoPromocion;
import ar.uade.cine.swing.api.dto.catalogos.TipoSala;
import lombok.RequiredArgsConstructor;

import java.util.List;

// Los catálogos de enums con sus datos (API.md, «Catálogos»); los combos salen de acá, no de una lista fija.
@RequiredArgsConstructor
public final class ApiCatalogos {

    private final ClienteHttp http;

    public List<String> obtenerGeneros() {
        return http.lista("/generos", String.class);
    }

    public List<Clasificacion> obtenerClasificaciones() {
        return http.lista("/clasificaciones", Clasificacion.class);
    }

    public List<TipoSala> obtenerTiposSala() {
        return http.lista("/tipos-sala", TipoSala.class);
    }

    public List<Tarifa> obtenerTarifas() {
        return http.lista("/tarifas", Tarifa.class);
    }

    public List<MedioPago> obtenerMediosPago() {
        return http.lista("/medios-pago", MedioPago.class);
    }

    public List<TipoProducto> obtenerTiposProducto() {
        return http.lista("/tipos-producto", TipoProducto.class);
    }

    public List<TipoPromocion> obtenerTiposPromocion() {
        return http.lista("/tipos-promocion", TipoPromocion.class);
    }

    public List<String> obtenerIdiomas() {
        return http.lista("/idiomas", String.class);
    }

    public List<String> obtenerProyecciones() {
        return http.lista("/proyecciones", String.class);
    }
}
