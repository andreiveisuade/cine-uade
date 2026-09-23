package ar.uade.cine.infrastructure.comprobantes;

import ar.uade.cine.service.informes.Bordero;

public interface GeneradorBordero {

    void emitir(Bordero bordero);
}
