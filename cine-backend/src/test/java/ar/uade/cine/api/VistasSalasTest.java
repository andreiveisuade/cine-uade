package ar.uade.cine.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ar.uade.cine.api.VistasSalas.AsientoVista;
import ar.uade.cine.api.VistasSalas.SalaVista;
import ar.uade.cine.dominio.cartelera.Clasificacion;
import ar.uade.cine.dominio.cartelera.Genero;
import ar.uade.cine.dominio.funciones.Funcion;
import ar.uade.cine.dominio.funciones.Proyeccion;
import ar.uade.cine.dominio.funciones.Version;
import ar.uade.cine.dominio.salas.Asiento;
import ar.uade.cine.dominio.salas.Sala;
import ar.uade.cine.dominio.salas.TipoAsiento;
import ar.uade.cine.dominio.salas.TipoSala;
import ar.uade.cine.dominio.ventas.TipoTarifa;
import ar.uade.cine.persistencia.memoria.AsientoDAOMemoria;
import ar.uade.cine.persistencia.memoria.FuncionDAOMemoria;
import ar.uade.cine.persistencia.memoria.PeliculaDAOMemoria;
import ar.uade.cine.persistencia.memoria.ProgramacionDAOMemoria;
import ar.uade.cine.persistencia.memoria.ReservaDAOMemoria;
import ar.uade.cine.persistencia.memoria.SalaDAOMemoria;
import ar.uade.cine.persistencia.AsientoDAO;
import ar.uade.cine.persistencia.FuncionDAO;
import ar.uade.cine.persistencia.PeliculaDAO;
import ar.uade.cine.persistencia.SalaDAO;
import ar.uade.cine.servicio.CalculadoraPrecio;
import ar.uade.cine.servicio.GestorCartelera;
import ar.uade.cine.servicio.GestorFunciones;
import ar.uade.cine.servicio.GestorProgramaciones;
import ar.uade.cine.servicio.GestorSalas;

/**
 * El JSON de las salas es un contrato con el front: la grilla del ABM y el mapa de
 * butacas se dibujan con estos campos. Un cambio de forma acá no rompe ninguna regla de
 * negocio —por eso ningún test de servicio lo atraparía— pero deja una pantalla en blanco.
 */
class VistasSalasTest {

    private GestorSalas salas;
    private GestorFunciones funciones;
    private CalculadoraPrecio calculadora;
    private VistasSalas vistas;

    @BeforeEach
    void prepararEscenario() {
        PeliculaDAO peliculaDAO = new PeliculaDAOMemoria();
        SalaDAO salaDAO = new SalaDAOMemoria();
        AsientoDAO asientoDAO = new AsientoDAOMemoria();
        FuncionDAO funcionDAO = new FuncionDAOMemoria();

        salas = new GestorSalas(salaDAO, asientoDAO, funcionDAO);
        funciones = new GestorFunciones(funcionDAO, peliculaDAO, salaDAO, new ReservaDAOMemoria());
        calculadora = new CalculadoraPrecio();
        vistas = new VistasSalas(salas, calculadora);

        new GestorCartelera(peliculaDAO, funcionDAO, new GestorProgramaciones(
                new ProgramacionDAOMemoria(), funcionDAO, funciones))
                .agregar("Matrix", 136, List.of(Genero.ACCION), Clasificacion.ATP);
    }

    /**
     * La distribución no se guarda: se cuenta. Es la misma decisión que la capacidad, y
     * lo que la sostiene es que marcar una butaca fuera de servicio no tiene que obligar
     * a corregir un número guardado en otro lado.
     */
    @Test
    void laDistribucionSeDerivaDeLasButacasQueExisten() {
        SalaVista vista = vistas.sala(salas.agregar("Sala 1", TipoSala.DOS_D, List.of(3, 5, 2)));

        assertEquals(List.of(3, 5, 2), vista.butacasPorFila());
        assertEquals(3, vista.filas());
        assertEquals(10, vista.capacidadSala());
    }

    /** Embebida en una función, la sala va sin butacas: son cientos y no se muestran ahí. */
    @Test
    void laSalaEmbebidaNoArrastraElDetalleDeButacas() {
        assertNull(vistas.sala(salas.agregar("Sala 1", TipoSala.DOS_D, List.of(4))).asientos());
    }

    @Test
    void elAbmDeSalasSiTraeCadaButacaConSuTipo() {
        Sala sala = salas.agregar("Sala 1", TipoSala.DOS_D, List.of(2, 2),
                Map.of("A1", TipoAsiento.VIP));

        SalaVista vista = vistas.salaConButacas(sala);

        assertEquals(4, vista.asientos().size());
        assertEquals("VIP", butaca(vista, "A1").tipo());
        assertEquals("ESTANDAR", butaca(vista, "A2").tipo());
        assertEquals("HABILITADO", butaca(vista, "A1").estado());
    }

