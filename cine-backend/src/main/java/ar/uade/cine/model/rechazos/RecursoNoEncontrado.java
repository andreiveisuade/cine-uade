package ar.uade.cine.model.rechazos;

// Lo pedido por id o código no existe, en la ruta o en el cuerpo; ManejadorErrores lo contesta con 404.
public final class RecursoNoEncontrado extends Rechazo {

    public RecursoNoEncontrado(String mensaje) {
        super(mensaje);
    }
}
