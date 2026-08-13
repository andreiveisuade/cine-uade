package ar.uade.cine.servicio;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import ar.uade.cine.dominio.funciones.Funcion;
import ar.uade.cine.dominio.salas.Asiento;
import ar.uade.cine.dominio.salas.EstadoAsiento;
import ar.uade.cine.dominio.ventas.Entrada;
import ar.uade.cine.dominio.ventas.EstadoReserva;
import ar.uade.cine.dominio.ventas.Reserva;
import ar.uade.cine.persistencia.AsientoDAO;
import ar.uade.cine.persistencia.FuncionDAO;
import ar.uade.cine.persistencia.ReservaDAO;

/**
 * Qué butacas están tomadas en una función y cuáles quedan.
 *
 * <p>Una butaca no está ocupada en sí misma: lo está <em>para una función</em>, si alguna
 * reserva vigente de esa función la tomó. Por eso esto no es asunto de la reserva ni de la
 * sala —el sujeto es la función— y vive en su propia clase en vez de adentro de
 * {@link GestorReservas}, que trata el ciclo de vida de una reserva.
 *
 * <p>Es la <strong>única</strong> definición de "ocupado" del sistema. La consola y la API
 * dibujan el mismo mapa de butacas preguntándole acá, y {@link GestorReservas} valida
 * contra lo mismo al vender: si cada uno lo calculara por su cuenta, el mapa podría
 * ofrecer una butaca que la reserva después rechaza.
 */
public class Ocupacion {

    private final ReservaDAO reservaDAO;
    private final FuncionDAO funcionDAO;
    private final AsientoDAO asientoDAO;

    public Ocupacion(ReservaDAO reservaDAO, FuncionDAO funcionDAO, AsientoDAO asientoDAO) {
        this.reservaDAO = reservaDAO;
        this.funcionDAO = funcionDAO;
        this.asientoDAO = asientoDAO;
    }

    /**
     * Ids de las butacas tomadas en esa función. Las reservas canceladas y las expiradas
     * liberan las suyas (R6).
     */
    public Set<Integer> asientosOcupados(int funcionId) {
        expirarVencidas(funcionId);
        return reservaDAO.listarPorFuncion(funcionId).stream()
                .filter(Reserva::estaVigente)
                .flatMap(r -> r.getEntradas().stream())
                .map(Entrada::asientoId)
                .collect(Collectors.toSet());
    }

    /** Butacas de la sala que todavía nadie tomó para esa función. */
    public List<Asiento> asientosLibres(int funcionId) {
        Funcion funcion = funcionDAO.buscarPorId(funcionId)
                .orElseThrow(() -> new IllegalArgumentException("No existe la función " + funcionId));
        Set<Integer> ocupados = asientosOcupados(funcionId);
        return asientoDAO.listarPorSala(funcion.getSalaId()).stream()
                .filter(a -> a.getEstado() != EstadoAsiento.FUERA_DE_SERVICIO)
                .filter(a -> !ocupados.contains(a.getId()))
                .toList();
    }

    public int lugaresLibres(int funcionId) {
        return asientosLibres(funcionId).size();
    }

    /**
     * Cierra las reservas de esa función que nadie pagó a tiempo, y con eso devuelve sus
     * butacas a la venta.
     *
     * <p>No hay proceso de fondo ni scheduler: la limpieza la hace quien consulta, que es
     * justo el momento en que importa. Y tiene que <strong>escribir</strong>, no solo
     * derivar el estado al vuelo: el <code>UNIQUE (funcion_id, asiento_id)</code> de la
     * base no sabe nada de vencimientos, así que si la fila de la entrada conserva su
     * funcion_id, la butaca queda libre en la teoría y bloqueada en la práctica.
     */
    private void expirarVencidas(int funcionId) {
        LocalDateTime ahora = LocalDateTime.now();
        for (Reserva reserva : reservaDAO.listarPorFuncion(funcionId)) {
            if (reserva.estaVencida(ahora)) {
                reserva.setEstado(EstadoReserva.EXPIRADA);
                reservaDAO.actualizar(reserva);
            }
        }
    }
}
