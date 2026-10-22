package ar.uade.cine.model.rechazos;

// Falta probar que lo pedido es de quien lo pide (R22: el código de una entrada); se responde 403.
// Status propio para que la web pida el dato que falta sin leer el texto.
public final class SinPrueba extends Rechazo {

    public SinPrueba(String mensaje) {
        super(mensaje);
    }
}
