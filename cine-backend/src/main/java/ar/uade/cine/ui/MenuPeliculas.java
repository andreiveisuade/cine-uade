package ar.uade.cine.ui;

import java.util.ArrayList;
import java.util.List;

import ar.uade.cine.dominio.cartelera.Clasificacion;
import ar.uade.cine.dominio.cartelera.Genero;
import ar.uade.cine.servicio.DatosPelicula;
import ar.uade.cine.servicio.GestorCartelera;

/** El catálogo de películas por consola. */
class MenuPeliculas implements Menu {

    private final Consola consola;
    private final GestorCartelera cartelera;

    MenuPeliculas(Consola consola, GestorCartelera cartelera) {
        this.consola = consola;
        this.cartelera = cartelera;
    }

    @Override
    public String titulo() {
        return "Películas";
    }

    @Override
    public void mostrar() {
        consola.mostrar("\n-- Películas --");
        consola.mostrar("1. Listar  2. Agregar  3. Buscar por id  4. Eliminar  5. Filtrar por género");
        consola.mostrar("6. Completar datos de catálogo  7. Ver solo cartelera");
        consola.pedir("Opción: ");
        switch (consola.leer()) {
            case "1" -> consola.imprimir(cartelera.listar());
            case "2" -> agregar();
            case "3" -> buscar();
            case "4" -> {
                cartelera.eliminar(consola.leerEntero("Id: "));
                consola.mostrar("Película eliminada");
            }
            case "5" -> consola.imprimir(
                    cartelera.listarPorGenero(consola.elegir("Género", Genero.values())));
            case "6" -> completarCatalogo();
            case "7" -> consola.imprimir(cartelera.listarEnCartelera());
            default -> consola.mostrar("Opción inválida");
        }
    }

    private void agregar() {
        String titulo = consola.leerTexto("Título: ");
        int duracion = consola.leerEntero("Duración en minutos: ");
        List<Genero> generos = pedirGeneros();
        cartelera.agregar(titulo, duracion, generos,
                consola.elegir("Clasificación", Clasificacion.values()));
        consola.mostrar("Película agregada");
    }

    private void buscar() {
        int id = consola.leerEntero("Id: ");
        cartelera.buscar(id).ifPresentOrElse(
                pelicula -> consola.mostrar(pelicula.toString()),
                () -> consola.mostrar("No existe la película " + id));
    }

    /** Los datos de catálogo son opcionales: se cargan aparte del alta. */
    private void completarCatalogo() {
        int id = consola.leerEntero("Id de película: ");
        String director = consola.leerTexto("Director: ");
        String sinopsis = consola.leerTexto("Sinopsis: ");
        int anio = consola.leerEntero("Año: ");
        String idiomaOriginal = consola.leerTexto("Idioma original: ");
        String posterUrl = consola.leerTexto("URL del poster: ");

        cartelera.editar(id, DatosPelicula.deCatalogo(director, sinopsis, anio, idiomaOriginal, posterUrl));
        consola.mostrar("Datos de catálogo guardados");
    }

    /** Una película puede tener varios géneros: se aceptan varios números separados por coma. */
    private List<Genero> pedirGeneros() {
        Genero[] opciones = Genero.values();
        consola.listarOpciones(opciones);
        consola.pedir("Géneros (números separados por coma): ");

        List<Genero> elegidos = new ArrayList<>();
        for (String parte : consola.leer().split(",")) {
            elegidos.add(consola.porIndice(opciones, Integer.parseInt(parte.trim()), "Género"));
        }
        return elegidos;
    }
}
