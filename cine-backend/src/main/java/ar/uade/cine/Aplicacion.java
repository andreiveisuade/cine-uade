package ar.uade.cine;

import ar.uade.cine.persistencia.AsientoDAO;
import ar.uade.cine.persistencia.ClienteDAO;
import ar.uade.cine.persistencia.CompraCandyDAO;
import ar.uade.cine.persistencia.EmpleadoDAO;
import ar.uade.cine.persistencia.FuncionDAO;
import ar.uade.cine.persistencia.GeneradorTicket;
import ar.uade.cine.persistencia.GeneradorTicketCandy;
import ar.uade.cine.persistencia.PagoDAO;
import ar.uade.cine.persistencia.PeliculaDAO;
import ar.uade.cine.persistencia.ProductoDAO;
import ar.uade.cine.persistencia.ProgramacionDAO;
import ar.uade.cine.persistencia.PromocionDAO;
import ar.uade.cine.persistencia.ReservaDAO;
import ar.uade.cine.persistencia.SalaDAO;
import ar.uade.cine.persistencia.archivo.GeneradorTicketCandyTxt;
import ar.uade.cine.persistencia.archivo.GeneradorTicketTxt;
import ar.uade.cine.persistencia.mysql.AsientoDAOMySQL;
import ar.uade.cine.persistencia.mysql.ClienteDAOMySQL;
import ar.uade.cine.persistencia.mysql.CompraCandyDAOMySQL;
import ar.uade.cine.persistencia.mysql.EmpleadoDAOMySQL;
import ar.uade.cine.persistencia.mysql.FuncionDAOMySQL;
import ar.uade.cine.persistencia.mysql.PagoDAOMySQL;
import ar.uade.cine.persistencia.mysql.PeliculaDAOMySQL;
import ar.uade.cine.persistencia.mysql.ProductoDAOMySQL;
import ar.uade.cine.persistencia.mysql.ProgramacionDAOMySQL;
import ar.uade.cine.persistencia.mysql.PromocionDAOMySQL;
import ar.uade.cine.persistencia.mysql.ReservaDAOMySQL;
import ar.uade.cine.persistencia.mysql.SalaDAOMySQL;
import ar.uade.cine.servicio.GestorCandy;
import ar.uade.cine.servicio.GestorCartelera;
import ar.uade.cine.servicio.GestorClientes;
import ar.uade.cine.servicio.GestorEmpleados;
import ar.uade.cine.servicio.GestorFunciones;
import ar.uade.cine.servicio.GestorPagos;
import ar.uade.cine.servicio.GestorProgramaciones;
import ar.uade.cine.servicio.GestorPromociones;
import ar.uade.cine.servicio.GestorReservas;
import ar.uade.cine.servicio.GestorSalas;

/**
 * La aplicación armada: los diez gestores, ya conectados a dónde se guarda cada cosa.
 *
 * <p>Es el <strong>único</strong> lugar del sistema donde se nombra una implementación
 * concreta. Los gestores solo conocen las interfaces de <code>persistencia</code>, así que
 * acá se decide en qué medio va cada cosa y nada más que acá.
 *
 * <p>Existe porque hay dos maneras de entrar al sistema —{@link Main} por consola y
 * {@link ar.uade.cine.api.ServidorApi} por HTTP— y las dos necesitan exactamente la misma
 * aplicación abajo. Cuando cada arranque armaba la suya, las dos copias se separaron:
 * la de HTTP se quedó sin GestorCandy, y el candy dejó de existir para el que entraba por
 * la web aunque el gestor estuviera escrito y probado. Con un solo armado eso no puede
 * volver a pasar: sumar un gestor lo hace aparecer en las dos puertas a la vez.
 */
public class Aplicacion {

    private final GestorCartelera cartelera;
    private final GestorSalas salas;
    private final GestorFunciones funciones;
    private final GestorProgramaciones programaciones;
    private final GestorClientes clientes;
    private final GestorEmpleados empleados;
    private final GestorPromociones promociones;
    private final GestorPagos pagos;
    private final GestorReservas reservas;
    private final GestorCandy candy;

    /**
     * Todo en MySQL, con los comprobantes en archivos de texto: es como corren la consola
     * y la API.
     *
     * <p>Los tickets van a disco y no a la base a propósito. Un comprobante es un papel
     * que se entrega, no un dato que se consulta: por eso su contrato es
     * {@link GeneradorTicket} y no un DAO.
     */
    public static Aplicacion enMySQL() {
        return new Aplicacion(
                new PeliculaDAOMySQL(), new SalaDAOMySQL(), new AsientoDAOMySQL(),
                new FuncionDAOMySQL(), new ClienteDAOMySQL(), new EmpleadoDAOMySQL(),
                new ReservaDAOMySQL(), new PagoDAOMySQL(), new PromocionDAOMySQL(),
                new ProgramacionDAOMySQL(), new ProductoDAOMySQL(), new CompraCandyDAOMySQL(),
                new GeneradorTicketTxt(), new GeneradorTicketCandyTxt());
    }

    /**
     * Arma los gestores contra las implementaciones que se le pasen. Cambiar dónde se
     * guarda algo es cambiar un argumento: pasarle un
     * {@link ar.uade.cine.persistencia.archivo.ReservaDAOTxt} manda las reservas a un
     * archivo sin tocar una sola regla de negocio.
     *
     * <p>Es público justamente para eso, y para que un test pueda levantar el sistema
     * entero en memoria.
     */
    public Aplicacion(PeliculaDAO peliculaDAO, SalaDAO salaDAO, AsientoDAO asientoDAO,
                      FuncionDAO funcionDAO, ClienteDAO clienteDAO, EmpleadoDAO empleadoDAO,
                      ReservaDAO reservaDAO, PagoDAO pagoDAO, PromocionDAO promocionDAO,
                      ProgramacionDAO programacionDAO, ProductoDAO productoDAO,
                      CompraCandyDAO compraCandyDAO,
                      GeneradorTicket generadorTicket, GeneradorTicketCandy generadorTicketCandy) {

        cartelera = new GestorCartelera(peliculaDAO, funcionDAO);
        salas = new GestorSalas(salaDAO, asientoDAO, funcionDAO);
        funciones = new GestorFunciones(funcionDAO, peliculaDAO, salaDAO, reservaDAO);
        programaciones = new GestorProgramaciones(programacionDAO, funcionDAO, funciones);
        clientes = new GestorClientes(clienteDAO, reservaDAO, compraCandyDAO);
        empleados = new GestorEmpleados(empleadoDAO);
        promociones = new GestorPromociones(promocionDAO);
        pagos = new GestorPagos(pagoDAO, reservaDAO, funcionDAO, promociones);
        reservas = new GestorReservas(reservaDAO, funcionDAO, salaDAO, asientoDAO, clienteDAO,
                peliculaDAO, generadorTicket);
        candy = new GestorCandy(productoDAO, compraCandyDAO, clienteDAO, reservaDAO,
                generadorTicketCandy);
    }

    public GestorCartelera getCartelera() {
        return cartelera;
    }

    public GestorSalas getSalas() {
        return salas;
    }

    public GestorFunciones getFunciones() {
        return funciones;
    }

    public GestorProgramaciones getProgramaciones() {
        return programaciones;
    }

    public GestorClientes getClientes() {
        return clientes;
    }

    public GestorEmpleados getEmpleados() {
        return empleados;
    }

    public GestorPromociones getPromociones() {
        return promociones;
    }

    public GestorPagos getPagos() {
        return pagos;
    }

    public GestorReservas getReservas() {
        return reservas;
    }

    public GestorCandy getCandy() {
        return candy;
    }
}
