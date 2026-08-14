package ar.uade.cine;

import ar.uade.cine.persistencia.AsientoDAO;
import ar.uade.cine.persistencia.ClienteDAO;
import ar.uade.cine.persistencia.CompraCandyDAO;
import ar.uade.cine.persistencia.EmpleadoDAO;
import ar.uade.cine.persistencia.FuncionDAO;
import ar.uade.cine.comprobantes.GeneradorRecibo;
import ar.uade.cine.comprobantes.GeneradorTicket;
import ar.uade.cine.comprobantes.GeneradorTicketCandy;
import ar.uade.cine.persistencia.PagoDAO;
import ar.uade.cine.persistencia.PeliculaDAO;
import ar.uade.cine.persistencia.ProductoDAO;
import ar.uade.cine.persistencia.ProgramacionDAO;
import ar.uade.cine.persistencia.PromocionDAO;
import ar.uade.cine.persistencia.ReservaDAO;
import ar.uade.cine.persistencia.SalaDAO;
import ar.uade.cine.comprobantes.txt.GeneradorReciboTxt;
import ar.uade.cine.comprobantes.txt.GeneradorTicketCandyTxt;
import ar.uade.cine.comprobantes.txt.GeneradorTicketTxt;
import ar.uade.cine.pasarelas.PasarelaPagos;
import ar.uade.cine.pasarelas.emulada.MercadoPagoEmulado;
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
import ar.uade.cine.servicio.CalculadoraPrecio;
import ar.uade.cine.servicio.GestorCandy;
import ar.uade.cine.servicio.GestorCartelera;
import ar.uade.cine.servicio.GestorClientes;
import ar.uade.cine.servicio.GestorEmpleados;
import ar.uade.cine.servicio.GestorFunciones;
import ar.uade.cine.servicio.GestorPagos;
import ar.uade.cine.servicio.GestorProductos;
import ar.uade.cine.servicio.GestorProgramaciones;
import ar.uade.cine.servicio.GestorPromociones;
import ar.uade.cine.servicio.GestorReservas;
import ar.uade.cine.servicio.GestorSalas;
import ar.uade.cine.servicio.Ocupacion;

/**
 * La aplicación armada: los gestores, ya conectados a dónde se guarda cada cosa.
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
    private final Ocupacion ocupacion;
    private final GestorProductos productos;
    private final GestorCandy candy;
    private final CalculadoraPrecio calculadoraPrecio;

    /**
     * Todo en MySQL, con los comprobantes en archivos de texto y la pasarela emulada: es
     * como corren la consola y la API.
     *
     * <p>Los tickets van a disco y no a la base a propósito. Un comprobante es un papel
     * que se entrega, no un dato que se consulta: por eso su contrato es
     * {@link GeneradorTicket} y no un DAO.
     *
     * <p>{@link MercadoPagoEmulado} es la única implementación de pasarela que hay, y este
     * es el único lugar que la nombra: enchufar la integración de verdad —con credenciales
     * y llamadas de red— es cambiar este argumento, sin tocar una regla de negocio.
     */
    public static Aplicacion enMySQL() {
        return new Aplicacion(
                new PeliculaDAOMySQL(), new SalaDAOMySQL(), new AsientoDAOMySQL(),
                new FuncionDAOMySQL(), new ClienteDAOMySQL(), new EmpleadoDAOMySQL(),
                new ReservaDAOMySQL(), new PagoDAOMySQL(), new PromocionDAOMySQL(),
                new ProgramacionDAOMySQL(), new ProductoDAOMySQL(), new CompraCandyDAOMySQL(),
                new GeneradorTicketTxt(), new GeneradorTicketCandyTxt(),
                new GeneradorReciboTxt(), new MercadoPagoEmulado());
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
                      GeneradorTicket generadorTicket, GeneradorTicketCandy generadorTicketCandy,
                      GeneradorRecibo generadorRecibo, PasarelaPagos pasarela) {

        calculadoraPrecio = new CalculadoraPrecio();

        salas = new GestorSalas(salaDAO, asientoDAO, funcionDAO);
        funciones = new GestorFunciones(funcionDAO, peliculaDAO, salaDAO, reservaDAO);
        programaciones = new GestorProgramaciones(programacionDAO, funcionDAO, funciones);
        // Después de programaciones: la cartelera las extiende antes de listar.
        cartelera = new GestorCartelera(peliculaDAO, funcionDAO, programaciones);
        clientes = new GestorClientes(clienteDAO, reservaDAO, compraCandyDAO);
        empleados = new GestorEmpleados(empleadoDAO);
        promociones = new GestorPromociones(promocionDAO);
        pagos = new GestorPagos(pagoDAO, reservaDAO, funcionDAO, promociones, pasarela,
                generadorRecibo);
        ocupacion = new Ocupacion(reservaDAO, funcionDAO, asientoDAO);
        reservas = new GestorReservas(reservaDAO, funcionDAO, salaDAO, asientoDAO, clienteDAO,
                peliculaDAO, generadorTicket, calculadoraPrecio, ocupacion);
        productos = new GestorProductos(productoDAO);
        candy = new GestorCandy(compraCandyDAO, clienteDAO, reservaDAO, generadorTicketCandy,
                productos);
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

    /** Qué butacas están tomadas en cada función: lo que necesita el mapa de la sala. */
    public Ocupacion getOcupacion() {
        return ocupacion;
    }

    /** La carta del candy: el ABM de productos y combos. */
    public GestorProductos getProductos() {
        return productos;
    }

    /** Las ventas del candy, que le preguntan los precios a {@link #getProductos()}. */
    public GestorCandy getCandy() {
        return candy;
    }

    /** La usa la capa HTTP para mostrar el precio de cada butaca en el mapa de la sala. */
    public CalculadoraPrecio getCalculadoraPrecio() {
        return calculadoraPrecio;
    }
}
