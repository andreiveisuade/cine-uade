package ar.uade.cine.swing.pantallas.ventas;

import ar.uade.cine.swing.pantallas.Destino;
import ar.uade.cine.swing.pantallas.Navegacion;

import java.util.ArrayList;
import java.util.List;

// Lo que comparten los tests de ventas: reservas de ejemplo y una navegación que anota adónde se fue.
final class Ventas {

    static final String FUNCION = "{\"id\":1,\"peliculaId\":7,\"salaId\":1,\"inicio\":\"2026-11-06T20:30:00\","
            + "\"idioma\":\"DOBLADA\",\"proyeccion\":\"DOS_D\",\"precio\":5000.0}";
    static final String PELICULA = "{\"id\":7,\"titulo\":\"Matrix\",\"duracionMinutos\":136,\"generos\":[],"
            + "\"clasificacion\":\"ATP\",\"enCartelera\":true}";
    static final String SALA = "{\"id\":1,\"nombre\":\"Sala 1\",\"tipo\":\"DOS_D\",\"butacasPorFila\":[10],"
            + "\"filas\":1,\"capacidadSala\":10,\"minutosLimpieza\":15}";
    static final String TARIFAS = "[{\"nombre\":\"GENERAL\",\"multiplicador\":1.0,\"requiereAcreditacion\":false},"
            + "{\"nombre\":\"JUBILADO\",\"multiplicador\":0.5,\"requiereAcreditacion\":true}]";
    static final String MEDIOS = "[{\"nombre\":\"EFECTIVO\",\"requiereAutorizacion\":false},"
            + "{\"nombre\":\"CREDITO\",\"requiereAutorizacion\":true}]";

    private Ventas() {
    }

    static String reserva(int id, String estado, boolean cobrable, String pago) {
        return "{\"id\":" + id + ",\"estado\":\"" + estado + "\",\"codigo\":\"ABC" + id + "\","
                + "\"ingresadaEn\":\"2026-11-06T20:10:00\","
                + "\"entradas\":[{\"codigo\":\"A1\",\"tarifa\":\"GENERAL\",\"precio\":5000.0},"
                + "{\"codigo\":\"A2\",\"tarifa\":\"JUBILADO\",\"precio\":2500.0}],\"total\":7500.0,"
                + "\"funcion\":" + FUNCION + ",\"pelicula\":" + PELICULA + ",\"sala\":" + SALA + ","
                + "\"cliente\":{\"id\":3,\"nombre\":\"Ana Prueba\",\"email\":\"ana@mail.com\"},"
                + "\"pago\":" + pago + ",\"cobrable\":" + cobrable + ",\"cancelable\":" + cobrable + "}";
    }

    static final class Anotadora implements Navegacion {

        final List<String> visitas = new ArrayList<>();

        @Override
        public void ir(Destino destino) {
            visitas.add(destino.name());
        }

        @Override
        public void abrirInforme(int funcionId) {
            visitas.add("informe " + funcionId);
        }

        @Override
        public void abrirCobro(int reservaId) {
            visitas.add("cobro " + reservaId);
        }

        @Override
        public void abrirMapa(int salaId) {
            visitas.add("mapa " + salaId);
        }
    }
}
