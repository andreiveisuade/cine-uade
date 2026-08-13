package ar.uade.cine.ui;

import ar.uade.cine.servicio.GestorClientes;

/** Alta y baja de clientes por consola. */
class MenuClientes implements Menu {

    private final Consola consola;
    private final GestorClientes clientes;

    MenuClientes(Consola consola, GestorClientes clientes) {
        this.consola = consola;
        this.clientes = clientes;
    }

    @Override
    public String titulo() {
        return "Clientes";
    }

    @Override
    public void mostrar() {
        consola.mostrar("\n-- Clientes --");
        consola.mostrar("1. Listar  2. Registrar  3. Eliminar");
        consola.pedir("Opción: ");
        switch (consola.leer()) {
            case "1" -> consola.imprimir(clientes.listar());
            case "2" -> {
                String nombre = consola.leerTexto("Nombre: ");
                clientes.registrar(nombre, consola.leerTexto("Email: "));
                consola.mostrar("Cliente registrado");
            }
            case "3" -> {
                clientes.eliminar(consola.leerEntero("Id: "));
                consola.mostrar("Cliente eliminado");
            }
            default -> consola.mostrar("Opción inválida");
        }
    }
}
