package ar.uade.cine.service.ventas;

import java.util.List;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import ar.uade.cine.infrastructure.pasarelas.PasarelaPagos;
import ar.uade.cine.infrastructure.reloj.Reloj;
import ar.uade.cine.model.rechazos.Rechazo;
import ar.uade.cine.model.ventas.Reserva;

// Cobra lo que la pasarela aprobó aunque el comprador no vuelva al sitio (R17); tarea programada.
// MP no muestra "Volver al sitio" con una back_url local: sin esto cobraba y la reserva vencía.
// Sin @Transactional a propósito, como GestorImportaciones: cada checkout va en su propia transacción de
// GestorPagos, y uno que no se puede cobrar no deja rollback-only a los demás.
@Service
@RequiredArgsConstructor
@Slf4j
public class ConciliacionPagos {

    private final PasarelaPagos pasarela;
    private final GestorPagos pagos;
    private final Reloj reloj;

    // El intervalo va por placeholder porque la anotación no lee un record Propiedades*. En el perfil test
    // no corre: ver Adaptadores.Tareas. Devuelve cuántas cobró, para los tests.
    @Scheduled(fixedDelayString = "${cine.mercadopago.conciliacion}",
            initialDelayString = "${cine.mercadopago.conciliacion}")
    public int conciliar() {
        // Un pago aprobado hace más de MINUTOS_PARA_PAGAR es de una reserva ya cobrada o ya vencida.
        List<String> aprobados;
        try {
            aprobados = pasarela.aprobadosDesde(reloj.ahora().minusMinutes(Reserva.MINUTOS_PARA_PAGAR));
        } catch (Rechazo e) {
            log.warn("conciliación: no se pudo consultar la pasarela: {}", e.getMessage());
            return 0;
        }
        int cobrados = 0;
        int fallidos = 0;
        for (String checkout : aprobados) {
            try {
                if (pagos.conciliarCheckout(checkout)) {
                    cobrados++;
                }
            } catch (RuntimeException e) {
                // Aprobado en la pasarela y sin cobrar acá (vencida, monto distinto...): no hay devolución
                // automática (R13), alguien lo tiene que mirar.
                log.warn("conciliación: el checkout {} está aprobado y no se cobró: {}", checkout, e.getMessage());
                fallidos++;
            }
        }
        if (cobrados > 0 || fallidos > 0) {
            log.info("conciliación: {} cobrados, {} sin cobrar", cobrados, fallidos);
        }
        return cobrados;
    }
}
