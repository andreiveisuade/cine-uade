package ar.uade.cine.infrastructure.seguridad;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import ar.uade.cine.ConfiguracionDePrueba.RelojMovible;

class LimiteDeIntentosTest {

    private static final String IP = "200.1.1.1";

    private final RelojMovible reloj = new RelojMovible();
    private final LimiteDeIntentos limite = new LimiteDeIntentos(reloj);

    private void fallar(int veces, String email, String ip) {
        for (int i = 0; i < veces; i++) {
            limite.fallo(email, ip);
        }
    }

    // Por email frena al que prueba claves de una cuenta desde muchas IP; por IP, al que prueba muchas cuentas.
    @ParameterizedTest(name = "{0}")
    @CsvSource(textBlock = """
            mismo email desde otra IP,  ana@mail.com,  10.0.0.9
            otro email desde la misma IP, beto@mail.com, 200.1.1.1
            """)
    void cincoFallosSeguidosBloquean(String caso, String email, String ip) {
        fallar(4, "ana@mail.com", IP);
        assertFalse(limite.bloqueado(email, ip));

        limite.fallo("ana@mail.com", IP);

        assertTrue(limite.bloqueado(email, ip));
    }

    @Test
    void pasadoElMinutoVuelveAPoderProbar() {
        fallar(5, "ana@mail.com", IP);

        reloj.mover(RelojMovible.INICIO.plusSeconds(59));
        assertTrue(limite.bloqueado("ana@mail.com", IP));
        reloj.mover(RelojMovible.INICIO.plusMinutes(1));
        assertFalse(limite.bloqueado("ana@mail.com", IP));
    }

    @Test
    void unIngresoCorrectoReiniciaLosFallosDelEmail() {
        fallar(4, "ana@mail.com", IP);

        limite.exito("ana@mail.com");
        limite.fallo("ana@mail.com", "10.0.0.9");

        assertFalse(limite.bloqueado("ana@mail.com", "10.0.0.9"));
    }

    // Sin limpieza, cada email inventado quedaría en memoria para siempre.
    @Test
    void lasEntradasVencidasSeBorran() {
        fallar(1, "ana@mail.com", IP);
        reloj.mover(RelojMovible.INICIO.plusMinutes(2));

        limite.fallo("beto@mail.com", "10.0.0.9");

        assertEquals(2, limite.registrados());
    }
}
