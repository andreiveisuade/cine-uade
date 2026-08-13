package ar.uade.cine.ui;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;

import ar.uade.cine.dominio.ventas.MedioPago;
import ar.uade.cine.dominio.ventas.Pago;
import ar.uade.cine.dominio.ventas.Reserva;
import ar.uade.cine.dominio.ventas.TipoTarifa;
import ar.uade.cine.servicio.GestorClientes;
import ar.uade.cine.servicio.GestorFunciones;
import ar.uade.cine.servicio.GestorPagos;
import ar.uade.cine.servicio.GestorReservas;

/**
 * Reservar butacas y cobrarlas, por consola. Reservar y cobrar son dos gestores distintos
 * —son dos circuitos del negocio— pero para quien atiende el mostrador es un solo menú.
 */
class MenuReservas implements Menu {

    private final Consola consola;
    private final GestorReservas reservas;
    private final GestorPagos pagos;
    private final GestorFunciones funciones;
    private final GestorClientes clientes;
    private final MapaDeButacas mapa;

    MenuReservas(Consola consola, GestorReservas reservas, GestorPagos pagos,
                 GestorFunciones funciones, GestorClientes clientes, MapaDeButacas mapa) {
        this.consola = consola;
        this.reservas = reservas;
        this.pagos = pagos;
        this.funciones = funciones;
        this.clientes = clientes;
        this.mapa = mapa;
    }

    @Override
    public String titulo() {
        return "Reservas";
    }

    @Override
    public void mostrar() {
        consola.mostrar("\n-- Reservas --");
        consola.mostrar("1. Listar  2. Reservar  3. Cobrar  4. Cancelar  5. Ver por cliente");
        consola.mostrar("6. Mapa de butacas  7. Ver pago de una reserva");
        consola.pedir("Opción: ");
        switch (consola.leer()) {
            case "1" -> consola.imprimir(reservas.listar());
            case "2" -> reservar();
            case "3" -> cobrar();
            case "4" -> {
                reservas.cancelar(consola.leerEntero("Id de reserva: "));
                consola.mostrar("Reserva cancelada");
            }
            case "5" -> consola.imprimir(
                    reservas.listarPorCliente(consola.leerEntero("Id de cliente: ")));
            case "6" -> mapa.deFuncion(consola.leerEntero("Id de función: "));
            case "7" -> verPago();
            default -> consola.mostrar("Opción inválida");
        }
    }

    private void reservar() {
        consola.imprimir(funciones.listar());
        int funcionId = consola.leerEntero("Id de función: ");
        consola.imprimir(clientes.listar());
        int clienteId = consola.leerEntero("Id de cliente: ");

        mapa.deFuncion(funcionId);
        consola.mostrar("Tarifas: " + Arrays.toString(TipoTarifa.values())
                + ", GENERAL si no se aclara");
        consola.pedir("Butacas separadas por coma (ej. B4,B5:JUBILADO): ");

        Reserva reserva = reservas.reservar(funcionId, clienteId, leerButacas());
        consola.mostrar(String.format("Reserva confirmada. Total: $ %.2f", reserva.getTotal()));
        consola.mostrar("Ticket en tickets/ticket-" + reserva.getId() + ".txt");
    }

    private void cobrar() {
        int reservaId = consola.leerEntero("Id de reserva: ");
        MedioPago medio = consola.elegir("Medio de pago", MedioPago.values());
        Pago pago = pagos.cobrar(reservaId, medio, PedidoDeAutorizacion.pedir(consola, medio));
        consola.mostrar(String.format("Cobrado $ %.2f con %s", pago.getMonto(), pago.getMedio()));
    }

    private void verPago() {
        int reservaId = consola.leerEntero("Id de reserva: ");
        pagos.buscarPorReserva(reservaId).ifPresentOrElse(
                pago -> consola.mostrar(pago.toString()),
                () -> consola.mostrar("La reserva " + reservaId + " todavía no se cobró"));
    }

    /**
     * Butacas separadas por coma, con la tarifa opcional detrás de ":". En
     * <code>B4,B5:JUBILADO</code> van dos butacas y solo la segunda paga reducida.
     * Pedir la tarifa de a una por pantalla era insufrible para una reserva de cuatro.
     */
    private Map<String, TipoTarifa> leerButacas() {
        Map<String, TipoTarifa> butacas = new LinkedHashMap<>();
        for (String parte : consola.leer().split(",")) {
            String[] campos = parte.trim().split(":");
            TipoTarifa tarifa = campos.length > 1
                    ? TipoTarifa.valueOf(campos[1].trim().toUpperCase())
                    : TipoTarifa.GENERAL;
            butacas.put(campos[0].trim().toUpperCase(), tarifa);
        }
        return butacas;
    }
}
