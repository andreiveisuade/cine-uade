package ar.uade.cine.ui;

import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

import ar.uade.cine.interfaces.Pelicula;
import ar.uade.cine.modelo.Genero;
import ar.uade.cine.servicio.GestorCartelera;

/**
 * Única capa que habla con el usuario: acá viven Scanner y System.out, y en ningún
 * otro lado. Si mañana esto fuera una API REST, se reemplaza solo esta clase.
 */
public class MenuConsola {

    private final GestorCartelera gestor;
    private final Scanner scanner = new Scanner(System.in);

    public MenuConsola(GestorCartelera gestor) {
        this.gestor = gestor;
    }

    public void iniciar() {
        boolean salir = false;
        while (!salir) {
            System.out.println("\n--- CINE ---");
            System.out.println("1. Listar películas");
            System.out.println("2. Agregar película");
            System.out.println("3. Buscar por id");
            System.out.println("4. Eliminar");
            System.out.println("0. Salir");
            System.out.print("Opción: ");

            switch (scanner.nextLine().trim()) {
                case "1" -> listar();
                case "2" -> agregar();
                case "3" -> buscar();
                case "4" -> eliminar();
                case "0" -> salir = true;
                default -> System.out.println("Opción inválida");
            }
        }
    }

    private void listar() {
        List<Pelicula> peliculas = gestor.listar();
        if (peliculas.isEmpty()) {
            System.out.println("No hay películas cargadas");
            return;
        }
        peliculas.forEach(System.out::println);
    }

    private void agregar() {
        System.out.print("Título: ");
        String titulo = scanner.nextLine();
        System.out.print("Duración en minutos: ");
        try {
            int duracion = Integer.parseInt(scanner.nextLine().trim());
            gestor.agregar(titulo, duracion, pedirGeneros());
            System.out.println("Película agregada");
        } catch (NumberFormatException e) {
            System.out.println("Se esperaba un número");
        } catch (IllegalArgumentException e) {
            System.out.println(e.getMessage());
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
        for (String parte : scanner.nextLine().split(",")) {
            int indice = Integer.parseInt(parte.trim()) - 1;
            if (indice < 0 || indice >= opciones.length) {
                throw new IllegalArgumentException("Género inexistente: " + (indice + 1));
            }
            elegidos.add(opciones[indice]);
        }
        return elegidos;
    }

    private void buscar() {
        System.out.print("Id: ");
        try {
            int id = Integer.parseInt(scanner.nextLine().trim());
            gestor.buscar(id).ifPresentOrElse(
                    System.out::println,
                    () -> System.out.println("No existe la película " + id));
        } catch (NumberFormatException e) {
            System.out.println("El id tiene que ser un número");
        }
    }

    private void eliminar() {
        System.out.print("Id: ");
        try {
            gestor.eliminar(Integer.parseInt(scanner.nextLine().trim()));
            System.out.println("Película eliminada");
        } catch (NumberFormatException e) {
            System.out.println("El id tiene que ser un número");
        } catch (IllegalArgumentException e) {
            System.out.println(e.getMessage());
        }
    }
}
