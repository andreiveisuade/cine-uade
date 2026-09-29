package ar.uade.cine.swing.api.dto.programaciones;

// Lo que Swing manda al planificador de la semana, para proponer o para crear las funciones.
// Solo `precio` es obligatorio; lo que va null lo completa el backend con su default.
public record PedidoGrilla(String desde, Integer dias, String apertura, String cierre, Integer cuantasPeliculas,
                           Double precio, String idioma, String proyeccion) {
}
