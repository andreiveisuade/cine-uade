package ar.uade.cine.swing.api.dto.programaciones;

import java.util.List;

// `funciones` solo viene en el detalle (`GET /api/programaciones/{id}`). Previsualizada, `id` es 0.
public record Programacion(int id, int peliculaId, int salaId, String desde, String hasta, String generadaHasta,
                           String horaInicio, List<String> diasSemana, String idioma, String proyeccion,
                           double precio, boolean activa, List<FuncionGenerada> funciones) {
}
