package ar.uade.cine;

import ar.uade.cine.persistencia.AsientoDAO;
import ar.uade.cine.persistencia.BloqueoButacas;
import ar.uade.cine.persistencia.ClienteDAO;
import ar.uade.cine.persistencia.CompraCandyDAO;
import ar.uade.cine.persistencia.EmpleadoDAO;
import ar.uade.cine.persistencia.FuncionDAO;
import ar.uade.cine.persistencia.ImportacionDAO;
import ar.uade.cine.infraestructura.comprobantes.GeneradorBordero;
import ar.uade.cine.infraestructura.comprobantes.GeneradorRecibo;
import ar.uade.cine.infraestructura.comprobantes.GeneradorTicket;
import ar.uade.cine.infraestructura.comprobantes.GeneradorTicketCandy;
import ar.uade.cine.persistencia.PagoDAO;
import ar.uade.cine.persistencia.PeliculaDAO;
import ar.uade.cine.persistencia.ProductoDAO;
import ar.uade.cine.persistencia.ProgramacionDAO;
import ar.uade.cine.persistencia.PromocionDAO;
import ar.uade.cine.persistencia.ReservaDAO;
import ar.uade.cine.persistencia.SalaDAO;
import ar.uade.cine.infraestructura.comprobantes.txt.GeneradorBorderoTxt;
import ar.uade.cine.infraestructura.comprobantes.txt.GeneradorReciboTxt;
import ar.uade.cine.infraestructura.comprobantes.txt.GeneradorTicketCandyTxt;
import ar.uade.cine.infraestructura.comprobantes.txt.GeneradorTicketTxt;
import ar.uade.cine.infraestructura.importador.ImportadorCartelera;
import ar.uade.cine.infraestructura.importador.http.ImportadorHttp;
import ar.uade.cine.infraestructura.pasarelas.PasarelaPagos;
import ar.uade.cine.infraestructura.pasarelas.emulada.MercadoPagoEmulado;
import ar.uade.cine.persistencia.mysql.AsientoDAOMySQL;
import ar.uade.cine.persistencia.mysql.OrigenMySQL;
import ar.uade.cine.persistencia.mysql.Plantilla;
import ar.uade.cine.persistencia.mysql.ClienteDAOMySQL;
import ar.uade.cine.persistencia.mysql.CompraCandyDAOMySQL;
import ar.uade.cine.persistencia.mysql.EmpleadoDAOMySQL;
import ar.uade.cine.persistencia.mysql.FuncionDAOMySQL;
import ar.uade.cine.persistencia.mysql.ImportacionDAOMySQL;
import ar.uade.cine.persistencia.mysql.PagoDAOMySQL;
import ar.uade.cine.persistencia.mysql.PeliculaDAOMySQL;
import ar.uade.cine.persistencia.mysql.ProductoDAOMySQL;
import ar.uade.cine.persistencia.mysql.ProgramacionDAOMySQL;
import ar.uade.cine.persistencia.mysql.PromocionDAOMySQL;
import ar.uade.cine.persistencia.mysql.ReservaDAOMySQL;
import ar.uade.cine.persistencia.mysql.SalaDAOMySQL;
import ar.uade.cine.persistencia.redis.BloqueoButacasRedis;
import ar.uade.cine.servicio.ventas.CalculadoraPrecio;
import ar.uade.cine.servicio.candy.GestorCandy;
import ar.uade.cine.servicio.cartelera.GestorCartelera;
import ar.uade.cine.servicio.cartelera.GestorImportaciones;
import ar.uade.cine.servicio.cartelera.GestorRevisionCartelera;
import ar.uade.cine.servicio.usuarios.GestorClientes;
import ar.uade.cine.servicio.usuarios.GestorEmpleados;
import ar.uade.cine.servicio.funciones.GestorFunciones;
import ar.uade.cine.servicio.informes.GestorCaja;
import ar.uade.cine.servicio.informes.GestorInformes;
import ar.uade.cine.servicio.ventas.GestorPagos;
import ar.uade.cine.servicio.candy.GestorProductos;
import ar.uade.cine.servicio.programaciones.GestorProgramaciones;
import ar.uade.cine.servicio.programaciones.PlanificadorGrilla;
import ar.uade.cine.servicio.promociones.GestorPromociones;
import ar.uade.cine.servicio.ventas.GestorReservas;
import ar.uade.cine.servicio.salas.GestorSalas;
import ar.uade.cine.servicio.ventas.Ocupacion;

