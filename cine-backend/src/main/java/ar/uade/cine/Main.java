package ar.uade.cine;

import ar.uade.cine.persistencia.EmpleadoDAO;
import ar.uade.cine.persistencia.AsientoDAO;
import ar.uade.cine.persistencia.ClienteDAO;
import ar.uade.cine.persistencia.CompraCandyDAO;
import ar.uade.cine.persistencia.FuncionDAO;
import ar.uade.cine.persistencia.GeneradorTicket;
import ar.uade.cine.persistencia.GeneradorTicketCandy;
import ar.uade.cine.persistencia.PagoDAO;
import ar.uade.cine.persistencia.PeliculaDAO;
import ar.uade.cine.persistencia.ProductoDAO;
import ar.uade.cine.persistencia.ReservaDAO;
import ar.uade.cine.persistencia.SalaDAO;
import ar.uade.cine.persistencia.archivo.GeneradorTicketCandyTxt;
import ar.uade.cine.persistencia.archivo.GeneradorTicketTxt;
import ar.uade.cine.persistencia.mysql.EmpleadoDAOMySQL;
import ar.uade.cine.persistencia.mysql.AsientoDAOMySQL;
import ar.uade.cine.persistencia.mysql.ClienteDAOMySQL;
import ar.uade.cine.persistencia.mysql.CompraCandyDAOMySQL;
import ar.uade.cine.persistencia.mysql.FuncionDAOMySQL;
import ar.uade.cine.persistencia.mysql.PagoDAOMySQL;
import ar.uade.cine.persistencia.mysql.PeliculaDAOMySQL;
import ar.uade.cine.persistencia.mysql.ProductoDAOMySQL;
import ar.uade.cine.persistencia.mysql.ReservaDAOMySQL;
import ar.uade.cine.persistencia.mysql.SalaDAOMySQL;
import ar.uade.cine.servicio.GestorEmpleados;
import ar.uade.cine.servicio.GestorCandy;
import ar.uade.cine.servicio.GestorCartelera;
import ar.uade.cine.servicio.GestorClientes;
import ar.uade.cine.servicio.GestorFunciones;
import ar.uade.cine.servicio.GestorPagos;
import ar.uade.cine.servicio.GestorReservas;
import ar.uade.cine.servicio.GestorSalas;
import ar.uade.cine.ui.MenuConsola;

/**
 * Único lugar donde se eligen las implementaciones concretas. Los gestores solo
 * conocen las interfaces, así que acá se decide en qué medio se guarda cada cosa:
 * cambiar una línea por ReservaDAOTxt hace que las reservas vayan a un archivo sin
 * tocar ninguna regla de negocio.
 */
public class Main {

    public static void main(String[] args) {
        PeliculaDAO peliculaDAO = new PeliculaDAOMySQL();
        SalaDAO salaDAO = new SalaDAOMySQL();
        FuncionDAO funcionDAO = new FuncionDAOMySQL();
        ClienteDAO clienteDAO = new ClienteDAOMySQL();
        AsientoDAO asientoDAO = new AsientoDAOMySQL();
        EmpleadoDAO empleadoDAO = new EmpleadoDAOMySQL();
        PagoDAO pagoDAO = new PagoDAOMySQL();
        ReservaDAO reservaDAO = new ReservaDAOMySQL();
        ProductoDAO productoDAO = new ProductoDAOMySQL();
        CompraCandyDAO compraCandyDAO = new CompraCandyDAOMySQL();

        GeneradorTicket generadorTicket = new GeneradorTicketTxt();
        GeneradorTicketCandy generadorTicketCandy = new GeneradorTicketCandyTxt();

        GestorCartelera gestorCartelera = new GestorCartelera(peliculaDAO, funcionDAO);
        GestorSalas gestorSalas = new GestorSalas(salaDAO, asientoDAO, funcionDAO);
        GestorFunciones gestorFunciones = new GestorFunciones(funcionDAO, peliculaDAO, salaDAO, reservaDAO);
        GestorClientes gestorClientes = new GestorClientes(clienteDAO, reservaDAO, compraCandyDAO);
        GestorEmpleados gestorAdministradores = new GestorEmpleados(empleadoDAO);
        GestorPagos gestorPagos = new GestorPagos(pagoDAO, reservaDAO);
        GestorReservas gestorReservas = new GestorReservas(
                reservaDAO, funcionDAO, salaDAO, asientoDAO, clienteDAO, peliculaDAO, generadorTicket);
        GestorCandy gestorCandy = new GestorCandy(
                productoDAO, compraCandyDAO, clienteDAO, generadorTicketCandy);

        new MenuConsola(gestorCartelera, gestorSalas, gestorFunciones, gestorClientes,
                gestorReservas, gestorAdministradores, gestorPagos, gestorCandy).iniciar();
    }
}
