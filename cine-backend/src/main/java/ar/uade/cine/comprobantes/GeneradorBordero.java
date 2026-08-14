package ar.uade.cine.comprobantes;

import ar.uade.cine.servicio.Bordero;

/**
 * Emite el borderó de una función en un archivo, que es lo que después se sube al INCAA.
 *
 * <p>Es la misma clase de contrato que {@link GeneradorTicket}: se escribe una vez y el
 * programa no lo vuelve a leer, así que no es un DAO y no tiene buscar ni eliminar. Y es
 * una interfaz y no un método del gestor porque el organismo puede cambiar el formato que
 * acepta —un CSV, un XML— sin que eso sea un cambio en cómo se cuenta la recaudación.
 *
 * <p>Recibe el {@link Bordero} ya calculado y no los ids de nada: quien da formato no
 * consulta. Si tuviera que ir a buscar la película o los pagos, la cuenta terminaría
 * partida entre el gestor y el generador, y dos archivos distintos podrían declarar
 * números distintos.
 */
public interface GeneradorBordero {

    void emitir(Bordero bordero);
}
