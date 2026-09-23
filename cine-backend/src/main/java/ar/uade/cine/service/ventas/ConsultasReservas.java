package ar.uade.cine.service.ventas;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ar.uade.cine.model.cartelera.Pelicula;
import ar.uade.cine.model.usuarios.Cliente;
import ar.uade.cine.model.ventas.Reserva;
import ar.uade.cine.repository.ReservaRepository;

@Service
@Transactional(readOnly = true)
public class ConsultasReservas {

    private final ReservaRepository reservaRepository;

    public ConsultasReservas(ReservaRepository reservaRepository) {
        this.reservaRepository = reservaRepository;
    }

    public Optional<Reserva> buscar(int id) {
        return reservaRepository.findById(id);
    }

    public Optional<Reserva> buscarPorCodigo(String codigo) {
        return codigo == null ? Optional.empty()
                : reservaRepository.findByCodigo(codigo.trim().toUpperCase());
    }

    public List<Reserva> listarPorCliente(int clienteId) {
        return reservaRepository.findByCliente_IdOrderByCreadaEnDesc(clienteId);
    }

    // Estado y día en la base; el texto en memoria porque el código de butaca se arma de fila y número.
    public List<Reserva> buscar(CriteriosReserva criterios) {
        if (criterios == null || criterios.sinFiltros()) {
            return reservaRepository.findAll();
        }
        LocalDateTime desde = criterios.dia() == null ? null : criterios.dia().atStartOfDay();
        LocalDateTime hasta = desde == null ? null : desde.plusDays(1);
        String texto = criterios.textoNormalizado();
        return reservaRepository.buscar(criterios.estado(), desde, hasta).stream()
                .filter(r -> coincideElTexto(r, texto, r.getCliente(), r.getFuncion().getPelicula()))
                .toList();
    }

    private static boolean coincideElTexto(Reserva reserva, String texto, Cliente cliente,
                                           Pelicula pelicula) {
        if (texto.isEmpty() || contiene(reserva.getCodigo(), texto)) {
            return true;
        }
        if (reserva.getEntradas().stream().anyMatch(e -> contiene(e.codigoAsiento(), texto))) {
            return true;
        }
        if (cliente != null && (contiene(cliente.getNombre(), texto) || contiene(cliente.getEmail(), texto))) {
            return true;
        }
        return pelicula != null && contiene(pelicula.getTitulo(), texto);
    }

    private static boolean contiene(String campo, String texto) {
        return campo != null && campo.toLowerCase().contains(texto);
    }
}
