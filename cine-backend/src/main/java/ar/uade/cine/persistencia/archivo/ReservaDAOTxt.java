package ar.uade.cine.persistencia.archivo;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import ar.uade.cine.dominio.ventas.Entrada;
import ar.uade.cine.dominio.ventas.EstadoReserva;
import ar.uade.cine.dominio.ventas.Reserva;
import ar.uade.cine.dominio.ventas.ReservaImpl;
import ar.uade.cine.persistencia.PersistenciaException;
import ar.uade.cine.persistencia.ReservaDAO;

/**
 * Misma interfaz que ReservaDAOMySQL, otro medio: un archivo de texto con una
 * reserva por línea, campos separados por "|". Las butacas van en el mismo campo
 * separadas por coma, con su id, su código y lo que se cobró.
 *
 * <pre>
 * 1|3|7|12:A1:5000.0,13:A2:5000.0|RESERVADA|2026-08-13T20:15:00
 * 2|3|9|20:B5:8000.0|PAGADA|2026-08-13T20:41:12
 * </pre>
 *
 * Sin motor de base de datos, cualquier cambio implica reescribir el archivo entero:
 * se carga todo a memoria, se modifica la lista y se vuelca de nuevo.
 *
 * <p>Tampoco hay constraints: la garantía de que una butaca no se venda dos veces en la
 * misma función la da el UNIQUE de MySQL, y acá no existe equivalente. Alcanza porque
 * este DAO es para un solo proceso —los tests y la demo—, no para varios clientes a la vez.
 */
public class ReservaDAOTxt implements ReservaDAO {

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
        String butacas = r.getEntradas().stream()
                .map(e -> e.asientoId() + ":" + e.codigoAsiento() + ":" + e.precio())
                .reduce((a, b) -> a + "," + b)
                .orElse("");
        return r.getId() + "|" + r.getFuncionId() + "|" + r.getClienteId()
                + "|" + butacas + "|" + r.getEstado().name() + "|" + r.getCreadaEn();
    }

    private Reserva desdeLinea(String linea) {
        String[] campos = linea.split("\\|");
        List<Entrada> entradas = new ArrayList<>();
        if (!campos[3].isBlank()) {
            for (String butaca : campos[3].split(",")) {
                String[] partes = butaca.split(":");
                entradas.add(new Entrada(Integer.parseInt(partes[0]), partes[1], Double.parseDouble(partes[2])));
            }
        }
        return new ReservaImpl(
                Integer.parseInt(campos[0]),
                Integer.parseInt(campos[1]),
                Integer.parseInt(campos[2]),
                entradas,
                EstadoReserva.valueOf(campos[4]),
                LocalDateTime.parse(campos[5]));
    }
}
