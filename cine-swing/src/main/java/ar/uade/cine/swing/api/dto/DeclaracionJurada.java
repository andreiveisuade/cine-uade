package ar.uade.cine.swing.api.dto;

import java.util.List;

// La semana cinematográfica (jueves a miércoles) tal cual la arma el backend; el CSV se escribe acá, en la PC.
public record DeclaracionJurada(Exhibidor exhibidor, String desde, String hasta, String generadaEn,
                                List<FuncionDeclarada> funciones, List<PeliculaDeclarada> peliculas,
                                TotalDeclarado total) {
}
