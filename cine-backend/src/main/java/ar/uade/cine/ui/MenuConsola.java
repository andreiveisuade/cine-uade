package ar.uade.cine.ui;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

import ar.uade.cine.dominio.candy.CompraCandy;
import ar.uade.cine.dominio.candy.TipoProducto;
import ar.uade.cine.dominio.cartelera.Clasificacion;
import ar.uade.cine.dominio.cartelera.Genero;
import ar.uade.cine.dominio.cartelera.Pelicula;
import ar.uade.cine.dominio.funciones.Funcion;
import ar.uade.cine.dominio.funciones.Proyeccion;
import ar.uade.cine.dominio.funciones.Version;
import ar.uade.cine.dominio.salas.Asiento;
import ar.uade.cine.dominio.salas.EstadoAsiento;
import ar.uade.cine.dominio.salas.TipoAsiento;
import ar.uade.cine.dominio.salas.TipoSala;
import ar.uade.cine.dominio.usuarios.Empleado;
import ar.uade.cine.dominio.usuarios.Rol;
import ar.uade.cine.dominio.ventas.Entrada;
import ar.uade.cine.dominio.ventas.MedioPago;
import ar.uade.cine.dominio.ventas.Pago;
import ar.uade.cine.dominio.ventas.Reserva;
import ar.uade.cine.dominio.ventas.TipoTarifa;
import ar.uade.cine.persistencia.ButacaOcupadaException;
import ar.uade.cine.servicio.GestorEmpleados;
import ar.uade.cine.servicio.GestorCandy;
import ar.uade.cine.servicio.GestorCartelera;
import ar.uade.cine.servicio.GestorClientes;
import ar.uade.cine.servicio.GestorFunciones;
import ar.uade.cine.servicio.GestorPagos;
import ar.uade.cine.servicio.GestorReservas;
import ar.uade.cine.servicio.GestorSalas;

/**
 * Única capa que habla con el usuario: acá viven Scanner y System.out, y en ningún
 * otro lado. Si mañana esto fuera una API REST, se reemplaza solo esta clase.
 */
public class MenuConsola {

    private static final DateTimeFormatter FORMATO_FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final GestorCartelera cartelera;
    private final GestorSalas salas;
    private final GestorFunciones funciones;
    private final GestorClientes clientes;
    private final GestorReservas reservas;
    private final GestorEmpleados empleados;
    private final GestorPagos pagos;
    private final GestorCandy candy;
    private final Scanner scanner = new Scanner(System.in);

    public MenuConsola(GestorCartelera cartelera, GestorSalas salas, GestorFunciones funciones,
                       GestorClientes clientes, GestorReservas reservas,
                       GestorEmpleados empleados, GestorPagos pagos, GestorCandy candy) {
        this.cartelera = cartelera;
        this.salas = salas;
        this.funciones = funciones;
        this.clientes = clientes;
        this.reservas = reservas;
        this.empleados = empleados;
        this.pagos = pagos;
        this.candy = candy;
    }

    public void iniciar() {
        boolean salir = false;
        while (!salir) {
            System.out.println("\n===== CINE =====");
            System.out.println("1. Películas");
            System.out.println("2. Salas");
            System.out.println("3. Funciones");
            System.out.println("4. Clientes");
            System.out.println("5. Reservas");
            System.out.println("6. Candy");
            System.out.println("7. Arqueo del día");
            System.out.println("8. Empleados");
            System.out.println("0. Salir");
            System.out.print("Opción: ");

            try {
                switch (leer()) {
                    case "1" -> menuPeliculas();
                    case "2" -> menuSalas();
                    case "3" -> menuFunciones();
                    case "4" -> menuClientes();
                    case "5" -> menuReservas();
                    case "6" -> menuCandy();
                    case "7" -> arqueoDelDia();
                    case "8" -> menuEmpleados();
                    case "0" -> salir = true;
                    default -> System.out.println("Opción inválida");
                }
            } catch (NumberFormatException e) {
                System.out.println("Se esperaba un número");
            } catch (IllegalArgumentException e) {
                System.out.println(e.getMessage());
            } catch (ButacaOcupadaException e) {
                // Se la ganaron por milisegundos: no es un error de quien opera.
                System.out.println(e.getMessage() + ". Volvé a elegir sobre el mapa actualizado.");
            }
        }
    }

