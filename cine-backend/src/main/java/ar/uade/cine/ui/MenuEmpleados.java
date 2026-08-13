package ar.uade.cine.ui;

import ar.uade.cine.dominio.usuarios.Empleado;
import ar.uade.cine.dominio.usuarios.Rol;
import ar.uade.cine.dominio.ventas.Entrada;
import ar.uade.cine.dominio.ventas.Reserva;
import ar.uade.cine.servicio.GestorCartelera;
import ar.uade.cine.servicio.GestorEmpleados;
import ar.uade.cine.servicio.GestorFunciones;
import ar.uade.cine.servicio.GestorReservas;
import ar.uade.cine.servicio.GestorSalas;

/** Los empleados del cine: alta, inicio de sesión y el control de acceso en la puerta. */
class MenuEmpleados implements Menu {

    private final Consola consola;
    private final GestorEmpleados empleados;
    private final GestorReservas reservas;
    private final GestorFunciones funciones;
    private final GestorCartelera cartelera;
    private final GestorSalas salas;

    MenuEmpleados(Consola consola, GestorEmpleados empleados, GestorReservas reservas,
                  GestorFunciones funciones, GestorCartelera cartelera, GestorSalas salas) {
        this.consola = consola;
        this.empleados = empleados;
        this.reservas = reservas;
        this.funciones = funciones;
        this.cartelera = cartelera;
        this.salas = salas;
    }

    @Override
    public String titulo() {
        return "Empleados";
    }

    @Override
    public void mostrar() {
        consola.mostrar("\n-- Empleados --");
        consola.mostrar("1. Listar  2. Registrar  3. Iniciar sesión  4. Validar entrada en la puerta");
        consola.pedir("Opción: ");
        switch (consola.leer()) {
            case "1" -> consola.imprimir(empleados.listar());
            case "2" -> registrar();
            case "3" -> iniciarSesion();
            case "4" -> validarEntrada();
            default -> consola.mostrar("Opción inválida");
        }
    }

    private void registrar() {
        String nombre = consola.leerTexto("Nombre: ");
        String email = consola.leerTexto("Email: ");
        String password = consola.leerTexto("Contraseña (mínimo 6): ");
        empleados.registrar(nombre, email, password,
                consola.elegir("Rol", new Rol[] {Rol.ADMINISTRADOR, Rol.ACOMODADOR}));
        consola.mostrar("Empleado registrado");
    }

    private void iniciarSesion() {
        String email = consola.leerTexto("Email: ");
        Empleado empleado = empleados.iniciarSesion(email, consola.leerTexto("Contraseña: "));
        consola.mostrar("Bienvenido, " + empleado.getNombre()
                + " (" + empleado.getRol().name().toLowerCase() + ")");
    }

    /**
     * CU-18: lo que hace el acomodador en la puerta. El código es el del QR del ticket;
     * si el escáner no lee, se tipea, y por eso el alfabeto del código no tiene O ni 0.
     */
    private void validarEntrada() {
        Reserva reserva = reservas.registrarIngreso(consola.leerTexto("Código de la reserva: "));

        consola.mostrar("ENTRADA VÁLIDA - " + reserva.getCantidadEntradas() + " butaca(s)");
        funciones.buscar(reserva.getFuncionId()).ifPresent(f -> {
            cartelera.buscar(f.getPeliculaId())
                    .ifPresent(p -> consola.mostrar("  " + p.getTitulo()));
            salas.buscar(f.getSalaId())
                    .ifPresent(s -> consola.mostrar("  " + s.getNombre()
                            + " - " + f.getInicio().format(Consola.FORMATO_FECHA)));
        });
        for (Entrada entrada : reserva.getEntradas()) {
            String acreditar = entrada.tarifa().requiereAcreditacion() ? "  <-- PEDIR CARNET" : "";
            consola.mostrar("  " + entrada.codigoAsiento() + "  " + entrada.tarifa() + acreditar);
        }
    }
}
