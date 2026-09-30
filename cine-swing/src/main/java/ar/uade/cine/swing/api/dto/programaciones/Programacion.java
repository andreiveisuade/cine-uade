package ar.uade.cine.swing.api.dto.programaciones;

import java.util.List;

// Una grilla como la lista Swing en Programaciones, con su rango, sus días y si sigue activa.
// `funciones` solo viene en el detalle (`GET /api/programaciones/{id}`). Previsualizada, `id` es 0.
public record Programacion(int id, int peliculaId, int salaId, String desde, String hasta, String generadaHasta,
                           String horaInicio, List<String> diasSemana, String idioma, String proyeccion,
                           double precio, boolean activa, List<FuncionGenerada> funciones) {
}
