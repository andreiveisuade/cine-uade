package ar.uade.cine.infrastructure.pasarelas.emulada;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import ar.uade.cine.model.ventas.MedioPago;
import ar.uade.cine.infrastructure.pasarelas.PasarelaPagos;
import ar.uade.cine.model.dinero.Dinero;

// Mercado Pago simulado en memoria: checkouts y códigos de autorización al azar; Adapter de PasarelaPagos.
// El link lleva a una pantalla de prueba del sitio, que vuelve por el retorno con el status approved o rejected.
public class MercadoPagoEmulado implements PasarelaPagos {

    private static final String PANTALLA = "/#/pasarela-emulada/";
    private static final SecureRandom AZAR = new SecureRandom();

    private final Map<String, Checkout> checkouts = new ConcurrentHashMap<>();

    // Relativo: el comprador vuelve al host del que salió, sea nginx o el servidor de Vite.
    @Override
    public Checkout crear(int reservaId, MedioPago medio, Dinero monto, LocalDateTime vence) {
        String id = "MP-" + numero(10);
        String urlPago = PANTALLA + id + "?monto=" + monto.aPesos() + "&medio=" + medio.name();
        // El QR lleva solo el id: con el monto adentro, alguien podría editarlo antes de pagar.
        Checkout checkout = new Checkout(id, reservaId, medio, monto, urlPago, "MP-QR|" + id);
        checkouts.put(id, checkout);
        return checkout;
    }

    @Override
    public Optional<Checkout> buscar(String checkoutId) {
        return checkoutId == null ? Optional.empty() : Optional.ofNullable(checkouts.get(checkoutId));
    }

    // No borra el checkout: el doble cobro lo impide R5, y así un cobro fallido se puede reintentar.
    @Override
    public String autorizar(Checkout checkout) {
        return "MP-AUT-" + numero(8);
    }

    // Acá nadie paga afuera: el checkout emulado se cobra solo cuando alguien lo confirma.
    @Override
    public List<String> aprobadosDesde(LocalDateTime desde) {
        return List.of();
    }

    private static String numero(int digitos) {
        StringBuilder codigo = new StringBuilder(digitos);
        for (int i = 0; i < digitos; i++) {
            codigo.append(AZAR.nextInt(10));
        }
        return codigo.toString();
    }
}
