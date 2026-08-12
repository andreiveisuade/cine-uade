package ar.uade.cine.ui;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

import ar.uade.cine.interfaces.Reserva;
import ar.uade.cine.modelo.Genero;
import ar.uade.cine.servicio.GestorCartelera;
import ar.uade.cine.servicio.GestorClientes;
import ar.uade.cine.servicio.GestorFunciones;
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
        System.out.println("1. Listar  2. Agregar  3. Eliminar");
        System.out.print("Opción: ");
        switch (leer()) {
            case "1" -> imprimir(salas.listar());
            case "2" -> {
                System.out.print("Nombre: ");
                String nombre = leer();
                salas.agregar(nombre, leerEntero("Capacidad: "));
                System.out.println("Sala agregada");
            }
            case "3" -> {
                salas.eliminar(leerEntero("Id: "));
                System.out.println("Sala eliminada");
            }
            default -> System.out.println("Opción inválida");
        }
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
                double precio = leerDecimal("Precio de la entrada: ");
                funciones.programar(peliculaId, salaId, inicio, precio);
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
        System.out.println("1. Listar  2. Reservar  3. Pagar  4. Cancelar  5. Ver por cliente  6. Lugares libres");
        System.out.print("Opción: ");
        switch (leer()) {
            case "1" -> imprimir(reservas.listar());
            case "2" -> {
                imprimir(funciones.listar());
                int funcionId = leerEntero("Id de función: ");
                imprimir(clientes.listar());
                int clienteId = leerEntero("Id de cliente: ");
                int cantidad = leerEntero("Cantidad de entradas: ");
                Reserva reserva = reservas.reservar(funcionId, clienteId, cantidad);
                System.out.println("Reserva confirmada. Ticket en tickets/ticket-" + reserva.getId() + ".txt");
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
            case "6" -> System.out.println("Lugares libres: " + reservas.lugaresLibres(leerEntero("Id de función: ")));
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

    private void imprimir(List<?> elementos) {
        if (elementos.isEmpty()) {
            System.out.println("(no hay nada cargado)");
            return;
        }
        elementos.forEach(System.out::println);
    }
}
