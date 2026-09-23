package ar.uade.cine.controller.vistas;

import java.util.Map;
import java.util.TreeMap;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import ar.uade.cine.controller.http.Fechas;
import ar.uade.cine.dto.ventas.BorderoVistaDTO;
import ar.uade.cine.dto.ventas.DeclaracionJuradaVistaDTO;
import ar.uade.cine.dto.ventas.ExhibidorVistaDTO;
import ar.uade.cine.dto.ventas.FuncionDeclaradaVistaDTO;
import ar.uade.cine.dto.ventas.PeliculaDeclaradaVistaDTO;
import ar.uade.cine.dto.ventas.TotalDeclaradoVistaDTO;
import ar.uade.cine.dto.ventas.TotalTarifaDTO;
import ar.uade.cine.model.ventas.TipoTarifa;
import ar.uade.cine.service.informes.Bordero;
import ar.uade.cine.service.informes.DeclaracionJurada;

// El exhibidor sale de la configuración y no de un gestor: es el encabezado del documento, no una cifra.
// El CSV que se sube al INCAA lo arma el cliente de escritorio con este JSON.
@Component
public class VistasInformes {

    private final ExhibidorVistaDTO exhibidor;

    public VistasInformes(@Value("${cine.incaa.razon-social}") String razonSocial,
                          @Value("${cine.incaa.cuit}") String cuit,
                          @Value("${cine.incaa.numero-exhibidor}") String numeroExhibidor) {
        this.exhibidor = new ExhibidorVistaDTO(razonSocial, cuit, numeroExhibidor);
    }

    public BorderoVistaDTO bordero(Bordero bordero) {
        return new BorderoVistaDTO(bordero.funcionId(), bordero.pelicula(), bordero.sala(),
                Fechas.texto(bordero.funcion()), Fechas.texto(bordero.generadoEn()),
                bordero.espectadores(), bordero.recaudacionBruta().aPesos(),
                bordero.descuentos().aPesos(), bordero.recaudacionNeta().aPesos(),
                porTarifa(bordero));
    }

    public DeclaracionJuradaVistaDTO declaracionJurada(DeclaracionJurada declaracion) {
        return new DeclaracionJuradaVistaDTO(exhibidor, declaracion.desde().toString(),
                declaracion.hasta().toString(), Fechas.texto(declaracion.generadaEn()),
                declaracion.funciones().stream().map(VistasInformes::funcion).toList(),
                declaracion.peliculas().stream().map(pelicula -> new PeliculaDeclaradaVistaDTO(
                        pelicula.titulo(), pelicula.clasificacion().name(),
                        pelicula.totales().funciones(), pelicula.totales().espectadores(),
                        entradasPorTarifa(pelicula.totales()),
                        pelicula.totales().recaudacionBruta().aPesos(),
                        pelicula.totales().descuentos().aPesos(),
                        pelicula.totales().recaudacionNeta().aPesos())).toList(),
                total(declaracion.total()));
    }

    private static FuncionDeclaradaVistaDTO funcion(DeclaracionJurada.FilaFuncion fila) {
        Bordero bordero = fila.bordero();
        return new FuncionDeclaradaVistaDTO(bordero.funcionId(), Fechas.texto(bordero.funcion()),
                bordero.sala(), bordero.pelicula(), fila.clasificacion().name(), fila.version().name(),
                fila.proyeccion().name(), bordero.espectadores(), porTarifa(bordero),
                bordero.recaudacionBruta().aPesos(), bordero.descuentos().aPesos(),
                bordero.recaudacionNeta().aPesos());
    }

    private static TotalDeclaradoVistaDTO total(DeclaracionJurada.Totales totales) {
        return new TotalDeclaradoVistaDTO(totales.funciones(), totales.espectadores(),
                entradasPorTarifa(totales), totales.recaudacionBruta().aPesos(),
                totales.descuentos().aPesos(), totales.recaudacionNeta().aPesos());
    }

    // TreeMap: el front lista las tarifas en el orden en que llegan.
    private static Map<String, TotalTarifaDTO> porTarifa(Bordero bordero) {
        Map<String, TotalTarifaDTO> porTarifa = new TreeMap<>();
        bordero.porTarifa().forEach((tarifa, total) ->
                porTarifa.put(tarifa.name(), new TotalTarifaDTO(total.cantidad(), total.total().aPesos())));
        return porTarifa;
    }

    private static Map<String, Integer> entradasPorTarifa(DeclaracionJurada.Totales totales) {
        Map<String, Integer> entradas = new TreeMap<>();
        for (Map.Entry<TipoTarifa, Integer> tarifa : totales.entradasPorTarifa().entrySet()) {
            entradas.put(tarifa.getKey().name(), tarifa.getValue());
        }
        return entradas;
    }
}
