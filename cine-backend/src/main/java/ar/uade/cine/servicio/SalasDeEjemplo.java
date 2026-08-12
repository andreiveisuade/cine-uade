package ar.uade.cine.servicio;

import java.util.List;

import ar.uade.cine.modelo.TipoSala;

/**
 * Las seis salas del complejo, con distribuciones distintas. Cargarlas de una permite
 * probar el sistema (y dibujar el mapa de butacas) sin darlas de alta a mano.
 */
public class SalasDeEjemplo {

    private final GestorSalas gestorSalas;

    public SalasDeEjemplo(GestorSalas gestorSalas) {
        this.gestorSalas = gestorSalas;
    }

    public void cargar() {
        // Chica y en cuña: las filas de adelante son más cortas por la forma de la sala.
        gestorSalas.agregar("Sala 1", TipoSala.DOS_D,
                List.of(8, 10, 12, 12, 14),
                List.of(),
                List.of("A1", "A8"));

        // Rectangular clásica, la más usada del complejo.
        gestorSalas.agregar("Sala 2", TipoSala.DOS_D,
                List.of(14, 14, 14, 14, 14, 14, 14, 14),
                List.of(),
                List.of("A1", "A14"));

        // Grande con las dos últimas filas premium.
        gestorSalas.agregar("Sala 3", TipoSala.TRES_D,
                List.of(12, 14, 16, 18, 18, 20, 20, 20, 16, 16),
                List.of("I1", "I2", "I3", "I4", "J1", "J2", "J3", "J4"),
                List.of("A1", "A12"));

        // La más grande: pantalla gigante y platea profunda.
        gestorSalas.agregar("Sala 4", TipoSala.IMAX,
                List.of(16, 18, 20, 22, 22, 24, 24, 24, 24, 22, 20, 18),
                List.of(),
                List.of("A1", "A2", "A15", "A16"));

        // Butacas de pareja, pocas y todas premium.
        gestorSalas.agregar("Sala 5", TipoSala.VIP,
                List.of(6, 6, 8, 8),
                List.of("A1", "A2", "A3", "A4", "A5", "A6",
                        "B1", "B2", "B3", "B4", "B5", "B6",
                        "C1", "C2", "C3", "C4", "C5", "C6", "C7", "C8",
                        "D1", "D2", "D3", "D4", "D5", "D6", "D7", "D8"),
                List.of());

        // Sala de efectos: butacas móviles, capacidad media.
        gestorSalas.agregar("Sala 6", TipoSala.CUATRO_D,
                List.of(10, 12, 12, 14, 14, 12),
                List.of(),
                List.of("A1", "A10"));
    }
}
