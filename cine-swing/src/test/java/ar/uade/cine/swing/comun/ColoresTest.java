package ar.uade.cine.swing.comun;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.awt.Color;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

class ColoresTest {

    @AfterEach
    void volverAlOscuro() {
        Colores.usarTema(true);
    }

    // El borde de una pantalla ya armada tiene que cambiar con el tema sin recrearla (y sin volver a pedir datos).
    @Test
    void unColorCreadoAntesDeCambiarElTemaPintaConElTemaNuevo() {
        Colores.usarTema(false);
        Color error = Colores.error();
        Color borde = Colores.borde();
        int errorClaro = error.getRGB();
        int bordeClaro = borde.getRGB();

        Colores.usarTema(true);

        assertNotEquals(errorClaro, error.getRGB());
        assertEquals(new Color(0xFF6B6B).getRGB(), error.getRGB());
        assertNotEquals(bordeClaro, borde.getRGB());
    }

    @Test
    void elTranslucidoConservaSuAlfaAlCambiarElTema() {
        Color limpieza = Colores.limpieza();

        Colores.usarTema(false);

        assertEquals(70, limpieza.getAlpha());
    }
}
