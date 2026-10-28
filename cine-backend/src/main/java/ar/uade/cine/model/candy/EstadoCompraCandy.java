package ar.uade.cine.model.candy;

import ar.uade.cine.model.rechazos.ConflictoDeNegocio;

// Estados de una compra del candy y sus transiciones (R30), una constante por estado.
// Patrón State (GoF), como EstadoReserva: lo que permite cada estado vive en su constante, no en CompraCandy.
public enum EstadoCompraCandy {

    PENDIENTE("sin cobrar") {
        @Override
        EstadoCompraCandy cobrar() {
            return COBRADA;
        }

        @Override
        EstadoCompraCandy anular() {
            return ANULADA;
        }

        @Override
        public boolean esperaCobro() {
            return true;
        }
    },

    COBRADA("cobrada") {
        @Override
        public boolean estaCobrada() {
            return true;
        }
    },

    ANULADA("anulada");

    private final String etiqueta;

    EstadoCompraCandy(String etiqueta) {
        this.etiqueta = etiqueta;
    }

    // De paquete: solo CompraCandy cambia su estado.
    EstadoCompraCandy cobrar() {
        throw rechazo("no se puede cobrar");
    }

    // R30: se anula solo el pedido que todavía espera el pago de su reserva.
    EstadoCompraCandy anular() {
        throw rechazo("solo se anula un pedido sin cobrar");
    }

    public boolean esperaCobro() {
        return false;
    }

    public boolean estaCobrada() {
        return false;
    }

    private ConflictoDeNegocio rechazo(String queNoSePuede) {
        return new ConflictoDeNegocio("La compra de candy está " + etiqueta + ": " + queNoSePuede);
    }
}
