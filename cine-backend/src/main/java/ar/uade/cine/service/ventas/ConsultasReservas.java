package ar.uade.cine.service.ventas;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ar.uade.cine.model.cartelera.Pelicula;
import ar.uade.cine.model.funciones.Funcion;
import ar.uade.cine.model.usuarios.Cliente;
import ar.uade.cine.model.ventas.Reserva;
import ar.uade.cine.repository.ClienteRepository;
import ar.uade.cine.repository.FuncionRepository;
import ar.uade.cine.repository.PeliculaRepository;
import ar.uade.cine.repository.ReservaRepository;

@Service
@Transactional(readOnly = true)
public class ConsultasReservas {

    private final ReservaRepository reservaRepository;
    private final FuncionRepository funcionRepository;
    private final PeliculaRepository peliculaRepository;
    private final ClienteRepository clienteRepository;

    public ConsultasReservas(ReservaRepository reservaRepository, FuncionRepository funcionRepository,
                             PeliculaRepository peliculaRepository, ClienteRepository clienteRepository) {
        this.reservaRepository = reservaRepository;
        this.funcionRepository = funcionRepository;
        this.peliculaRepository = peliculaRepository;
        this.clienteRepository = clienteRepository;
    }

    public Optional<Reserva> buscar(int id) {
        return reservaRepository.findById(id);
    }

    public List<Reserva> listarPorCliente(int clienteId) {
        return reservaRepository.findByCliente_IdOrderByCreadaEnDesc(clienteId);
    }

    // En memoria y no con WHERE: el texto cruza cuatro tablas.
    public List<Reserva> buscar(CriteriosReserva criterios) {
        if (criterios == null || criterios.sinFiltros()) {
            return reservaRepository.findAll();
        }
        Map<Integer, Funcion> funciones = porId(funcionRepository.findAll(), Funcion::getId);
        Map<Integer, Pelicula> peliculas = porId(peliculaRepository.findAll(), Pelicula::getId);
        Map<Integer, Cliente> clientes = porId(clienteRepository.findAll(), Cliente::getId);
        String texto = criterios.textoNormalizado();
        return reservaRepository.findAll().stream()
                .filter(r -> criterios.estado() == null || r.getEstado() == criterios.estado())
                .filter(r -> criterios.dia() == null
                        || esDelDia(funciones.get(r.getFuncionId()), criterios.dia()))
                .filter(r -> coincideElTexto(r, texto, clientes.get(r.getClienteId()),
                        peliculaDe(funciones.get(r.getFuncionId()), peliculas)))
                .toList();
    }

    private static <T> Map<Integer, T> porId(List<T> elementos, Function<T, Integer> id) {
        return elementos.stream().collect(Collectors.toMap(id, Function.identity()));
    }

    private static boolean esDelDia(Funcion funcion, LocalDate dia) {
        return funcion != null && funcion.getInicio().toLocalDate().equals(dia);
    }

    private static Pelicula peliculaDe(Funcion funcion, Map<Integer, Pelicula> peliculas) {
        return funcion == null ? null : peliculas.get(funcion.getPeliculaId());
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
