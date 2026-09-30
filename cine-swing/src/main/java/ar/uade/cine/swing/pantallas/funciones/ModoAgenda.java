package ar.uade.cine.swing.pantallas.funciones;

import ar.uade.cine.swing.api.dto.funciones.Funcion;
import ar.uade.cine.swing.api.dto.salas.Sala;
import ar.uade.cine.swing.comun.SelectorDias;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static ar.uade.cine.swing.comun.Etiquetas.etiqueta;

// Cómo mira la Agenda, una sala la semana o todas las salas un día; cada modo arma sus columnas y su pedido.
/** En una sola columna, las funciones simultáneas de varias salas se pisarían: por eso hay dos modos y no uno. */
enum ModoAgenda {

    SEMANA("Semana (una sala)", 7, true) {
        @Override
        List<ColumnaAgenda> columnas(LocalDate desde, List<Sala> salas, Sala elegida) {
            List<ColumnaAgenda> columnas = new ArrayList<>();
            if (elegida == null) return columnas;
            for (int i = 0; i < dias(); i++) {
                LocalDate fecha = desde.plusDays(i);
                columnas.add(new ColumnaAgenda(SelectorDias.abreviatura(fecha.getDayOfWeek()),
                        String.valueOf(fecha.getDayOfMonth()),
                        f -> dia(f).equals(fecha),
                        f -> etiqueta(f.proyeccion()) + " · " + etiqueta(f.idioma()).toLowerCase()));
            }
            return columnas;
        }

        @Override
        Map<String, String> filtros(LocalDate desde, Sala elegida) {
            Map<String, String> filtros = rango(desde);
            filtros.put("salaId", String.valueOf(elegida.id()));
            return filtros;
        }

        @Override
        String donde(Sala elegida) {
            return " en " + elegida.nombre();
        }
    },

    DIA("Día (todas las salas)", 1, false) {
        @Override
        List<ColumnaAgenda> columnas(LocalDate desde, List<Sala> salas, Sala elegida) {
            List<ColumnaAgenda> columnas = new ArrayList<>();
            for (Sala s : salas) {
                columnas.add(new ColumnaAgenda(s.nombre(), etiqueta(s.tipo()),
                        f -> f.sala().id() == s.id() && dia(f).equals(desde), f -> etiqueta(f.proyeccion())));
            }
            return columnas;
        }

        @Override
        Map<String, String> filtros(LocalDate desde, Sala elegida) {
            return rango(desde);
        }

        @Override
        String donde(Sala elegida) {
            return "";
        }
    };

    private final String texto;
    private final int dias;
    private final boolean eligeSala;

    ModoAgenda(String texto, int dias, boolean eligeSala) {
        this.texto = texto;
        this.dias = dias;
        this.eligeSala = eligeSala;
    }

    String texto() {
        return texto;
    }

    /** Cuánto se ve, y cuánto avanzan las flechas. */
    int dias() {
        return dias;
    }

    /** Si hay que elegir una sala: la semana es de una, el día las muestra todas. */
    boolean eligeSala() {
        return eligeSala;
    }

    abstract List<ColumnaAgenda> columnas(LocalDate desde, List<Sala> salas, Sala elegida);

    /** Solo el rango que se ve, con los filtros de la API. */
    abstract Map<String, String> filtros(LocalDate desde, Sala elegida);

    /** Lo que sigue al "N funciones" del conteo. */
    abstract String donde(Sala elegida);

    Map<String, String> rango(LocalDate desde) {
        Map<String, String> filtros = new LinkedHashMap<>();
        filtros.put("desde", desde.toString());
        filtros.put("hasta", desde.plusDays(dias - 1).toString());
        return filtros;
    }

    private static LocalDate dia(Funcion f) {
        return LocalDateTime.parse(f.inicio()).toLocalDate();
    }
}
