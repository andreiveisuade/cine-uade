package ar.uade.cine.dto.ventas;

import java.util.List;

import ar.uade.cine.dto.cartelera.PeliculaVistaDTO;
import ar.uade.cine.dto.funciones.FuncionVistaDTO;
import ar.uade.cine.dto.salas.SalaVistaDTO;

public record DatosBajadaTerminalDTO(
        List<PeliculaVistaDTO> peliculas,
        List<SalaVistaDTO> salas,
        List<FuncionVistaDTO> funciones,
        List<ReservaVistaDTO> reservasActivas
) {}