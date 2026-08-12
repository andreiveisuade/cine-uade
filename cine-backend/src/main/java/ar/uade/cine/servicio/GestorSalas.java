package ar.uade.cine.servicio;

import java.util.List;
import java.util.Optional;

import ar.uade.cine.interfaces.Sala;
import ar.uade.cine.interfaces.SalaDAO;
import ar.uade.cine.modelo.SalaImpl;

public class GestorSalas {

    private final SalaDAO dao;

    public GestorSalas(SalaDAO dao) {
        this.dao = dao;
    }

    public void agregar(String nombre, int capacidad) {
        if (nombre == null || nombre.isBlank()) {
            throw new IllegalArgumentException("El nombre no puede estar vacío");
        }
        if (capacidad <= 0) {
            throw new IllegalArgumentException("La capacidad debe ser mayor a cero");
        }
        boolean repetida = dao.listar().stream()
                .anyMatch(s -> s.getNombre().equalsIgnoreCase(nombre));
        if (repetida) {
            throw new IllegalArgumentException("Ya existe una sala con ese nombre");
        }
        dao.guardar(new SalaImpl(nombre, capacidad));
    }

    public List<Sala> listar() {
        return dao.listar();
    }

    public Optional<Sala> buscar(int id) {
        return dao.buscarPorId(id);
    }

    public void eliminar(int id) {
        if (dao.buscarPorId(id).isEmpty()) {
            throw new IllegalArgumentException("No existe la sala " + id);
        }
        dao.eliminar(id);
    }
}
