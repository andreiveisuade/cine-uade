package ar.uade.cine.swing.pantallas.programaciones;

import ar.uade.cine.swing.api.dto.programaciones.IndicadoresGrilla;
import ar.uade.cine.swing.comun.Componentes;

import javax.swing.JPanel;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.util.Locale;
import java.util.function.Function;

import static ar.uade.cine.swing.comun.Formato.conDecimal;
import static ar.uade.cine.swing.comun.Formato.porcentaje;

// Las tres cifras de una propuesta de grilla, cada una comparada con la corrida anterior.
final class TarjetasIndicadores extends JPanel {

    TarjetasIndicadores(IndicadoresGrilla i, IndicadoresGrilla anterior) {
        super(new GridLayout(1, 3, 8, 0));
        add(Componentes.cifra("Ocupación de las salas", porcentaje(i.ocupacion()),
                String.format(Locale.ROOT, "%,d de %,d minutos con función", i.minutosProgramados(),
                        i.minutosDisponibles()).replace(',', '.'),
                variacion(i, anterior, IndicadoresGrilla::ocupacion, d -> porcentaje(d))));
        add(Componentes.cifra("Puntaje promedio", conDecimal(i.puntajePromedio()), "pesado por funciones",
                variacion(i, anterior, IndicadoresGrilla::puntajePromedio, d -> conDecimal(d))));
        add(Componentes.cifra("Géneros", i.generosCubiertos() + " de " + i.generosTotales(), "del catálogo",
                variacion(i, anterior, x -> (double) x.generosCubiertos(), d -> String.valueOf(Math.round(d)))));
        setMaximumSize(new Dimension(Integer.MAX_VALUE, getPreferredSize().height));
    }

    private static String variacion(IndicadoresGrilla actual, IndicadoresGrilla anterior,
                                    Function<IndicadoresGrilla, Double> valor, Function<Double, String> formato) {
        if (anterior == null) return null;
        double delta = valor.apply(actual) - valor.apply(anterior);
        if (Math.abs(delta) < 0.0001) return "igual que la anterior";
        return (delta > 0 ? "▲ " : "▼ ") + formato.apply(Math.abs(delta)) + " vs. la anterior";
    }
}
