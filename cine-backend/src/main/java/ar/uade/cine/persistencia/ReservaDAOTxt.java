package ar.uade.cine.persistencia;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import ar.uade.cine.interfaces.Reserva;
import ar.uade.cine.interfaces.ReservaDAO;
import ar.uade.cine.modelo.EstadoReserva;
import ar.uade.cine.modelo.ReservaImpl;

/**
 * Misma interfaz que ReservaDAOMySQL, otro medio: un archivo de texto con una
 * reserva por línea, campos separados por "|".
 *
 * <pre>
 * 1|3|7|2|RESERVADA
 * 2|3|9|4|PAGADA
 * </pre>
 *
 * Sin motor de base de datos, cualquier cambio implica reescribir el archivo entero:
 * se carga todo a memoria, se modifica la lista y se vuelca de nuevo.
 */
public class ReservaDAOTxt implements ReservaDAO {

    private static final String SEPARADOR = "\\|";

    private final Path archivo;

    public ReservaDAOTxt() {
        this(Path.of("reservas.txt"));
    }

    public ReservaDAOTxt(Path archivo) {
        this.archivo = archivo;
    }

    @Override
    public void guardar(Reserva reserva) {
        List<Reserva> reservas = listar();
        int proximoId = reservas.stream().mapToInt(Reserva::getId).max().orElse(0) + 1;
        reserva.setId(proximoId);
        reservas.add(reserva);
        volcar(reservas);
    }

    @Override
    public void actualizar(Reserva reserva) {
        List<Reserva> reservas = listar();
        for (int i = 0; i < reservas.size(); i++) {
            if (reservas.get(i).getId() == reserva.getId()) {
                reservas.set(i, reserva);
                volcar(reservas);
                return;
            }
        }
        throw new PersistenciaException("No existe la reserva " + reserva.getId() + " en " + archivo);
    }

    @Override
    public Optional<Reserva> buscarPorId(int id) {
        return listar().stream().filter(r -> r.getId() == id).findFirst();
    }

    @Override
    public List<Reserva> listar() {
        if (!Files.exists(archivo)) {
            return new ArrayList<>();
        }
        try {
            List<Reserva> reservas = new ArrayList<>();
            for (String linea : Files.readAllLines(archivo)) {
                if (!linea.isBlank()) {
                    reservas.add(desdeLinea(linea));
                }
            }
            return reservas;
        } catch (IOException e) {
            throw new PersistenciaException("No se pudo leer " + archivo, e);
        }
    }

    @Override
    public List<Reserva> listarPorFuncion(int funcionId) {
        return listar().stream().filter(r -> r.getFuncionId() == funcionId).toList();
    }

    @Override
    public List<Reserva> listarPorCliente(int clienteId) {
        return listar().stream().filter(r -> r.getClienteId() == clienteId).toList();
    }

    private void volcar(List<Reserva> reservas) {
        List<String> lineas = reservas.stream().map(this::aLinea).toList();
        try {
            Files.write(archivo, lineas);
        } catch (IOException e) {
            throw new PersistenciaException("No se pudo escribir " + archivo, e);
        }
    }

    private String aLinea(Reserva r) {
        return r.getId() + "|" + r.getFuncionId() + "|" + r.getClienteId()
                + "|" + r.getCantidadEntradas() + "|" + r.getEstado().name();
    }

    private Reserva desdeLinea(String linea) {
        String[] campos = linea.split(SEPARADOR);
        return new ReservaImpl(
                Integer.parseInt(campos[0]),
                Integer.parseInt(campos[1]),
                Integer.parseInt(campos[2]),
                Integer.parseInt(campos[3]),
                EstadoReserva.valueOf(campos[4]));
    }
}
