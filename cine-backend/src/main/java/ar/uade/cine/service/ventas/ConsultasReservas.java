package ar.uade.cine.service.ventas;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ar.uade.cine.model.cartelera.Pelicula;
import ar.uade.cine.model.usuarios.Cliente;
import ar.uade.cine.model.usuarios.Usuario;
import ar.uade.cine.model.ventas.Reserva;
import ar.uade.cine.repository.ventas.ReservaRepository;

// Lecturas de reservas para listados y búsquedas; aparte de GestorReservas, con join fetch contra el N+1.
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class ConsultasReservas {

    private final ReservaRepository reservaRepository;

    public Optional<Reserva> buscar(int id) {
        return reservaRepository.findById(id);
    }

    public Optional<Reserva> buscarPorCodigo(String codigo) {
        return codigo == null ? Optional.empty()
                : reservaRepository.findByCodigo(Reserva.normalizarCodigo(codigo));
    }

    // Con función, película, sala y cliente ya cargados: afuera de la transacción no hay
    // sesión que los traiga, y traerlos de a uno es la consulta por fila que se quiere evitar.
    public List<Reserva> conDetalle(Collection<Integer> ids) {
        return ids.isEmpty() ? List.of() : reservaRepository.findConDetalle(ids);
    }

    // Las de un cliente, la más nueva primero, en una consulta. Sin email no hay a quién buscar;
    // con espacios o mayúsculas, se busca como lo guardó Usuario.
    public List<Reserva> listarPorEmail(String email) {
        String buscado = Usuario.normalizarEmail(email);
        return buscado.isEmpty() ? List.of() : reservaRepository.findByCliente_EmailOrderByIdDesc(buscado);
    }

    // La más nueva primero, ordenada en la base. Estado y día también en la base; el texto en
    // memoria porque el código de butaca se arma de fila y número.
    public List<Reserva> buscar(CriteriosReserva criterios) {
        if (criterios == null || criterios.sinFiltros()) {
            return reservaRepository.findAllByOrderByIdDesc();
        }
        LocalDateTime desde = criterios.dia() == null ? null : criterios.dia().atStartOfDay();
        LocalDateTime hasta = desde == null ? null : desde.plusDays(1);
        String texto = criterios.textoNormalizado();
        return reservaRepository.buscar(criterios.estado(), desde, hasta).stream()
                .filter(r -> coincideElTexto(r, texto))
                .toList();
    }

    private static boolean coincideElTexto(Reserva reserva, String texto) {
        if (texto.isEmpty() || contiene(reserva.getCodigo(), texto)) {
            return true;
        }
        if (reserva.getEntradas().stream().anyMatch(e -> contiene(e.codigoAsiento(), texto))) {
            return true;
        }
        Cliente cliente = reserva.getCliente();
        if (cliente != null && (contiene(cliente.getNombre(), texto) || contiene(cliente.getEmail(), texto))) {
            return true;
        }
        Pelicula pelicula = reserva.getFuncion().getPelicula();
        return pelicula != null && contiene(pelicula.getTitulo(), texto);
    }

    private static boolean contiene(String campo, String texto) {
        return campo != null && campo.toLowerCase().contains(texto);
    }
}
