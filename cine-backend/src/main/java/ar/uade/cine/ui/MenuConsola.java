package ar.uade.cine.ui;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

import ar.uade.cine.interfaces.Asiento;
import ar.uade.cine.interfaces.Funcion;
import ar.uade.cine.interfaces.Reserva;
import ar.uade.cine.modelo.EstadoAsiento;
import ar.uade.cine.modelo.Genero;
import ar.uade.cine.modelo.Idioma;
import ar.uade.cine.modelo.Proyeccion;
import ar.uade.cine.modelo.TipoSala;
import ar.uade.cine.servicio.GestorCartelera;
import ar.uade.cine.servicio.GestorClientes;
import ar.uade.cine.servicio.GestorFunciones;
import ar.uade.cine.servicio.GestorReservas;
import ar.uade.cine.servicio.GestorSalas;
import ar.uade.cine.servicio.SalasDeEjemplo;

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
    private final Scanner scanner = new Scanner(System.in);

    public MenuConsola(GestorCartelera cartelera, GestorSalas salas, GestorFunciones funciones,
                       GestorClientes clientes, GestorReservas reservas) {
        this.cartelera = cartelera;
        this.salas = salas;
        this.funciones = funciones;
        this.clientes = clientes;
        this.reservas = reservas;
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
            System.out.println("0. Salir");
            System.out.print("Opción: ");

            try {
                switch (leer()) {
                    case "1" -> menuPeliculas();
                    case "2" -> menuSalas();
                    case "3" -> menuFunciones();
                    case "4" -> menuClientes();
                    case "5" -> menuReservas();
                    case "0" -> salir = true;
                    default -> System.out.println("Opción inválida");
                }
            } catch (NumberFormatException e) {
                System.out.println("Se esperaba un número");
            } catch (IllegalArgumentException e) {
                System.out.println(e.getMessage());
            }
        }
    }

    // ---------- películas ----------

    private void menuPeliculas() {
        System.out.println("\n-- Películas --");
        System.out.println("1. Listar  2. Agregar  3. Buscar por id  4. Eliminar  5. Filtrar por género");
        System.out.print("Opción: ");
        switch (leer()) {
            case "1" -> imprimir(cartelera.listar());
            case "2" -> {
                System.out.print("Título: ");
                String titulo = leer();
                int duracion = leerEntero("Duración en minutos: ");
                cartelera.agregar(titulo, duracion, pedirGeneros());
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
            case "5" -> imprimir(cartelera.listarPorGenero(elegirGenero()));
            default -> System.out.println("Opción inválida");
        }
    }

    /** Muestra el enum numerado y acepta varios separados por coma: "1,3". */
    private List<Genero> pedirGeneros() {
        Genero[] opciones = Genero.values();
        for (int i = 0; i < opciones.length; i++) {
            System.out.println("  " + (i + 1) + ". " + opciones[i]);
        }
        System.out.print("Géneros (números separados por coma): ");

        List<Genero> elegidos = new ArrayList<>();
        for (String parte : leer().split(",")) {
            elegidos.add(porIndice(opciones, Integer.parseInt(parte.trim())));
        }
        return elegidos;
    }

    private Genero elegirGenero() {
        Genero[] opciones = Genero.values();
        for (int i = 0; i < opciones.length; i++) {
            System.out.println("  " + (i + 1) + ". " + opciones[i]);
        }
        return porIndice(opciones, leerEntero("Género: "));
    }

    private Genero porIndice(Genero[] opciones, int numero) {
        if (numero < 1 || numero > opciones.length) {
            throw new IllegalArgumentException("Género inexistente: " + numero);
        }
        return opciones[numero - 1];
    }

    // ---------- salas ----------

    private void menuSalas() {
        System.out.println("\n-- Salas --");
        System.out.println("1. Listar  2. Agregar  3. Ver butacas  4. Eliminar  5. Cargar las 6 salas de ejemplo");
        System.out.println("6. Marcar butaca fuera de servicio  7. Reponer butaca");
        System.out.print("Opción: ");
        switch (leer()) {
            case "1" -> imprimir(salas.listar());
            case "2" -> {
                System.out.print("Nombre: ");
                String nombre = leer();
                TipoSala tipo = elegirTipoSala();
                System.out.print("Butacas de cada fila, separadas por coma (ej. 8,10,12): ");
                List<Integer> distribucion = new ArrayList<>();
                for (String parte : leer().split(",")) {
                    distribucion.add(Integer.parseInt(parte.trim()));
                }
                salas.agregar(nombre, tipo, distribucion);
                System.out.println("Sala agregada");
            }
            case "3" -> mostrarButacas(leerEntero("Id de sala: "));
            case "4" -> {
                salas.eliminar(leerEntero("Id: "));
                System.out.println("Sala eliminada");
            }
            case "5" -> {
                new SalasDeEjemplo(salas).cargar();
                System.out.println("6 salas cargadas");
            }
            case "6" -> {
                int salaId = leerEntero("Id de sala: ");
                System.out.print("Butaca (ej. C7): ");
                salas.marcarFueraDeServicio(salaId, leer());
                System.out.println("Butaca fuera de servicio");
            }
            case "7" -> {
                int salaId = leerEntero("Id de sala: ");
                System.out.print("Butaca (ej. C7): ");
                salas.reponer(salaId, leer());
                System.out.println("Butaca repuesta");
            }
            default -> System.out.println("Opción inválida");
        }
    }

    private Idioma elegirIdioma() {
        System.out.println("  1. DOBLADA  2. SUBTITULADA");
        int numero = leerEntero("Idioma: ");
        if (numero < 1 || numero > Idioma.values().length) {
            throw new IllegalArgumentException("Idioma inexistente: " + numero);
        }
        return Idioma.values()[numero - 1];
    }

    private Proyeccion elegirProyeccion() {
        System.out.println("  1. 2D  2. 3D");
        int numero = leerEntero("Proyección: ");
        if (numero < 1 || numero > Proyeccion.values().length) {
            throw new IllegalArgumentException("Proyección inexistente: " + numero);
        }
        return Proyeccion.values()[numero - 1];
    }

    private TipoSala elegirTipoSala() {
        TipoSala[] opciones = TipoSala.values();
        for (int i = 0; i < opciones.length; i++) {
            System.out.println("  " + (i + 1) + ". " + opciones[i]);
        }
        int numero = leerEntero("Tipo de sala: ");
        if (numero < 1 || numero > opciones.length) {
            throw new IllegalArgumentException("Tipo inexistente: " + numero);
        }
        return opciones[numero - 1];
    }

    /** Dibuja la sala vacía, fila por fila. */
    private void mostrarButacas(int salaId) {
        salas.buscar(salaId).orElseThrow(() -> new IllegalArgumentException("No existe la sala " + salaId));
        List<Asiento> asientos = salas.asientosDe(salaId);
        imprimirMapa(asientos, List.of());
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
                Idioma idioma = elegirIdioma();
                Proyeccion proyeccion = elegirProyeccion();
                double precio = leerDecimal("Precio base de la butaca estándar: ");
                funciones.programar(peliculaId, salaId, inicio, idioma, proyeccion, precio);
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
        System.out.println("1. Listar  2. Reservar  3. Pagar  4. Cancelar  5. Ver por cliente  6. Mapa de butacas");
        System.out.print("Opción: ");
        switch (leer()) {
            case "1" -> imprimir(reservas.listar());
            case "2" -> {
                imprimir(funciones.listar());
                int funcionId = leerEntero("Id de función: ");
                imprimir(clientes.listar());
                int clienteId = leerEntero("Id de cliente: ");
                mostrarMapaDeFuncion(funcionId);
                System.out.print("Butacas separadas por coma (ej. B4,B5): ");
                List<String> codigos = List.of(leer().split(","));
                Reserva reserva = reservas.reservar(funcionId, clienteId, codigos);
                System.out.printf("Reserva confirmada. Total: $ %.2f%n", reserva.getTotal());
                System.out.println("Ticket en tickets/ticket-" + reserva.getId() + ".txt");
            }
            case "3" -> {
                reservas.pagar(leerEntero("Id de reserva: "));
                System.out.println("Reserva pagada");
            }
            case "4" -> {
                reservas.cancelar(leerEntero("Id de reserva: "));
                System.out.println("Reserva cancelada");
            }
            case "5" -> imprimir(reservas.listarPorCliente(leerEntero("Id de cliente: ")));
            case "6" -> mostrarMapaDeFuncion(leerEntero("Id de función: "));
            default -> System.out.println("Opción inválida");
        }
    }

    // ---------- entrada y salida ----------

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
     * Una línea por fila. libresConocidos vacío = sala sin función, todas disponibles.
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
