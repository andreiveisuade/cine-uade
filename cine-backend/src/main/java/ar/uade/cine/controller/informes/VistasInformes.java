package ar.uade.cine.controller.informes;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import ar.uade.cine.controller.candy.VistasCandy;
import ar.uade.cine.controller.http.Fechas;
import ar.uade.cine.controller.ventas.VistasVentas;
import ar.uade.cine.dto.informes.ArqueoCandyVistaDTO;
import ar.uade.cine.dto.informes.ArqueoVistaDTO;
import ar.uade.cine.dto.informes.BorderoVistaDTO;
import ar.uade.cine.dto.informes.DeclaracionJuradaVistaDTO;
import ar.uade.cine.dto.informes.ExhibidorVistaDTO;
import ar.uade.cine.dto.informes.FuncionDeclaradaVistaDTO;
import ar.uade.cine.dto.informes.InformeFuncionVistaDTO;
import ar.uade.cine.dto.informes.PeliculaDeclaradaVistaDTO;
import ar.uade.cine.dto.informes.TotalDeclaradoVistaDTO;
import ar.uade.cine.dto.informes.TotalMedioVistaDTO;
import ar.uade.cine.dto.informes.TotalTarifaVistaDTO;
import ar.uade.cine.model.candy.CompraCandy;
import ar.uade.cine.model.dinero.Dinero;
import ar.uade.cine.model.ventas.TipoTarifa;
import ar.uade.cine.service.informes.Arqueo;
import ar.uade.cine.service.informes.Bordero;
import ar.uade.cine.service.informes.DeclaracionJurada;
import ar.uade.cine.service.informes.InformeFuncion;

// Arma los JSON de borderó, arqueos y declaración jurada; Assembler que reusa VistasVentas y VistasCandy.
// El exhibidor sale de la configuración y no de un gestor: es el encabezado del documento, no una cifra.
// El CSV que se sube al INCAA lo arma el cliente de escritorio con este JSON.
@Component
@RequiredArgsConstructor
public class VistasInformes {

    private final PropiedadesIncaa incaa;
    private final VistasVentas vistasVentas;
    private final VistasCandy vistasCandy;

    public BorderoVistaDTO bordero(Bordero bordero) {
        return new BorderoVistaDTO(bordero.funcionId(), bordero.pelicula(), bordero.sala(),
                Fechas.texto(bordero.funcion()), Fechas.texto(bordero.generadoEn()),
                bordero.espectadores(), bordero.recaudacionBruta().aPesos(),
                bordero.descuentos().aPesos(), bordero.recaudacionNeta().aPesos(),
                porTarifa(bordero));
    }

    public InformeFuncionVistaDTO informe(InformeFuncion informe) {
        return new InformeFuncionVistaDTO(bordero(informe.bordero()), informe.comprasCandy(),
                informe.candy().aPesos(), informe.total().aPesos());
    }

    public ArqueoVistaDTO arqueo(Arqueo arqueo) {
        return new ArqueoVistaDTO(arqueo.fecha().toString(), arqueo.total().aPesos(),
                arqueo.entradas(), porMedio(arqueo), vistasVentas.pagosDeArqueo(arqueo.pagos()));
    }

    public ArqueoCandyVistaDTO arqueoCandy(LocalDate dia, Dinero total, List<CompraCandy> compras) {
        return new ArqueoCandyVistaDTO(dia.toString(), total.aPesos(),
                compras.stream().map(vistasCandy::compra).toList());
    }

    public DeclaracionJuradaVistaDTO declaracionJurada(DeclaracionJurada declaracion) {
        return new DeclaracionJuradaVistaDTO(exhibidor(), declaracion.desde().toString(),
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

    private ExhibidorVistaDTO exhibidor() {
        return new ExhibidorVistaDTO(incaa.razonSocial(), incaa.cuit(), incaa.numeroExhibidor());
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
    private static Map<String, TotalTarifaVistaDTO> porTarifa(Bordero bordero) {
        Map<String, TotalTarifaVistaDTO> porTarifa = new TreeMap<>();
        bordero.porTarifa().forEach((tarifa, total) ->
                porTarifa.put(tarifa.name(), new TotalTarifaVistaDTO(total.cantidad(), total.total().aPesos())));
        return porTarifa;
    }

    private static Map<String, TotalMedioVistaDTO> porMedio(Arqueo arqueo) {
        Map<String, TotalMedioVistaDTO> porMedio = new TreeMap<>();
        arqueo.porMedio().forEach((medio, acumulado) ->
                porMedio.put(medio.name(), new TotalMedioVistaDTO(acumulado.cantidad(), acumulado.total().aPesos())));
        return porMedio;
    }

    private static Map<String, Integer> entradasPorTarifa(DeclaracionJurada.Totales totales) {
        Map<String, Integer> entradas = new TreeMap<>();
        for (Map.Entry<TipoTarifa, Integer> tarifa : totales.entradasPorTarifa().entrySet()) {
            entradas.put(tarifa.getKey().name(), tarifa.getValue());
        }
        return entradas;
    }
}
