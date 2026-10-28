package ar.uade.cine.swing.pantallas.funciones;

import ar.uade.cine.swing.api.ApiFunciones;
import ar.uade.cine.swing.api.ApiSalas;
import ar.uade.cine.swing.api.ClienteHttp;
import ar.uade.cine.swing.api.ServidorFalso;
import ar.uade.cine.swing.pantallas.Destino;
import ar.uade.cine.swing.pantallas.Navegacion;
import ar.uade.cine.swing.pantallas.Operador;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.time.LocalDate;

import static ar.uade.cine.swing.pantallas.funciones.PantallaFuncionesTest.SALA;
import static ar.uade.cine.swing.pantallas.funciones.PantallaFuncionesTest.funcion;
import static org.junit.jupiter.api.Assertions.assertFalse;

// La agenda pide solo el rango que se ve: la semana de una sala o un día de todas, y las flechas lo corren.
class PantallaAgendaTest {

    private static final LocalDate HOY = LocalDate.now();

    private ServidorFalso servidor;
    private Operador encargado;

    @BeforeEach
    void levantar() throws IOException {
        servidor = new ServidorFalso();
        servidor.responder("GET /api/salas", 200, "[" + SALA + "]");
        servidor.responder("GET /api/funciones?desde=" + HOY + "&hasta=" + HOY.plusDays(6) + "&salaId=1", 200,
                "[" + funcion(1, HOY + "T20:30:00") + "," + funcion(2, HOY.plusDays(2) + "T18:00:00") + "]");
        ClienteHttp http = new ClienteHttp(servidor.url());
        encargado = Operador.en(() -> new PantallaAgenda(new ApiFunciones(http), new ApiSalas(http), new Quieta()));
    }

    @AfterEach
    void bajar() {
        servidor.bajar();
        encargado.cerrar();
    }

    @Test
    void arrancaConLaSemanaDeLaPrimeraSala() {
        encargado.esperarTexto("2 funciones en Sala 1");
    }

    @Test
    void elDiaPideTodasLasSalasSinFiltrarSala() {
        servidor.responder("GET /api/funciones?desde=" + HOY + "&hasta=" + HOY, 200,
                "[" + funcion(1, HOY + "T20:30:00") + "]");
        encargado.esperarTexto("2 funciones en Sala 1");

        encargado.elegir("Ver", "Día (todas las salas)");

        encargado.esperarTexto("1 funciones");
        assertFalse(encargado.textos().contains("Sala"), "en el día no se elige sala");
    }

    @Test
    void laFlechaAvanzaUnaSemanaYSinFuncionesLoDice() {
        LocalDate siguiente = HOY.plusDays(7);
        servidor.responder("GET /api/funciones?desde=" + siguiente + "&hasta=" + siguiente.plusDays(6) + "&salaId=1",
                200, "[]");
        encargado.esperarTexto("2 funciones en Sala 1");

        encargado.clic("→");

        encargado.esperarTexto("No hay funciones programadas en este rango.");
        encargado.esperarTexto("0 funciones en Sala 1");
    }

    private static final class Quieta implements Navegacion {

        @Override
        public void ir(Destino destino) {
        }

        @Override
        public void abrirInforme(int funcionId) {
        }

        @Override
        public void abrirCobro(int reservaId) {
        }

        @Override
        public void abrirMapa(int salaId) {
        }
    }
}