/**
 * La aplicación armada: los gestores, ya conectados a dónde se guarda cada cosa.
 *
 * <p>Es el <strong>único</strong> lugar del sistema donde se nombra una implementación
 * concreta. Los gestores solo conocen las interfaces de <code>persistencia</code>, así que
 * acá se decide en qué medio va cada cosa y nada más que acá.
 *
 * <p>Existe porque el sistema se levanta desde más de un lado y todos necesitan
 * exactamente la misma aplicación abajo: {@link ar.uade.cine.api.ServidorApi} en
 * producción, y {@code AplicacionTest} en memoria para recorrer el circuito completo sin
 * tocar MySQL. Hubo una época en que cada arranque armaba la suya y las copias se
 * separaron: una se quedó sin GestorCandy, y el candy dejó de existir para quien entraba
 * por ahí aunque el gestor estuviera escrito y probado. Con un solo armado eso no puede
 * volver a pasar: sumar un gestor lo hace aparecer en todas las puertas a la vez.
 *
 * <p>Esa es también la razón por la que el constructor es público: es lo que deja que un
 * test levante el sistema entero contra las implementaciones en memoria y verifique que
 * los gestores están conectados entre sí, y no solo que cada uno anda por su cuenta.
 */
public class Aplicacion {

    private final GestorCartelera cartelera;
    private final GestorRevisionCartelera revisionCartelera;
    private final GestorImportaciones importaciones;
    private final GestorSalas salas;
    private final GestorFunciones funciones;
    private final GestorProgramaciones programaciones;
    private final PlanificadorGrilla planificadorGrilla;
    private final GestorClientes clientes;
    private final GestorEmpleados empleados;
    private final GestorPromociones promociones;
    private final GestorPagos pagos;
    private final GestorReservas reservas;
    private final Ocupacion ocupacion;
    private final GestorProductos productos;
    private final GestorCandy candy;
    private final GestorInformes informes;
    private final GestorCaja caja;
    private final CalculadoraPrecio calculadoraPrecio;