    /**
     * Sin función de por medio no hay respuesta para "¿está ocupada?" ni para "¿cuánto
     * sale?": la butaca está tomada en una función y libre en otra. Viajan en null y el
     * front no las muestra, que es distinto de mandar false y cero.
     */
    @Test
    void fueraDeUnaFuncionNoSeSabeSiEstaTomadaNiCuantoSale() {
        Sala sala = salas.agregar("Sala 1", TipoSala.DOS_D, List.of(2));

        AsientoVista butaca = butaca(vistas.salaConButacas(sala), "A1");

        assertNull(butaca.ocupado());
        assertNull(butaca.precio());
    }

    /** Una butaca rota se sigue dibujando: el front la pinta distinto, no la esconde. */
    @Test
    void laButacaFueraDeServicioViajaConSuEstado() {
        Sala sala = salas.agregar("Sala 1", TipoSala.DOS_D, List.of(3));
        salas.marcarFueraDeServicio(sala.getId(), "A2");

        SalaVista vista = vistas.salaConButacas(sala);

        assertEquals(3, vista.asientos().size(), "la butaca rota no desaparece del mapa");
        assertEquals("FUERA_DE_SERVICIO", butaca(vista, "A2").estado());
        assertEquals("HABILITADO", butaca(vista, "A1").estado());
    }

    // ---------- la butaca dentro del mapa de una función ----------

    @Test
    void enElMapaDeUnaFuncionLaButacaDiceSiEstaTomada() {
        Sala sala = salas.agregar("Sala 1", TipoSala.DOS_D, List.of(3));
        Funcion funcion = programarEn(sala, 5000);
        Asiento a1 = asiento(sala, "A1");
        Asiento a2 = asiento(sala, "A2");

        assertTrue(vistas.asiento(a1, funcion, sala, Set.of(a1.getId())).ocupado());
        assertFalse(vistas.asiento(a2, funcion, sala, Set.of(a1.getId())).ocupado());
    }

    /**
     * El mapa muestra el precio de tarifa general. La tarifa de cada persona se elige al
     * reservar y de ahí solo puede bajar: si el mapa mostrara la más barata, el total de
     * la compra saldría más caro que lo anunciado.
     */
    @Test
    void elMapaMuestraElPrecioDeTarifaGeneral() {
        Sala sala = salas.agregar("Sala 1", TipoSala.DOS_D, List.of(3));
        Funcion funcion = programarEn(sala, 5000);
        Asiento a1 = asiento(sala, "A1");

        double mostrado = vistas.asiento(a1, funcion, sala, Set.of()).precio();

        assertEquals(calculadora.precioDe(funcion, sala, a1, TipoTarifa.GENERAL), mostrado);
        assertTrue(mostrado > calculadora.precioDe(funcion, sala, a1, TipoTarifa.JUBILADO),
                "estaría anunciando un precio que no es el que se cobra por defecto");
    }

    /** La butaca VIP sale más cara que la estándar de la misma función. */
    @Test
    void elPrecioDelMapaContemplaElTipoDeButaca() {
        Sala sala = salas.agregar("Sala 1", TipoSala.IMAX, List.of(2),
                Map.of("A1", TipoAsiento.VIP));
        Funcion funcion = programarEn(sala, 5000);

        double vip = vistas.asiento(asiento(sala, "A1"), funcion, sala, Set.of()).precio();
        double estandar = vistas.asiento(asiento(sala, "A2"), funcion, sala, Set.of()).precio();

        assertEquals(12000.0, vip, 0.001, "5000 x 1.6 (IMAX) x 1.5 (VIP)");
        assertEquals(8000.0, estandar, 0.001, "5000 x 1.6 (IMAX)");
    }

    private Funcion programarEn(Sala sala, double precio) {
        return funciones.programar(1, sala.getId(), LocalDateTime.of(2026, 8, 20, 20, 0),
                Version.SUBTITULADA, Proyeccion.DOS_D, precio);
    }

    private Asiento asiento(Sala sala, String codigo) {
        return salas.asientosDe(sala.getId()).stream()
                .filter(a -> a.getCodigo().equals(codigo))
                .findFirst()
                .orElseThrow(() -> new AssertionError("La sala no tiene la butaca " + codigo));
    }

    private static AsientoVista butaca(SalaVista vista, String codigo) {
        return vista.asientos().stream()
                .filter(a -> a.codigo().equals(codigo))
                .findFirst()
                .orElseThrow(() -> new AssertionError("La vista no tiene la butaca " + codigo));
    }
}
