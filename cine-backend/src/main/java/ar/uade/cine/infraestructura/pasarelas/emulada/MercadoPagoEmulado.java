package ar.uade.cine.infraestructura.pasarelas.emulada;

import java.security.SecureRandom;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import ar.uade.cine.dominio.ventas.MedioPago;
import ar.uade.cine.infraestructura.pasarelas.PasarelaPagos;
import ar.uade.cine.dominio.dinero.Dinero;

/**
 * El checkout de MercadoPago, emulado: arma el link y el QR, y devuelve un código de
 * autorización como lo haría el procesador. <strong>No hay credenciales ni llamadas de
 * red</strong>, y por eso el nombre lo dice: nadie tiene que abrir el archivo para
 * descubrir que atrás no hay nada.
 *
 * <p>Los checkouts viven en memoria y se pierden al reiniciar. Es a propósito y no una
 * simplificación pendiente: en la integración de verdad el checkout es un objeto de
 * MercadoPago, no del cine, y guardarlo en una tabla nuestra sería duplicar un dato ajeno
 * que además vence solo. Lo que sí es nuestro —que esa reserva se cobró, con qué medio y
 * con qué autorización— se persiste, pero en {@code pago}, cuando el pago se confirma.
 *
 * <p>Emula lo que se puede emular sin mentir: la ida —abrir el checkout— y la vuelta —el
 * código—. Lo que no emula es el rechazo, porque el modelo tampoco lo tiene: acá un pago
 * registrado es un pago exitoso, y una tarjeta sin fondos hoy no se puede representar en
 * ninguna parte del sistema.
 *
 * <p>El host es {@code emulado.local} para que sea imposible confundirlo con uno real: un
 * link que arranque con el dominio verdadero terminaría copiado en una demo.
 */
public class MercadoPagoEmulado implements PasarelaPagos {

    private static final String HOST = "https://checkout.emulado.local/mp/";
    private static final SecureRandom AZAR = new SecureRandom();

    /**
     * Concurrente porque la API atiende varios pedidos a la vez: dos personas pagando al
     * mismo tiempo son dos hilos escribiendo este mapa.
     */
    private final Map<String, Checkout> checkouts = new ConcurrentHashMap<>();

    @Override
    public Checkout crear(int reservaId, MedioPago medio, Dinero monto) {
        String id = "MP-" + numero(10);
        // El QR no lleva la reserva ni el monto sueltos: lleva el id del checkout, que es
        // lo que el procesador sabe resolver. Poner el monto adentro dejaría que alguien
        // lo editara antes de pagar.
        Checkout checkout = new Checkout(id, reservaId, medio, monto, HOST + id, "MP-QR|" + id);
        checkouts.put(id, checkout);
        return checkout;
    }

    @Override
    public Optional<Checkout> buscar(String checkoutId) {
        return checkoutId == null ? Optional.empty() : Optional.ofNullable(checkouts.get(checkoutId));
    }

    /**
     * El checkout no se borra al autorizar, y no es un olvido: quien impide que la misma
     * reserva se cobre dos veces es R5, en el gestor. Si la pasarela lo cerrara, un cobro
     * que falla después de autorizar —la reserva venció mientras el cliente pagaba—
     * dejaría el checkout quemado y sin forma de reintentar, que es peor que un mapa que
     * crece: en la integración de verdad, el que vence los checkouts es MercadoPago.
     */
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
