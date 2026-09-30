package ar.uade.cine.swing.api;

import ar.uade.cine.swing.api.dto.informes.Arqueo;
import ar.uade.cine.swing.api.dto.informes.ArqueoCandy;
import ar.uade.cine.swing.api.dto.informes.Bordero;
import ar.uade.cine.swing.api.dto.informes.DeclaracionJurada;
import ar.uade.cine.swing.api.dto.informes.InformeFuncion;
import lombok.RequiredArgsConstructor;

import java.util.LinkedHashMap;
import java.util.Map;

// Los números de caja e INCAA (API.md, «Arqueo e informes»); los archivos se escriben en la PC, no acá.
@RequiredArgsConstructor
public final class ApiInformes {

    private final ClienteHttp http;

    public Bordero obtenerBordero(int funcionId) {
        return http.get("/funciones/" + funcionId + "/bordero", Bordero.class);
    }

    public InformeFuncion obtenerInformeDeFuncion(int funcionId) {
        return http.get("/funciones/" + funcionId + "/informe", InformeFuncion.class);
    }

    public Arqueo obtenerArqueo(String fecha) {
        return http.get("/arqueo" + Parametros.consulta(Map.of("fecha", Parametros.oVacio(fecha))), Arqueo.class);
    }

    public ArqueoCandy obtenerArqueoCandy(String fecha) {
        return http.get("/candy/arqueo" + Parametros.consulta(Map.of("fecha", Parametros.oVacio(fecha))),
                ArqueoCandy.class);
    }

    /** Sin fechas, el backend devuelve la última semana cinematográfica cerrada. */
    public DeclaracionJurada obtenerDeclaracionJurada(String desde, String hasta) {
        Map<String, String> filtros = new LinkedHashMap<>();
        filtros.put("desde", desde);
        filtros.put("hasta", hasta);
        return http.get("/declaracion-jurada" + Parametros.consulta(filtros), DeclaracionJurada.class);
    }
}
