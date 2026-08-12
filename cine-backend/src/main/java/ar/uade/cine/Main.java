package ar.uade.cine;

import ar.uade.cine.interfaces.ClienteDAO;
import ar.uade.cine.interfaces.FuncionDAO;
import ar.uade.cine.interfaces.GeneradorTicket;
import ar.uade.cine.interfaces.PeliculaDAO;
import ar.uade.cine.interfaces.ReservaDAO;
import ar.uade.cine.interfaces.SalaDAO;
import ar.uade.cine.persistencia.ClienteDAOMySQL;
import ar.uade.cine.persistencia.FuncionDAOMySQL;
import ar.uade.cine.persistencia.GeneradorTicketTxt;
import ar.uade.cine.persistencia.PeliculaDAOMySQL;
import ar.uade.cine.persistencia.ReservaDAOMySQL;
import ar.uade.cine.persistencia.SalaDAOMySQL;
import ar.uade.cine.servicio.GestorCartelera;
import ar.uade.cine.servicio.GestorClientes;
import ar.uade.cine.servicio.GestorFunciones;
import ar.uade.cine.servicio.GestorReservas;
import ar.uade.cine.servicio.GestorSalas;
import ar.uade.cine.ui.MenuConsola;

/**
 * Único lugar donde se eligen las implementaciones concretas. Los gestores solo
 * conocen las interfaces, así que acá se decide en qué medio se guarda cada cosa.
 */
public class Main {

    public static void main(String[] args) {
        PeliculaDAO peliculaDAO = new PeliculaDAOMySQL();
        SalaDAO salaDAO = new SalaDAOMySQL();
        FuncionDAO funcionDAO = new FuncionDAOMySQL();
        ClienteDAO clienteDAO = new ClienteDAOMySQL();

        ReservaDAO reservaDAO = new ReservaDAOMySQL();
        // Cambiando esta línea las reservas pasan a guardarse en reservas.txt,
        // sin tocar una sola línea de GestorReservas ni del menú:
        // ReservaDAO reservaDAO = new ReservaDAOTxt();

        GeneradorTicket generadorTicket = new GeneradorTicketTxt();

        GestorCartelera gestorCartelera = new GestorCartelera(peliculaDAO);
        GestorSalas gestorSalas = new GestorSalas(salaDAO);
        GestorFunciones gestorFunciones = new GestorFunciones(funcionDAO, peliculaDAO, salaDAO);
        GestorClientes gestorClientes = new GestorClientes(clienteDAO);
        GestorReservas gestorReservas = new GestorReservas(
                reservaDAO, funcionDAO, salaDAO, clienteDAO, peliculaDAO, generadorTicket);

        new MenuConsola(gestorCartelera, gestorSalas, gestorFunciones, gestorClientes, gestorReservas).iniciar();
    }
}
