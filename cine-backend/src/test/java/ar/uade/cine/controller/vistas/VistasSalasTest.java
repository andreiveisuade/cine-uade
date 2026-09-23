package ar.uade.cine.controller.vistas;

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

import ar.uade.cine.model.cartelera.Clasificacion;
import ar.uade.cine.model.cartelera.Genero;
import ar.uade.cine.model.funciones.Funcion;
import ar.uade.cine.model.funciones.Proyeccion;
import ar.uade.cine.model.funciones.Version;
import ar.uade.cine.model.salas.Asiento;
import ar.uade.cine.model.salas.Sala;
import ar.uade.cine.model.salas.TipoAsiento;
import ar.uade.cine.model.salas.TipoSala;
import org.springframework.beans.factory.annotation.Autowired;

import ar.uade.cine.PruebaDeIntegracion;
import ar.uade.cine.model.ventas.TipoTarifa;
import ar.uade.cine.dto.salas.AsientoVistaDTO;
import ar.uade.cine.dto.salas.SalaVistaDTO;
import ar.uade.cine.service.ventas.CalculadoraPrecio;
import ar.uade.cine.service.cartelera.GestorCartelera;
import ar.uade.cine.service.funciones.GestorFunciones;
import ar.uade.cine.service.programaciones.GestorProgramaciones;
import ar.uade.cine.service.salas.GestorSalas;
import ar.uade.cine.model.dinero.Dinero;

class VistasSalasTest extends PruebaDeIntegracion {

    @Autowired
    private GestorSalas salas;
    @Autowired
    private GestorFunciones funciones;
    @Autowired
    private CalculadoraPrecio calculadora;
    @Autowired
    private VistasSalas vistas;
    @Autowired
    private GestorCartelera cartelera;

    @BeforeEach
    void prepararEscenario() {
        cartelera
                .agregar("Matrix", 136, List.of(Genero.ACCION), Clasificacion.ATP);
    }

    @Test
    void laDistribucionSeDerivaDeLasButacasQueExisten() {
        SalaVistaDTO vista = vistas.sala(salas.agregar("Sala 1", TipoSala.DOS_D, List.of(3, 5, 2)));

        assertEquals(List.of(3, 5, 2), vista.butacasPorFila());
        assertEquals(3, vista.filas());
        assertEquals(10, vista.capacidadSala());
    }

    @Test
    void laSalaEmbebidaNoArrastraElDetalleDeButacas() {
        assertNull(vistas.sala(salas.agregar("Sala 1", TipoSala.DOS_D, List.of(4))).asientos());
    }

    @Test
    void elAbmDeSalasSiTraeCadaButacaConSuTipo() {
        Sala sala = salas.agregar("Sala 1", TipoSala.DOS_D, List.of(2, 2),
                Map.of("A1", TipoAsiento.VIP));

        SalaVistaDTO vista = vistas.salaConButacas(sala);

        assertEquals(4, vista.asientos().size());
        assertEquals("VIP", butaca(vista, "A1").tipo());
        assertEquals("ESTANDAR", butaca(vista, "A2").tipo());
        assertEquals("HABILITADO", butaca(vista, "A1").estado());
    }

    @Test
    void fueraDeUnaFuncionNoSeSabeSiEstaTomadaNiCuantoSale() {
        Sala sala = salas.agregar("Sala 1", TipoSala.DOS_D, List.of(2));

        AsientoVistaDTO butaca = butaca(vistas.salaConButacas(sala), "A1");

        assertNull(butaca.ocupado());
        assertNull(butaca.precio());
    }

    @Test
    void laButacaFueraDeServicioViajaConSuEstado() {
        Sala sala = salas.agregar("Sala 1", TipoSala.DOS_D, List.of(3));
        salas.marcarFueraDeServicio(sala.getId(), "A2");

        SalaVistaDTO vista = vistas.salaConButacas(sala);

        assertEquals(3, vista.asientos().size(), "la butaca rota no desaparece del mapa");
        assertEquals("FUERA_DE_SERVICIO", butaca(vista, "A2").estado());
        assertEquals("HABILITADO", butaca(vista, "A1").estado());
    }

    @Test
    void enElMapaDeUnaFuncionLaButacaDiceSiEstaTomada() {
        Sala sala = salas.agregar("Sala 1", TipoSala.DOS_D, List.of(3));
        Funcion funcion = programarEn(sala, 5000);
        Asiento a1 = asiento(sala, "A1");
        Asiento a2 = asiento(sala, "A2");

        assertTrue(vistas.asiento(a1, funcion, sala, Set.of(a1.getId())).ocupado());
        assertFalse(vistas.asiento(a2, funcion, sala, Set.of(a1.getId())).ocupado());
    }

    @Test
    void elMapaMuestraElPrecioDeTarifaGeneral() {
        Sala sala = salas.agregar("Sala 1", TipoSala.DOS_D, List.of(3));
        Funcion funcion = programarEn(sala, 5000);
        Asiento a1 = asiento(sala, "A1");

        double mostrado = vistas.asiento(a1, funcion, sala, Set.of()).precio();

        assertEquals(calculadora.precioDe(funcion, sala, a1, TipoTarifa.GENERAL).aPesos(), mostrado);
        assertTrue(mostrado > calculadora.precioDe(funcion, sala, a1, TipoTarifa.JUBILADO).aPesos(),
                "estaría anunciando un precio que no es el que se cobra por defecto");
    }

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
                Version.SUBTITULADA, Proyeccion.DOS_D, Dinero.de(precio));
    }

    private Asiento asiento(Sala sala, String codigo) {
        return salas.asientosDe(sala.getId()).stream()
                .filter(a -> a.getCodigo().equals(codigo))
                .findFirst()
                .orElseThrow(() -> new AssertionError("La sala no tiene la butaca " + codigo));
    }

    private static AsientoVistaDTO butaca(SalaVistaDTO vista, String codigo) {
        return vista.asientos().stream()
                .filter(a -> a.codigo().equals(codigo))
                .findFirst()
                .orElseThrow(() -> new AssertionError("La vista no tiene la butaca " + codigo));
    }
}
