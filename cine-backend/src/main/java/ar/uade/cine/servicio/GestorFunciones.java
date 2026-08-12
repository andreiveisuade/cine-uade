package ar.uade.cine.servicio;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import ar.uade.cine.interfaces.Funcion;
import ar.uade.cine.interfaces.FuncionDAO;
import ar.uade.cine.interfaces.Pelicula;
import ar.uade.cine.interfaces.PeliculaDAO;
import ar.uade.cine.interfaces.SalaDAO;
import ar.uade.cine.modelo.FuncionImpl;

/**
 * Necesita los tres DAOs porque la regla R3 no se puede validar solo con funciones:
 * cuánto dura cada una sale de la película que proyecta.
 */
public class GestorFunciones {

    private final FuncionDAO funcionDAO;
    private final PeliculaDAO peliculaDAO;
    private final SalaDAO salaDAO;

    public GestorFunciones(FuncionDAO funcionDAO, PeliculaDAO peliculaDAO, SalaDAO salaDAO) {
        this.funcionDAO = funcionDAO;
        this.peliculaDAO = peliculaDAO;
        this.salaDAO = salaDAO;
    }

    public void programar(int peliculaId, int salaId, LocalDateTime inicio, double precio) {
        Pelicula pelicula = peliculaDAO.buscarPorId(peliculaId)
                .orElseThrow(() -> new IllegalArgumentException("No existe la película " + peliculaId));
        if (salaDAO.buscarPorId(salaId).isEmpty()) {
            throw new IllegalArgumentException("No existe la sala " + salaId);
        }
        if (inicio == null) {
            throw new IllegalArgumentException("Falta la fecha y hora de la función");
        }
        if (precio <= 0) {
            throw new IllegalArgumentException("El precio debe ser mayor a cero");
        }

        LocalDateTime fin = inicio.plusMinutes(pelicula.getDuracionMinutos());
        if (haySuperposicion(salaId, inicio, fin)) {
            throw new IllegalArgumentException("La sala ya tiene una función en ese horario");
        }
        funcionDAO.guardar(new FuncionImpl(peliculaId, salaId, inicio, precio));
    }

    /** Dos rangos se pisan si cada uno empieza antes de que termine el otro. */
    private boolean haySuperposicion(int salaId, LocalDateTime inicio, LocalDateTime fin) {
        for (Funcion existente : funcionDAO.listarPorSala(salaId)) {
            int duracion = peliculaDAO.buscarPorId(existente.getPeliculaId())
                    .map(Pelicula::getDuracionMinutos)
                    .orElse(0);
            LocalDateTime finExistente = existente.getInicio().plusMinutes(duracion);
            if (inicio.isBefore(finExistente) && existente.getInicio().isBefore(fin)) {
                return true;
            }
        }
        return false;
    }

    public List<Funcion> listar() {
        return funcionDAO.listar();
    }

    public List<Funcion> listarPorPelicula(int peliculaId) {
        return funcionDAO.listarPorPelicula(peliculaId);
    }

    public Optional<Funcion> buscar(int id) {
        return funcionDAO.buscarPorId(id);
    }

    public void eliminar(int id) {
        if (funcionDAO.buscarPorId(id).isEmpty()) {
            throw new IllegalArgumentException("No existe la función " + id);
        }
        funcionDAO.eliminar(id);
    }
}
