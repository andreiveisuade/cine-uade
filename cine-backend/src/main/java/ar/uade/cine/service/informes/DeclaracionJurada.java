package ar.uade.cine.service.informes;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import ar.uade.cine.model.cartelera.Clasificacion;
import ar.uade.cine.model.dinero.Dinero;
import ar.uade.cine.model.funciones.Proyeccion;
import ar.uade.cine.model.funciones.Version;
import ar.uade.cine.model.ventas.TipoTarifa;

// Declaración jurada de espectadores para el INCAA; Experto en sumar los borderós por película y en total.
// Cada fila es el borderó de la función: el período no tiene reglas propias, suma las de cada una.
public record DeclaracionJurada(LocalDate desde, LocalDate hasta, LocalDateTime generadaEn,
                                List<FilaFuncion> funciones, List<TotalPelicula> peliculas,
                                Totales total) {

    // Suma las filas por película y en total. El gestor solo arma las filas.
    public static DeclaracionJurada de(PeriodoDeclarado periodo, LocalDateTime generadaEn,
                                       List<FilaFuncion> filas) {
        Map<Integer, TotalPelicula> porPelicula = new LinkedHashMap<>();
        Totales total = Totales.CERO;
        for (FilaFuncion fila : filas) {
            TotalPelicula acumulado = porPelicula.getOrDefault(fila.peliculaId(),
                    new TotalPelicula(fila.bordero().pelicula(), fila.clasificacion(), Totales.CERO));
            porPelicula.put(fila.peliculaId(), new TotalPelicula(acumulado.titulo(),
                    acumulado.clasificacion(), acumulado.totales().mas(fila.bordero())));
            total = total.mas(fila.bordero());
        }
        List<TotalPelicula> peliculas = porPelicula.values().stream()
                .sorted(Comparator.comparing(TotalPelicula::titulo))
                .toList();
        return new DeclaracionJurada(periodo.desde(), periodo.hasta(), generadaEn, filas, peliculas, total);
    }

    public record FilaFuncion(int peliculaId, Bordero bordero, Version version, Proyeccion proyeccion,
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