    // ---------- películas ----------

    private void menuPeliculas() {
        System.out.println("\n-- Películas --");
        System.out.println("1. Listar  2. Agregar  3. Buscar por id  4. Eliminar  5. Filtrar por género");
        System.out.println("6. Completar datos de catálogo  7. Ver solo cartelera");
        System.out.print("Opción: ");
        switch (leer()) {
            case "1" -> imprimir(cartelera.listar());
            case "2" -> {
                System.out.print("Título: ");
                String titulo = leer();
                int duracion = leerEntero("Duración en minutos: ");
                List<Genero> generos = pedirGeneros();
                cartelera.agregar(titulo, duracion, generos, elegir("Clasificación", Clasificacion.values()));
                System.out.println("Película agregada");
            }
            case "3" -> {
                int id = leerEntero("Id: ");
                cartelera.buscar(id).ifPresentOrElse(
                        System.out::println,
                        () -> System.out.println("No existe la película " + id));
            }
            case "4" -> {
                cartelera.eliminar(leerEntero("Id: "));
                System.out.println("Película eliminada");
            }
            case "5" -> imprimir(cartelera.listarPorGenero(elegir("Género", Genero.values())));
            case "6" -> completarCatalogo();
            case "7" -> imprimir(cartelera.listarEnCartelera());
            default -> System.out.println("Opción inválida");
        }
    }

    /** Los datos de catálogo son opcionales: se cargan aparte del alta. */
    private void completarCatalogo() {
        int id = leerEntero("Id de película: ");
        Pelicula pelicula = cartelera.buscar(id)
                .orElseThrow(() -> new IllegalArgumentException("No existe la película " + id));

        System.out.print("Director: ");
        pelicula.setDirector(leer());
        System.out.print("Sinopsis: ");
        pelicula.setSinopsis(leer());
        pelicula.setAnio(leerEntero("Año: "));
        System.out.print("Idioma original: ");
        pelicula.setIdiomaOriginal(leer());
        System.out.print("URL del poster: ");
        pelicula.setPosterUrl(leer());

        cartelera.actualizar(pelicula);
        System.out.println("Datos de catálogo guardados");
    }

    /** Una película puede tener varios géneros: se aceptan varios números separados por coma. */
    private List<Genero> pedirGeneros() {
        Genero[] opciones = Genero.values();
        listarOpciones(opciones);
        System.out.print("Géneros (números separados por coma): ");

        List<Genero> elegidos = new ArrayList<>();
        for (String parte : leer().split(",")) {
            elegidos.add(porIndice(opciones, Integer.parseInt(parte.trim()), "Género"));
        }
        return elegidos;
    }

    // ---------- salas ----------

    private void menuSalas() {
        System.out.println("\n-- Salas --");
        System.out.println("1. Listar  2. Agregar  3. Ver butacas  4. Eliminar");
        System.out.println("5. Marcar butaca fuera de servicio  6. Reponer butaca");
        System.out.print("Opción: ");
        switch (leer()) {
            case "1" -> salas.listar().forEach(
                    s -> System.out.println(s + " - " + salas.capacidad(s.getId()) + " butacas"));
            case "2" -> {
                System.out.print("Nombre: ");
                String nombre = leer();
                TipoSala tipo = elegir("Tipo de sala", TipoSala.values());
                System.out.print("Butacas de cada fila, separadas por coma (ej. 8,10,12): ");
                List<Integer> distribucion = new ArrayList<>();
                for (String parte : leer().split(",")) {
                    distribucion.add(Integer.parseInt(parte.trim()));
                }
                salas.agregar(nombre, tipo, distribucion, pedirButacasEspeciales());
                System.out.println("Sala agregada");
            }
            case "3" -> mostrarButacas(leerEntero("Id de sala: "));
            case "4" -> {
                salas.eliminar(leerEntero("Id: "));
                System.out.println("Sala eliminada");
            }
            case "5" -> {
                int salaId = leerEntero("Id de sala: ");
                System.out.print("Butaca (ej. C7): ");
                salas.marcarFueraDeServicio(salaId, leer());
                System.out.println("Butaca fuera de servicio");
            }
            case "6" -> {
                int salaId = leerEntero("Id de sala: ");
                System.out.print("Butaca (ej. C7): ");
                salas.reponer(salaId, leer());
                System.out.println("Butaca repuesta");
            }
            default -> System.out.println("Opción inválida");
        }
    }