    /**
     * Todo en MySQL, con los comprobantes en archivos de texto y la pasarela emulada: es
     * como corre la API en producción.
     *
     * <p>Los tickets van a disco y no a la base a propósito. Un comprobante es un papel
     * que se entrega, no un dato que se consulta: por eso su contrato es
     * {@link GeneradorTicket} y no un DAO.
     *
     * <p>Los bloqueos de butaca van a Redis y no a MySQL porque son lo contrario de todo
     * lo demás que hay acá: duran tres minutos y después no le importan a nadie. Si Redis
     * no está, {@link BloqueoButacasRedis} degrada a "ningún bloqueo" y el sistema sigue
     * vendiendo como antes de que existiera.
     *
     * <p>{@link MercadoPagoEmulado} es la única implementación de pasarela que hay, y este
     * es el único lugar que la nombra: enchufar la integración de verdad —con credenciales
     * y llamadas de red— es cambiar este argumento, sin tocar una regla de negocio.
     *
     * <p>{@link ImportadorHttp} es lo mismo del otro lado: el que sale a buscar la cartelera
     * real es un proceso aparte, y acá se dice dónde está. Un test le pasa otro y prueba el
     * circuito entero sin TMDB.
     */
    public static Aplicacion enMySQL() {
        // Un solo pool y una sola plantilla para los trece DAO. Va acá por el mismo motivo
        // que los DAO: este es el único lugar que nombra implementaciones concretas, y de
        // dónde salen las conexiones es una decisión de armado, no de cada DAO. Que la
        // compartan es además lo que hace que el pool sirva: trece pools de diez
        // conexiones serían ciento treinta conexiones contra la misma base.
        Plantilla plantilla = new Plantilla(OrigenMySQL.conPool());

        return new Aplicacion(
                new PeliculaDAOMySQL(plantilla), new SalaDAOMySQL(plantilla),
                new AsientoDAOMySQL(plantilla), new FuncionDAOMySQL(plantilla),
                new ClienteDAOMySQL(plantilla), new EmpleadoDAOMySQL(plantilla),
                new ReservaDAOMySQL(plantilla), new PagoDAOMySQL(plantilla),
                new PromocionDAOMySQL(plantilla), new ProgramacionDAOMySQL(plantilla),
                new ProductoDAOMySQL(plantilla), new CompraCandyDAOMySQL(plantilla),
                new ImportacionDAOMySQL(plantilla), new BloqueoButacasRedis(),
                new GeneradorTicketTxt(), new GeneradorTicketCandyTxt(),
                new GeneradorReciboTxt(), new GeneradorBorderoTxt(), new MercadoPagoEmulado(),
                new ImportadorHttp());
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
                      CompraCandyDAO compraCandyDAO, ImportacionDAO importacionDAO,
                      BloqueoButacas bloqueoButacas,
                      GeneradorTicket generadorTicket, GeneradorTicketCandy generadorTicketCandy,
                      GeneradorRecibo generadorRecibo, GeneradorBordero generadorBordero,
                      PasarelaPagos pasarela, ImportadorCartelera importador) {

        calculadoraPrecio = new CalculadoraPrecio();

        salas = new GestorSalas(salaDAO, asientoDAO, funcionDAO);
        funciones = new GestorFunciones(funcionDAO, peliculaDAO, salaDAO, reservaDAO);
        programaciones = new GestorProgramaciones(programacionDAO, funcionDAO, funciones);
        // Después de programaciones: la cartelera las extiende antes de listar.
        cartelera = new GestorCartelera(peliculaDAO, funcionDAO, programaciones);
        // El buzon del importador cuelga del catalogo: revisar necesita dar de alta,
        // pero el catalogo no necesita saber que existe un importador.
        revisionCartelera = new GestorRevisionCartelera(peliculaDAO, funcionDAO, cartelera);
        // No depende de la cartelera ni del buzon: lo que el importador trae entra por la
        // API, como cualquier cliente. Este gestor solo despierta al importador y anota.
        importaciones = new GestorImportaciones(importacionDAO, importador);
        planificadorGrilla = new PlanificadorGrilla(peliculaDAO, salaDAO, funciones);
        clientes = new GestorClientes(clienteDAO, reservaDAO, compraCandyDAO);
        empleados = new GestorEmpleados(empleadoDAO);
        promociones = new GestorPromociones(promocionDAO);
        pagos = new GestorPagos(pagoDAO, reservaDAO, funcionDAO, promociones, pasarela,
                generadorRecibo);
        ocupacion = new Ocupacion(reservaDAO, funcionDAO, asientoDAO, bloqueoButacas);
        reservas = new GestorReservas(reservaDAO, funcionDAO, salaDAO, asientoDAO, clienteDAO,
                peliculaDAO, generadorTicket, calculadoraPrecio, ocupacion);
        productos = new GestorProductos(productoDAO);
        candy = new GestorCandy(compraCandyDAO, clienteDAO, reservaDAO, generadorTicketCandy,
                productos);
        informes = new GestorInformes(funcionDAO, peliculaDAO, salaDAO, reservaDAO, pagoDAO,
                compraCandyDAO, generadorBordero);
        caja = new GestorCaja(pagoDAO, reservaDAO, compraCandyDAO);
    }

    public GestorCartelera getCartelera() {
        return cartelera;
    }

    /** El buzon de lo que trajo el importador y todavia nadie miro. */
    public GestorRevisionCartelera getRevisionCartelera() {
        return revisionCartelera;
    }

    /** Pedir cartelera nueva a demanda, y el historial de lo que se pidio. */
    public GestorImportaciones getImportaciones() {
        return importaciones;
    }

    public GestorSalas getSalas() {
        return salas;
    }

    public GestorFunciones getFunciones() {
        return funciones;
    }

    public PlanificadorGrilla getPlanificadorGrilla() {
        return planificadorGrilla;
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

    /** El borderó del INCAA y la recaudación de cada función, entradas más candy. */
    public GestorInformes getInformes() {
        return informes;
    }

    /** El cierre de caja del día: boletería y candy. El otro corte de los informes. */
    public GestorCaja getCaja() {
        return caja;
    }

    /** La usa la capa HTTP para mostrar el precio de cada butaca en el mapa de la sala. */
    public CalculadoraPrecio getCalculadoraPrecio() {
        return calculadoraPrecio;
    }
}
