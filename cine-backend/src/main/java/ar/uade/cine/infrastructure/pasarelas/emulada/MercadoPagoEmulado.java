package ar.uade.cine.infrastructure.pasarelas.emulada;

import java.security.SecureRandom;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import ar.uade.cine.model.ventas.MedioPago;
import ar.uade.cine.infrastructure.pasarelas.PasarelaPagos;
import ar.uade.cine.model.dinero.Dinero;

public class MercadoPagoEmulado implements PasarelaPagos {

    private static final String HOST = "https://checkout.emulado.local/mp/";
    private static final SecureRandom AZAR = new SecureRandom();

    private final Map<String, Checkout> checkouts = new ConcurrentHashMap<>();

    @Override
    public Checkout crear(int reservaId, MedioPago medio, Dinero monto) {
        String id = "MP-" + numero(10);
        // El QR lleva solo el id: con el monto adentro, alguien podría editarlo antes de pagar.
        Checkout checkout = new Checkout(id, reservaId, medio, monto, HOST + id, "MP-QR|" + id);
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

    private static String numero(int digitos) {
        StringBuilder codigo = new StringBuilder(digitos);
        for (int i = 0; i < digitos; i++) {
            codigo.append(AZAR.nextInt(10));
        }
        return codigo.toString();
    }
}