    /** Las butacas que no son estándar. Dejarlo vacío deja toda la sala estándar. */
    private Map<String, TipoAsiento> pedirButacasEspeciales() {
        System.out.print("Butacas especiales (ej. A1:VIP,B2:ACCESIBLE), vacío si no hay: ");
        String respuesta = leer();
        Map<String, TipoAsiento> especiales = new LinkedHashMap<>();
        if (respuesta.isBlank()) {
            return especiales;
        }
        for (String parte : respuesta.split(",")) {
            String[] campos = parte.trim().split(":");
            if (campos.length != 2) {
                throw new IllegalArgumentException("Formato esperado: A1:VIP,B2:ACCESIBLE");
            }
            especiales.put(campos[0].trim().toUpperCase(),
                    TipoAsiento.valueOf(campos[1].trim().toUpperCase()));
        }
        return especiales;
    }

    /** Dibuja la sala vacía, fila por fila. */
    private void mostrarButacas(int salaId) {
        salas.buscar(salaId).orElseThrow(() -> new IllegalArgumentException("No existe la sala " + salaId));
        imprimirMapa(salas.asientosDe(salaId), List.of());
    }

    // ---------- funciones ----------

    private void menuFunciones() {
        System.out.println("\n-- Funciones --");
        System.out.println("1. Listar  2. Programar  3. Ver por película  4. Eliminar");
        System.out.print("Opción: ");
        switch (leer()) {
            case "1" -> imprimir(funciones.listar());
            case "2" -> {
                imprimir(cartelera.listar());
                int peliculaId = leerEntero("Id de película: ");
                imprimir(salas.listar());
                int salaId = leerEntero("Id de sala: ");
                LocalDateTime inicio = leerFecha();
                Version version = elegir("Versión", Version.values());
                Proyeccion proyeccion = elegir("Proyección", Proyeccion.values());
                double precio = leerDecimal("Precio base de la butaca estándar: ");
                funciones.programar(peliculaId, salaId, inicio, version, proyeccion, precio);
                System.out.println("Función programada");
            }
            case "3" -> imprimir(funciones.listarPorPelicula(leerEntero("Id de película: ")));
            case "4" -> {
                funciones.eliminar(leerEntero("Id: "));
                System.out.println("Función eliminada");
            }
            default -> System.out.println("Opción inválida");
        }
    }

    // ---------- clientes ----------

    private void menuClientes() {
        System.out.println("\n-- Clientes --");
        System.out.println("1. Listar  2. Registrar  3. Eliminar");
        System.out.print("Opción: ");
        switch (leer()) {
            case "1" -> imprimir(clientes.listar());
            case "2" -> {
                System.out.print("Nombre: ");
                String nombre = leer();
                System.out.print("Email: ");
                clientes.registrar(nombre, leer());
                System.out.println("Cliente registrado");
            }
            case "3" -> {
                clientes.eliminar(leerEntero("Id: "));
                System.out.println("Cliente eliminado");
            }
            default -> System.out.println("Opción inválida");
        }
    }

    // ---------- reservas ----------

