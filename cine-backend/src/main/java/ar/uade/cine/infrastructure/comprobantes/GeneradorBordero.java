package ar.uade.cine.infrastructure.comprobantes;

import ar.uade.cine.service.informes.Bordero;

/**
 * Emite el borderó de una función, el archivo que se sube al INCAA. Es interfaz porque el
 * formato puede cambiar sin que cambie la cuenta; recibe el {@link Bordero} ya calculado
 * para que quien da formato no consulte ni recalcule.
 */
public interface GeneradorBordero {

    void emitir(Bordero bordero);
}
