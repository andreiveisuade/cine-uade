package ar.uade.cine.swing;

import ar.uade.cine.swing.api.Apis;
import ar.uade.cine.swing.pantallas.Destino;
import ar.uade.cine.swing.pantallas.Navegacion;
import ar.uade.cine.swing.pantallas.candy.PantallaCandy;
import ar.uade.cine.swing.pantallas.cartelera.PantallaImportador;
import ar.uade.cine.swing.pantallas.cartelera.PantallaPeliculas;
import ar.uade.cine.swing.pantallas.cartelera.PantallaPendientes;
import ar.uade.cine.swing.pantallas.funciones.PantallaAgenda;
import ar.uade.cine.swing.pantallas.funciones.PantallaFunciones;
import ar.uade.cine.swing.pantallas.informes.PantallaCaja;
import ar.uade.cine.swing.pantallas.informes.PantallaDeclaracionJurada;
import ar.uade.cine.swing.pantallas.informes.PantallaInformeFuncion;
import ar.uade.cine.swing.pantallas.programaciones.PantallaPlanificador;
import ar.uade.cine.swing.pantallas.programaciones.PantallaProgramaciones;
import ar.uade.cine.swing.pantallas.promociones.PantallaPromociones;
import ar.uade.cine.swing.pantallas.salas.PantallaMapaSala;
import ar.uade.cine.swing.pantallas.salas.PantallaSalas;
import ar.uade.cine.swing.pantallas.ventas.PantallaCobro;
import ar.uade.cine.swing.pantallas.ventas.PantallaPuerta;
import ar.uade.cine.swing.pantallas.ventas.PantallaReservas;

import javax.swing.JComponent;

// Crea cada pantalla con solo las Api que usa; Creador (GRASP): es la única clase que las tiene todas a mano.
/**
 * Aparte de VentanaPrincipal para que el marco quede en cabecera y menú, y para poder armar cada pantalla en un test
 * sin abrir una ventana. Cada llamada crea una pantalla nueva, que es lo que hace que muestre datos frescos.
 */
final class Pantallas {

    private final Apis apis;
    private final Navegacion navegacion;

    Pantallas(Apis apis, Navegacion navegacion) {
        this.apis = apis;
        this.navegacion = navegacion;
    }

    // Sin default a propósito: un destino nuevo sin pantalla no compila.
    JComponent de(Destino destino) {
        return switch (destino) {
            case PELICULAS -> new PantallaPeliculas(apis.cartelera(), apis.catalogos());
            case POR_REVISAR -> new PantallaPendientes(apis.cartelera(), navegacion);
            case IMPORTADOR -> new PantallaImportador(apis.cartelera(), navegacion);
            case SALAS -> new PantallaSalas(apis.catalogos(), apis.salas(), navegacion);
            case FUNCIONES -> new PantallaFunciones(apis.cartelera(), apis.catalogos(), apis.funciones(),
                    apis.salas(), navegacion);
            case GRILLA -> new PantallaProgramaciones(apis.cartelera(), apis.catalogos(), apis.programaciones(),
                    apis.salas());
            case PLANIFICADOR -> new PantallaPlanificador(apis.catalogos(), apis.programaciones());
            case AGENDA -> new PantallaAgenda(apis.funciones(), apis.salas(), navegacion);
            case RESERVAS -> new PantallaReservas(apis.ventas(), navegacion);
            case PROMOCIONES -> new PantallaPromociones(apis.catalogos(), apis.promociones());
            case CANDY -> new PantallaCandy(apis.candy(), apis.catalogos(), apis.clientes());
            case CAJA -> new PantallaCaja(apis.informes());
            case DECLARACION_JURADA -> new PantallaDeclaracionJurada(apis.catalogos(), apis.informes());
            case PUERTA -> new PantallaPuerta(apis.catalogos(), apis.ventas());
        };
    }

    JComponent informe(int funcionId) {
        return new PantallaInformeFuncion(apis.catalogos(), apis.funciones(), apis.informes(), navegacion, funcionId);
    }

    JComponent cobro(int reservaId) {
        return new PantallaCobro(apis.catalogos(), apis.ventas(), navegacion, reservaId);
    }

    JComponent mapa(int salaId) {
        return new PantallaMapaSala(apis.salas(), navegacion, salaId);
    }
}