    private void menuReservas() {
        System.out.println("\n-- Reservas --");
        System.out.println("1. Listar  2. Reservar  3. Cobrar  4. Cancelar  5. Ver por cliente");
        System.out.println("6. Mapa de butacas  7. Ver pago de una reserva");
        System.out.print("Opción: ");
        switch (leer()) {
            case "1" -> imprimir(reservas.listar());
            case "2" -> {
                imprimir(funciones.listar());
                int funcionId = leerEntero("Id de función: ");
                imprimir(clientes.listar());
                int clienteId = leerEntero("Id de cliente: ");
                mostrarMapaDeFuncion(funcionId);
                System.out.println("Tarifas: " + Arrays.toString(TipoTarifa.values()) + ", GENERAL si no se aclara");
                System.out.print("Butacas separadas por coma (ej. B4,B5:JUBILADO): ");
                Reserva reserva = reservas.reservar(funcionId, clienteId, leerButacas());
                System.out.printf("Reserva confirmada. Total: $ %.2f%n", reserva.getTotal());
                System.out.println("Ticket en tickets/ticket-" + reserva.getId() + ".txt");
            }
            case "3" -> {
                int reservaId = leerEntero("Id de reserva: ");
                MedioPago medio = elegir("Medio de pago", MedioPago.values());
                Pago pago = pagos.cobrar(reservaId, medio, pedirAutorizacion(medio));
                System.out.printf("Cobrado $ %.2f con %s%n", pago.getMonto(), pago.getMedio());
            }
            case "4" -> {
                reservas.cancelar(leerEntero("Id de reserva: "));
                System.out.println("Reserva cancelada");
            }
            case "5" -> imprimir(reservas.listarPorCliente(leerEntero("Id de cliente: ")));
            case "6" -> mostrarMapaDeFuncion(leerEntero("Id de función: "));
            case "7" -> {
                int reservaId = leerEntero("Id de reserva: ");
                pagos.buscarPorReserva(reservaId).ifPresentOrElse(
                        System.out::println,
                        () -> System.out.println("La reserva " + reservaId + " todavía no se cobró"));
            }
            default -> System.out.println("Opción inválida");
        }
    }

    // ---------- candy ----------

    private void menuCandy() {
        System.out.println("\n-- Candy --");
        System.out.println("1. Ver carta  2. Agregar producto  3. Armar combo  4. Vender");
        System.out.println("5. Sacar de la carta  6. Reponer  7. Ver compras de un cliente");
        System.out.print("Opción: ");
        switch (leer()) {
            case "1" -> imprimir(candy.listar());
            case "2" -> {
                System.out.print("Nombre: ");
                String nombre = leer();
                TipoProducto tipo = elegir("Tipo", TipoProducto.POCHOCLOS, TipoProducto.BEBIDA,
                        TipoProducto.GOLOSINA);
                candy.agregarProducto(nombre, tipo, leerDecimal("Precio: "));
                System.out.println("Producto agregado");
            }
            case "3" -> {
                imprimir(candy.listar());
                System.out.print("Nombre del combo: ");
                String nombre = leer();
                Map<Integer, Integer> componentes = pedirCantidades("Qué trae (id:cantidad, ej. 1:1,2:1): ");
                candy.armarCombo(nombre, leerDecimal("Precio promocional: "), componentes);
                System.out.println("Combo armado");
            }
            case "4" -> venderEnElCandy();
            case "5" -> {
                candy.cambiarDisponibilidad(leerEntero("Id de producto: "), false);
                System.out.println("Producto fuera de la carta");
            }
            case "6" -> {
                candy.cambiarDisponibilidad(leerEntero("Id de producto: "), true);
                System.out.println("Producto de nuevo en la carta");
            }
            case "7" -> imprimir(candy.listarComprasDe(leerEntero("Id de cliente: ")));
            default -> System.out.println("Opción inválida");
        }
    }

    private void venderEnElCandy() {
        imprimir(candy.listarDisponibles());
        imprimir(clientes.listar());
        int clienteId = leerEntero("Id de cliente: ");
        Map<Integer, Integer> pedido = pedirCantidades("Productos (id:cantidad, ej. 1:2,3:1): ");
        MedioPago medio = elegir("Medio de pago", MedioPago.values());

        CompraCandy compra = candy.vender(clienteId, pedido, medio, pedirAutorizacion(medio));
        System.out.printf("Compra registrada. Total: $ %.2f%n", compra.getTotal());
        double ahorro = candy.ahorroDe(compra);
        if (ahorro > 0) {
            System.out.printf("Se ahorró $ %.2f con los combos%n", ahorro);
        }
        System.out.println("Ticket en tickets/candy-" + compra.getId() + ".txt");
    }

