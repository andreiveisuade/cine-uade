package ar.uade.cine.infrastructure.seguridad;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Component;

import ar.uade.cine.infrastructure.reloj.Reloj;

// Cuántas claves equivocadas seguidas lleva cada email y cada IP; con 5, un minuto bloqueado.
// En memoria y no en la base: una sola instancia, y perder la cuenta al reiniciar no abre nada importante.
@Component
public class LimiteDeIntentos {

    private static final int FALLOS_PERMITIDOS = 5;
    private static final long MINUTOS_DE_BLOQUEO = 1;

    private record Fallos(int cuantos, LocalDateTime ultimo) {
    }

    private final Map<String, Fallos> fallos = new ConcurrentHashMap<>();
    private final Reloj reloj;

    public LimiteDeIntentos(Reloj reloj) {
        this.reloj = reloj;
    }

    public boolean bloqueado(String email, String ip) {
        return agotado(fallos.get(deEmail(email))) || agotado(fallos.get(deIp(ip)));
    }

    // Cada fallo barre las vencidas: así la memoria no crece con emails inventados.
    public void fallo(String email, String ip) {
        fallos.values().removeIf(this::vencido);
        sumar(deEmail(email));
        sumar(deIp(ip));
    }

    // Solo el email: si un ingreso correcto limpiara la IP, alternar con una cuenta propia esquivaría el límite.
    public void exito(String email) {
        fallos.remove(deEmail(email));
    }

    public void olvidar() {
        fallos.clear();
    }

    int registrados() {
        return fallos.size();
    }

    private void sumar(String clave) {
        LocalDateTime ahora = reloj.ahora();
        fallos.merge(clave, new Fallos(1, ahora), (antes, nuevo) ->
                vencido(antes) ? nuevo : new Fallos(antes.cuantos() + 1, ahora));
    }

    private boolean agotado(Fallos registro) {
        return registro != null && !vencido(registro) && registro.cuantos() >= FALLOS_PERMITIDOS;
    }

    private boolean vencido(Fallos registro) {
        return !reloj.ahora().isBefore(registro.ultimo().plusMinutes(MINUTOS_DE_BLOQUEO));
    }

    private static String deEmail(String email) {
        return "email:" + email;
    }

    private static String deIp(String ip) {
        return "ip:" + ip;
    }
}
