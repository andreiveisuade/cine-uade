package ar.uade.cine.dto.informes;

import java.util.List;

public record DeclaracionJuradaVistaDTO(ExhibidorVistaDTO exhibidor, String desde, String hasta,
                                        String generadaEn, List<FuncionDeclaradaVistaDTO> funciones,
                                        List<PeliculaDeclaradaVistaDTO> peliculas,
                                        TotalDeclaradoVistaDTO total) {
}