    /** Cantidades por id: "1:2,3:1" son dos unidades del producto 1 y una del 3. */
    private Map<Integer, Integer> pedirCantidades(String etiqueta) {
        System.out.print(etiqueta);
        Map<Integer, Integer> cantidades = new LinkedHashMap<>();
        for (String parte : leer().split(",")) {
            String[] campos = parte.trim().split(":");
            if (campos.length != 2) {
                throw new IllegalArgumentException("Formato esperado: id:cantidad, por ejemplo 1:2,3:1");
            }
            cantidades.put(Integer.parseInt(campos[0].trim()), Integer.parseInt(campos[1].trim()));
        }
        return cantidades;
    }

    // ---------- arqueo ----------

    /** Las dos cajas del cine son distintas: la boletería cobra reservas y el candy vende. */
    private void arqueoDelDia() {
        LocalDate dia = LocalDate.now();
        System.out.println("\n-- Boletería --");
        imprimir(pagos.listarDelDia(dia));
        System.out.println("\n-- Candy --");
        imprimir(candy.listarComprasDelDia(dia));

        double boleteria = pagos.totalCobrado(dia);
        double barra = candy.totalVendido(dia);
        System.out.printf("%nBoletería : $ %10.2f%n", boleteria);
        System.out.printf("Candy     : $ %10.2f%n", barra);
        System.out.printf("Total del %s: $ %10.2f%n", dia, boleteria + barra);
    }

    // ---------- empleados ----------

    private void menuEmpleados() {
        System.out.println("\n-- Empleados --");
        System.out.println("1. Listar  2. Registrar  3. Iniciar sesión  4. Validar entrada en la puerta");
        System.out.print("Opción: ");
        switch (leer()) {
            case "1" -> imprimir(empleados.listar());
            case "2" -> {
                System.out.print("Nombre: ");
                String nombre = leer();
                System.out.print("Email: ");
                String email = leer();
                System.out.print("Contraseña (mínimo 6): ");
                String password = leer();
                empleados.registrar(nombre, email, password,
                        elegir("Rol", new Rol[] {Rol.ADMINISTRADOR, Rol.ACOMODADOR}));
                System.out.println("Empleado registrado");
            }
            case "3" -> {
                System.out.print("Email: ");
                String email = leer();
                System.out.print("Contraseña: ");
                Empleado empleado = empleados.iniciarSesion(email, leer());
                System.out.println("Bienvenido, " + empleado.getNombre()
                        + " (" + empleado.getRol().name().toLowerCase() + ")");
            }
            case "4" -> validarEntrada();
            default -> System.out.println("Opción inválida");
        }
    }

    /**
     * CU-18: lo que hace el acomodador en la puerta. El código es el del QR del ticket;
     * si el escáner no lee, se tipea, y por eso el alfabeto del código no tiene O ni 0.
     */
    private void validarEntrada() {
        System.out.print("Código de la reserva: ");
        Reserva reserva = reservas.registrarIngreso(leer());

        System.out.println("ENTRADA VÁLIDA - " + reserva.getCantidadEntradas() + " butaca(s)");
        funciones.buscar(reserva.getFuncionId()).ifPresent(f -> {
            cartelera.buscar(f.getPeliculaId())
                    .ifPresent(p -> System.out.println("  " + p.getTitulo()));
            salas.buscar(f.getSalaId())
                    .ifPresent(s -> System.out.println("  " + s.getNombre()
                            + " - " + f.getInicio().format(FORMATO_FECHA)));
        });
        for (Entrada entrada : reserva.getEntradas()) {
            String acreditar = entrada.tarifa().requiereAcreditacion() ? "  <-- PEDIR CARNET" : "";
            System.out.println("  " + entrada.codigoAsiento() + "  " + entrada.tarifa() + acreditar);
        }
    }

    // ---------- entrada y salida ----------

    /**
     * Butacas separadas por coma, con la tarifa opcional detrás de ":". En
     * <code>B4,B5:JUBILADO</code> van dos butacas y solo la segunda paga reducida.
     * Pedir la tarifa de a una por pantalla era insufrible para una reserva de cuatro.
     */
    private Map<String, TipoTarifa> leerButacas() {
        Map<String, TipoTarifa> butacas = new LinkedHashMap<>();
        for (String parte : leer().split(",")) {
            String[] campos = parte.trim().split(":");
            TipoTarifa tarifa = campos.length > 1
                    ? TipoTarifa.valueOf(campos[1].trim().toUpperCase())
                    : TipoTarifa.GENERAL;
            butacas.put(campos[0].trim().toUpperCase(), tarifa);
        }
        return butacas;
    }

