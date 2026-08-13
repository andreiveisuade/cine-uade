package ar.uade.cine.ui;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import ar.uade.cine.dominio.salas.TipoAsiento;
import ar.uade.cine.dominio.salas.TipoSala;
import ar.uade.cine.servicio.GestorSalas;

/** Las salas y el estado de sus butacas, por consola. */
class MenuSalas implements Menu {

    private final Consola consola;
    private final GestorSalas salas;
    private final MapaDeButacas mapa;

    MenuSalas(Consola consola, GestorSalas salas, MapaDeButacas mapa) {
        this.consola = consola;
        this.salas = salas;
        this.mapa = mapa;
    }

    @Override
    public String titulo() {
        return "Salas";
    }

    @Override
    public void mostrar() {
        consola.mostrar("\n-- Salas --");
        consola.mostrar("1. Listar  2. Agregar  3. Ver butacas  4. Eliminar");
        consola.mostrar("5. Marcar butaca fuera de servicio  6. Reponer butaca");
        consola.pedir("Opción: ");
        switch (consola.leer()) {
            case "1" -> salas.listar().forEach(
                    s -> consola.mostrar(s + " - " + salas.capacidad(s.getId()) + " butacas"));
            case "2" -> agregar();
            case "3" -> mapa.deSala(consola.leerEntero("Id de sala: "));
            case "4" -> {
                salas.eliminar(consola.leerEntero("Id: "));
                consola.mostrar("Sala eliminada");
            }
            case "5" -> {
                int salaId = consola.leerEntero("Id de sala: ");
                salas.marcarFueraDeServicio(salaId, consola.leerTexto("Butaca (ej. C7): "));
                consola.mostrar("Butaca fuera de servicio");
            }
            case "6" -> {
                int salaId = consola.leerEntero("Id de sala: ");
                salas.reponer(salaId, consola.leerTexto("Butaca (ej. C7): "));
                consola.mostrar("Butaca repuesta");
            }
            default -> consola.mostrar("Opción inválida");
        }
    }

    private void agregar() {
        String nombre = consola.leerTexto("Nombre: ");
        TipoSala tipo = consola.elegir("Tipo de sala", TipoSala.values());
        consola.pedir("Butacas de cada fila, separadas por coma (ej. 8,10,12): ");
        List<Integer> distribucion = new ArrayList<>();
        for (String parte : consola.leer().split(",")) {
            distribucion.add(Integer.parseInt(parte.trim()));
        }
        salas.agregar(nombre, tipo, distribucion, pedirButacasEspeciales());
        consola.mostrar("Sala agregada");
    }

    /** Las butacas que no son estándar. Dejarlo vacío deja toda la sala estándar. */
    private Map<String, TipoAsiento> pedirButacasEspeciales() {
        consola.pedir("Butacas especiales (ej. A1:VIP,B2:ACCESIBLE), vacío si no hay: ");
        String respuesta = consola.leer();
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
}
