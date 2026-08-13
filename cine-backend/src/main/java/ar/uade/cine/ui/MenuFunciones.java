package ar.uade.cine.ui;

import java.time.LocalDateTime;

import ar.uade.cine.dominio.funciones.Proyeccion;
import ar.uade.cine.dominio.funciones.Version;
import ar.uade.cine.servicio.GestorCartelera;
import ar.uade.cine.servicio.GestorFunciones;
import ar.uade.cine.servicio.GestorSalas;

/**
 * La grilla de funciones por consola. Necesita la cartelera y las salas además del gestor
 * de funciones porque programar una función es elegir una película y una sala: se listan
 * antes de pedir el id, para no tener que adivinarlo.
 */
class MenuFunciones implements Menu {

    private final Consola consola;
    private final GestorFunciones funciones;
    private final GestorCartelera cartelera;
    private final GestorSalas salas;

    MenuFunciones(Consola consola, GestorFunciones funciones, GestorCartelera cartelera,
                  GestorSalas salas) {
        this.consola = consola;
        this.funciones = funciones;
        this.cartelera = cartelera;
        this.salas = salas;
    }

    @Override
    public String titulo() {
        return "Funciones";
    }

    @Override
    public void mostrar() {
        consola.mostrar("\n-- Funciones --");
        consola.mostrar("1. Listar  2. Programar  3. Ver por película  4. Eliminar");
        consola.pedir("Opción: ");
        switch (consola.leer()) {
            case "1" -> consola.imprimir(funciones.listar());
            case "2" -> programar();
            case "3" -> consola.imprimir(
                    funciones.listarPorPelicula(consola.leerEntero("Id de película: ")));
            case "4" -> {
                funciones.eliminar(consola.leerEntero("Id: "));
                consola.mostrar("Función eliminada");
            }
            default -> consola.mostrar("Opción inválida");
        }
    }

    private void programar() {
        consola.imprimir(cartelera.listar());
        int peliculaId = consola.leerEntero("Id de película: ");
        consola.imprimir(salas.listar());
        int salaId = consola.leerEntero("Id de sala: ");
        LocalDateTime inicio = consola.leerFecha();
        Version version = consola.elegir("Versión", Version.values());
        Proyeccion proyeccion = consola.elegir("Proyección", Proyeccion.values());
        double precio = consola.leerDecimal("Precio base de la butaca estándar: ");

        funciones.programar(peliculaId, salaId, inicio, version, proyeccion, precio);
        consola.mostrar("Función programada");
    }
}
