package ar.uade.cine.service.informes;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import ar.uade.cine.model.cartelera.Clasificacion;
import ar.uade.cine.model.dinero.Dinero;
import ar.uade.cine.model.funciones.Proyeccion;
import ar.uade.cine.model.funciones.Version;
import ar.uade.cine.model.ventas.TipoTarifa;

// Cada fila es el borderó de la función: el período no tiene reglas propias, suma las de cada una.
public record DeclaracionJurada(LocalDate desde, LocalDate hasta, LocalDateTime generadaEn,
                                List<FilaFuncion> funciones, List<TotalPelicula> peliculas,
                                Totales total) {

    public record FilaFuncion(Bordero bordero, Version version, Proyeccion proyeccion,
                              Clasificacion clasificacion) {
    }

    public record TotalPelicula(String titulo, Clasificacion clasificacion, Totales totales) {
    }

    public record Totales(int funciones, int espectadores, Map<TipoTarifa, Integer> entradasPorTarifa,
                          Dinero recaudacionBruta, Dinero descuentos, Dinero recaudacionNeta) {

        public static final Totales CERO = new Totales(0, 0, Map.of(),
                Dinero.CERO, Dinero.CERO, Dinero.CERO);

        public int entradas(TipoTarifa tarifa) {
            return entradasPorTarifa.getOrDefault(tarifa, 0);
        }

        Totales mas(Bordero bordero) {
            Map<TipoTarifa, Integer> porTarifa = new EnumMap<>(TipoTarifa.class);
            porTarifa.putAll(entradasPorTarifa);
            bordero.porTarifa().forEach((tarifa, total) -> porTarifa.merge(tarifa, total.cantidad(), Integer::sum));
            return new Totales(funciones + 1, espectadores + bordero.espectadores(), porTarifa,
                    recaudacionBruta.mas(bordero.recaudacionBruta()),
                    descuentos.mas(bordero.descuentos()),
                    recaudacionNeta.mas(bordero.recaudacionNeta()));
        }
    }
}
