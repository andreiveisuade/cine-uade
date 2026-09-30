package ar.uade.cine.model.rechazos;

// Un dato que falta o que no cumple una regla; ManejadorErrores lo contesta con 400 y el texto intacto.
public final class DatoInvalido extends Rechazo {

    public DatoInvalido(String mensaje) {
        super(mensaje);
    }
}
