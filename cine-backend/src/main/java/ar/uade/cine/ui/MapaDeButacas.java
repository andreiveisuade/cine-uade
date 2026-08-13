package ar.uade.cine.ui;

import java.util.List;

import ar.uade.cine.dominio.funciones.Funcion;
import ar.uade.cine.dominio.salas.Asiento;
import ar.uade.cine.dominio.salas.EstadoAsiento;
import ar.uade.cine.servicio.GestorFunciones;
import ar.uade.cine.servicio.GestorSalas;
import ar.uade.cine.servicio.Ocupacion;

/**
 * Dibuja la sala en caracteres. Lo usan el menú de salas —para ver cómo quedó una sala
 * recién creada— y el de reservas —para elegir butaca sobre el mapa de una función—, así
 * que vive aparte de los dos: es la misma sala dibujada igual, con o sin función.
 */
class MapaDeButacas {

    private final Consola consola;
    private final GestorSalas salas;
    private final GestorFunciones funciones;
    private final Ocupacion ocupacion;

    MapaDeButacas(Consola consola, GestorSalas salas, GestorFunciones funciones,
                  Ocupacion ocupacion) {
        this.consola = consola;
        this.salas = salas;
        this.funciones = funciones;
        this.ocupacion = ocupacion;
    }

    /** La sala vacía, fila por fila: todas las butacas cuentan como libres. */
    void deSala(int salaId) {
        salas.buscar(salaId)
                .orElseThrow(() -> new IllegalArgumentException("No existe la sala " + salaId));
        dibujar(salas.asientosDe(salaId), List.of());
    }

    /** El mapa de una función, con las butacas ya tomadas marcadas. */
    void deFuncion(int funcionId) {
        Funcion funcion = funciones.buscar(funcionId)
                .orElseThrow(() -> new IllegalArgumentException("No existe la función " + funcionId));
        List<Asiento> todos = salas.asientosDe(funcion.getSalaId());
        List<Asiento> libres = ocupacion.asientosLibres(funcionId);
        dibujar(todos, libres);
        consola.mostrar("Libres: " + libres.size() + " de " + todos.size());
    }

    /**
     * Una línea por fila. libres vacío = sala sin función, todas disponibles.
     * [B4] libre, (B4) ocupada, y el tipo se marca con un símbolo.
     */
    private void dibujar(List<Asiento> asientos, List<Asiento> libres) {
        List<Integer> idsLibres = libres.stream().map(Asiento::getId).toList();
        boolean sinFuncion = libres.isEmpty();
        consola.mostrar("        ---------- PANTALLA ----------");
        int filaActual = -1;
        StringBuilder linea = new StringBuilder();
        for (Asiento asiento : asientos) {
            if (asiento.getFila() != filaActual) {
                if (filaActual != -1) {
                    consola.mostrar(linea.toString());
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
                linea.append(libre ? "[" : "(").append(asiento.getNumero()).append(marca)
                        .append(libre ? "] " : ") ");
            }
        }
        if (filaActual != -1) {
            consola.mostrar(linea.toString());
        }
        consola.mostrar("  [n] libre  (n) ocupada  {n} fuera de servicio  * VIP  & pareja  + accesible");
    }
}