    private String leer() {
        return scanner.nextLine().trim();
    }

    private int leerEntero(String etiqueta) {
        System.out.print(etiqueta);
        return Integer.parseInt(leer());
    }

    private double leerDecimal(String etiqueta) {
        System.out.print(etiqueta);
        return Double.parseDouble(leer());
    }

    private LocalDateTime leerFecha() {
        System.out.print("Fecha y hora (dd/MM/yyyy HH:mm): ");
        try {
            return LocalDateTime.parse(leer(), FORMATO_FECHA);
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("Formato de fecha inválido. Ejemplo: 25/12/2026 20:30");
        }
    }

    private String pedirAutorizacion(MedioPago medio) {
        if (!medio.requiereAutorizacion()) {
            return "";
        }
        System.out.print("Código de autorización: ");
        return leer();
    }

    /** Muestra las opciones numeradas y devuelve la elegida. Sirve para cualquier enum. */
    @SafeVarargs
    private <T> T elegir(String que, T... opciones) {
        listarOpciones(opciones);
        return porIndice(opciones, leerEntero(que + ": "), que);
    }

    private <T> void listarOpciones(T[] opciones) {
        for (int i = 0; i < opciones.length; i++) {
            System.out.println("  " + (i + 1) + ". " + opciones[i]);
        }
    }

    private <T> T porIndice(T[] opciones, int numero, String que) {
        if (numero < 1 || numero > opciones.length) {
            throw new IllegalArgumentException(que + " inexistente: " + numero);
        }
        return opciones[numero - 1];
    }

    /** Muestra las butacas de la función marcando las tomadas. */
    private void mostrarMapaDeFuncion(int funcionId) {
        Funcion funcion = funciones.buscar(funcionId)
                .orElseThrow(() -> new IllegalArgumentException("No existe la función " + funcionId));
        List<Asiento> todos = salas.asientosDe(funcion.getSalaId());
        List<Asiento> libres = reservas.asientosLibres(funcionId);
        imprimirMapa(todos, libres);
        System.out.println("Libres: " + libres.size() + " de " + todos.size());
    }

    /**
     * Una línea por fila. libres vacío = sala sin función, todas disponibles.
     * [B4] libre, (B4) ocupada, y el tipo se marca con un símbolo.
     */
    private void imprimirMapa(List<Asiento> asientos, List<Asiento> libres) {
        List<Integer> idsLibres = libres.stream().map(Asiento::getId).toList();
        boolean sinFuncion = libres.isEmpty();
        System.out.println("        ---------- PANTALLA ----------");
        int filaActual = -1;
        StringBuilder linea = new StringBuilder();
        for (Asiento asiento : asientos) {
            if (asiento.getFila() != filaActual) {
                if (filaActual != -1) {
                    System.out.println(linea);
                }
                filaActual = asiento.getFila();
                linea = new StringBuilder("  " + asiento.getCodigo().charAt(0) + " ");
            }
            String marca = switch (asiento.getTipo()) {
                case VIP -> "*";
                case PAREJA -> "&";
                case ACCESIBLE -> "+";
                case ESTANDAR -> "";
            };
            if (asiento.getEstado() == EstadoAsiento.FUERA_DE_SERVICIO) {
                linea.append("{").append(asiento.getNumero()).append(marca).append("} ");
            } else {
                boolean libre = sinFuncion || idsLibres.contains(asiento.getId());
                linea.append(libre ? "[" : "(").append(asiento.getNumero()).append(marca).append(libre ? "] " : ") ");
            }
        }
        if (filaActual != -1) {
            System.out.println(linea);
        }
        System.out.println("  [n] libre  (n) ocupada  {n} fuera de servicio  * VIP  & pareja  + accesible");
    }

    private void imprimir(List<?> elementos) {
        if (elementos.isEmpty()) {
            System.out.println("(no hay nada cargado)");
            return;
        }
        elementos.forEach(System.out::println);
    }
}
