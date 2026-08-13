package ar.uade.cine.ui;

import java.util.LinkedHashMap;
import java.util.Map;

import ar.uade.cine.dominio.candy.CompraCandy;
import ar.uade.cine.dominio.candy.TipoProducto;
import ar.uade.cine.dominio.ventas.MedioPago;
import ar.uade.cine.servicio.GestorCandy;
import ar.uade.cine.servicio.GestorClientes;
import ar.uade.cine.servicio.GestorProductos;

/** La barra del cine por consola: carta, combos y ventas de mostrador. */
class MenuCandy implements Menu {

    private final Consola consola;
    private final GestorCandy candy;
    private final GestorProductos carta;
    private final GestorClientes clientes;

    MenuCandy(Consola consola, GestorCandy candy, GestorProductos carta, GestorClientes clientes) {
        this.consola = consola;
        this.candy = candy;
        this.carta = carta;
        this.clientes = clientes;
    }

    @Override
    public String titulo() {
        return "Candy";
    }

    @Override
    public void mostrar() {
        consola.mostrar("\n-- Candy --");
        consola.mostrar("1. Ver carta  2. Agregar producto  3. Armar combo  4. Vender");
        consola.mostrar("5. Sacar de la carta  6. Reponer  7. Ver compras de un cliente");
        consola.pedir("Opción: ");
        switch (consola.leer()) {
            case "1" -> consola.imprimir(carta.listar());
            case "2" -> agregarProducto();
            case "3" -> armarCombo();
            case "4" -> vender();
            case "5" -> {
                carta.cambiarDisponibilidad(consola.leerEntero("Id de producto: "), false);
                consola.mostrar("Producto fuera de la carta");
            }
            case "6" -> {
                carta.cambiarDisponibilidad(consola.leerEntero("Id de producto: "), true);
                consola.mostrar("Producto de nuevo en la carta");
            }
            case "7" -> consola.imprimir(
                    candy.listarComprasDe(consola.leerEntero("Id de cliente: ")));
            default -> consola.mostrar("Opción inválida");
        }
    }

    private void agregarProducto() {
        String nombre = consola.leerTexto("Nombre: ");
        TipoProducto tipo = consola.elegir("Tipo", TipoProducto.POCHOCLOS, TipoProducto.BEBIDA,
                TipoProducto.GOLOSINA);
        carta.agregar(nombre, tipo, consola.leerDecimal("Precio: "));
        consola.mostrar("Producto agregado");
    }

    private void armarCombo() {
        consola.imprimir(carta.listar());
        String nombre = consola.leerTexto("Nombre del combo: ");
        Map<Integer, Integer> componentes = pedirCantidades("Qué trae (id:cantidad, ej. 1:1,2:1): ");
        carta.armarCombo(nombre, consola.leerDecimal("Precio promocional: "), componentes);
        consola.mostrar("Combo armado");
    }

    private void vender() {
        consola.imprimir(carta.listarDisponibles());
        consola.imprimir(clientes.listar());
        int clienteId = consola.leerEntero("Id de cliente: ");
        Map<Integer, Integer> pedido = pedirCantidades("Productos (id:cantidad, ej. 1:2,3:1): ");
        MedioPago medio = consola.elegir("Medio de pago", MedioPago.values());

        CompraCandy compra = candy.vender(clienteId, pedido, medio,
                PedidoDeAutorizacion.pedir(consola, medio));
        consola.mostrar(String.format("Compra registrada. Total: $ %.2f", compra.getTotal()));

        double ahorro = carta.ahorroDe(compra);
        if (ahorro > 0) {
            consola.mostrar(String.format("Se ahorró $ %.2f con los combos", ahorro));
        }
        consola.mostrar("Ticket en tickets/candy-" + compra.getId() + ".txt");
    }

    /** Cantidades por id: "1:2,3:1" son dos unidades del producto 1 y una del 3. */
    private Map<Integer, Integer> pedirCantidades(String etiqueta) {
        consola.pedir(etiqueta);
        Map<Integer, Integer> cantidades = new LinkedHashMap<>();
        for (String parte : consola.leer().split(",")) {
            String[] campos = parte.trim().split(":");
            if (campos.length != 2) {
                throw new IllegalArgumentException("Formato esperado: id:cantidad, por ejemplo 1:2,3:1");
            }
            cantidades.put(Integer.parseInt(campos[0].trim()), Integer.parseInt(campos[1].trim()));
        }
        return cantidades;
    }
}
