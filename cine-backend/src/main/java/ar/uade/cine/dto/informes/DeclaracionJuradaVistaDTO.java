package ar.uade.cine.dto.informes;

import java.util.List;

// La declaración jurada de un período (GET /api/declaracion-jurada); Swing arma con ella el CSV del INCAA.
public record DeclaracionJuradaVistaDTO(ExhibidorVistaDTO exhibidor, String desde, String hasta,
                                        String generadaEn, List<FuncionDeclaradaVistaDTO> funciones,
                                        List<PeliculaDeclaradaVistaDTO> peliculas,
                                        TotalDeclaradoVistaDTO total) {
}
