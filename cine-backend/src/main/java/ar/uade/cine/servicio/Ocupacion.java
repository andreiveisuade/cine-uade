package ar.uade.cine.servicio;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.IntPredicate;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ar.uade.cine.dominio.funciones.Funcion;
import ar.uade.cine.dominio.salas.Asiento;
import ar.uade.cine.dominio.salas.EstadoAsiento;
import ar.uade.cine.dominio.ventas.Entrada;
import ar.uade.cine.dominio.ventas.EstadoReserva;
import ar.uade.cine.dominio.ventas.Reserva;
import ar.uade.cine.persistencia.AsientoDAO;
import ar.uade.cine.persistencia.BloqueoButacas;
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
 *
 * <h2>Ocupada por dos motivos distintos, y las dos etapas son necesarias</h2>
 * <p>Una butaca puede estar tomada porque <strong>alguien la compró</strong> —hay una
 * reserva vigente con esa entrada— o porque <strong>alguien la está eligiendo</strong> —hay
 * un bloqueo a nombre de una sesión—. Son dos mecanismos con vencimiento, y no hacen lo
 * mismo:
 *
 * <table>
 *   <caption>Las dos ventanas</caption>
 *   <tr><th></th><th>Bloqueo</th><th>Reserva RESERVADA</th></tr>
 *   <tr><td>Etapa</td><td>eligiendo butacas</td><td>elegidas, falta pagar</td></tr>
 *   <tr><td>Dura</td><td>{@link #MIENTRAS_ELIGE}</td>
 *       <td>{@link Reserva#MINUTOS_PARA_PAGAR} minutos</td></tr>
 *   <tr><td>De quién es</td><td>de una sesión anónima</td><td>de un cliente con nombre y mail</td></tr>
 *   <tr><td>Deja rastro</td><td>ninguno: se borra solo</td><td>historial, ticket, código de acceso</td></tr>
 *   <tr><td>Al vencer</td><td>desaparece</td><td>pasa a EXPIRADA, que es un dato del negocio</td></tr>
 * </table>
 *
 * <p>Que el bloqueo reemplazara al estado RESERVADA sería un retroceso: las reservas
 * esperando pago dejarían de sobrevivir a un reinicio, y el cliente no podría ver en su
 * historial algo que todavía no pagó. Y al revés tampoco: no se puede crear una reserva
 * mientras alguien elige, porque una reserva necesita un cliente y el cliente recién se
 * identifica al confirmar.
 *
 * <p>Las dos ventanas nunca se solapan sobre la misma butaca: cuando la reserva nace,
 * {@link GestorReservas#reservar} suelta el bloqueo, porque a partir de ahí la butaca la
 * retiene la reserva.
 */
public class Ocupacion {

    private static final Logger LOG = LoggerFactory.getLogger(Ocupacion.class);

    /**
     * Cuánto se le guarda una butaca a quien la está eligiendo. Es una regla de negocio y
     * por eso vive acá y no en el adaptador: el medio donde se guarda el bloqueo no decide
     * cuánto dura.
     *
     * <p>Corta a propósito, y mucho más corta que los treinta minutos para pagar: es el
     * tiempo de tocar el mapa y escribir un nombre y un mail. Cada vez que la persona toca
     * el mapa se renueva, así que tardar en decidir no cuesta la butaca; lo que la ventana
     * acota es cuánto retiene una butaca alguien que cerró la pestaña y no va a volver.
     */
    public static final Duration MIENTRAS_ELIGE = Duration.ofMinutes(3);

    private final ReservaDAO reservaDAO;
    private final FuncionDAO funcionDAO;
    private final AsientoDAO asientoDAO;
    private final BloqueoButacas bloqueos;

    public Ocupacion(ReservaDAO reservaDAO, FuncionDAO funcionDAO, AsientoDAO asientoDAO,
                     BloqueoButacas bloqueos) {
        this.reservaDAO = reservaDAO;
        this.funcionDAO = funcionDAO;
        this.asientoDAO = asientoDAO;
        this.bloqueos = bloqueos;
    }

    /**
     * Ids de las butacas tomadas en esa función. Las reservas canceladas y las expiradas
     * liberan las suyas (R6), y los bloqueos vencidos también.
     */
    public Set<Integer> asientosOcupados(int funcionId) {
        return asientosOcupados(funcionId, null);
    }

    /**
     * Lo mismo, pero sin contar las butacas que bloqueó esa sesión: las suyas no le están
     * ocupadas a ella.
     *
     * <p>Es lo que hace que el mapa le muestre elegidas —y no tomadas— las butacas que
     * acaba de clickear, y lo que evita que {@link GestorReservas#reservar} rechace una
     * reserva por un bloqueo que puso el mismo que está comprando.
     */
    public Set<Integer> asientosOcupados(int funcionId, String sesion) {
        expirarVencidas(funcionId);
        Set<Integer> ocupados = reservaDAO.listarPorFuncion(funcionId).stream()
                .filter(Reserva::estaVigente)
                .flatMap(r -> r.getEntradas().stream())
                .map(Entrada::asientoId)
                .collect(Collectors.toCollection(HashSet::new));
        bloqueos.bloqueadas(funcionId).forEach((asientoId, duenio) -> {
            if (!duenio.equals(sesion)) {
                ocupados.add(asientoId);
            }
        });
        return ocupados;
    }

    /** Butacas de la sala que todavía nadie tomó para esa función. */
    public List<Asiento> asientosLibres(int funcionId) {
        return asientosLibres(funcionId, null);
    }

    public List<Asiento> asientosLibres(int funcionId, String sesion) {
        Set<Integer> ocupados = asientosOcupados(funcionId, sesion);
        return asientosDeLaSala(funcionId).stream()
                .filter(a -> a.getEstado() != EstadoAsiento.FUERA_DE_SERVICIO)
                .filter(a -> !ocupados.contains(a.getId()))
                .toList();
    }

    public int lugaresLibres(int funcionId) {
        return asientosLibres(funcionId).size();
    }

    public int lugaresLibres(int funcionId, String sesion) {
        return asientosLibres(funcionId, sesion).size();
    }

    /**
     * Le guarda a esa sesión las butacas que está eligiendo, y le suelta las que dejó de
     * elegir.
     *
     * <p>Recibe la selección entera y no una butaca suelta porque así la operación es
     * idempotente: el navegador manda lo que tiene elegido cada vez que la persona toca el
     * mapa, y eso alcanza para tomar lo nuevo, renovar lo que sigue eligiendo y devolver a
     * la venta lo que deseleccionó. Con un "tomar" y un "soltar" separados, una pestaña
     * cerrada en el medio dejaba butacas tomadas que nadie iba a soltar.
     *
     * <p>Que una butaca no se consiga no es un error: es que otro llegó primero. Por eso
     * devuelve lo que consiguió en vez de fallar, y quien llama compara contra lo que pidió.
     *
     * @return los códigos que quedaron efectivamente a nombre de esa sesión
     */
    public List<String> bloquear(int funcionId, Collection<String> codigos, String sesion) {
        if (sesion == null || sesion.isBlank()) {
            throw new IllegalArgumentException("Hace falta una sesión para bloquear butacas");
        }
        List<Asiento> deLaSala = asientosDeLaSala(funcionId);
        Set<Integer> ocupados = asientosOcupados(funcionId, sesion);

        List<Asiento> pedidos = codigos == null ? List.of() : codigos.stream()
                .map(codigo -> buscarEnLaSala(deLaSala, codigo))
                .toList();

        List<String> conseguidas = pedidos.stream()
                .filter(a -> !ocupados.contains(a.getId()))
                .filter(a -> bloqueos.bloquear(funcionId, a.getId(), sesion, MIENTRAS_ELIGE))
                .map(Asiento::getCodigo)
                .toList();

        Set<Integer> sigueEligiendo = pedidos.stream().map(Asiento::getId).collect(Collectors.toSet());
        soltarDeLaSesion(funcionId, sesion, asientoId -> !sigueEligiendo.contains(asientoId));
        return conseguidas;
    }

    /** Suelta todo lo que esa sesión tenga bloqueado en esa función. */
    public void liberar(int funcionId, String sesion) {
        soltarDeLaSesion(funcionId, sesion, asientoId -> true);
    }

    private void soltarDeLaSesion(int funcionId, String sesion, IntPredicate corresponde) {
        for (Map.Entry<Integer, String> bloqueada : bloqueos.bloqueadas(funcionId).entrySet()) {
            if (bloqueada.getValue().equals(sesion) && corresponde.test(bloqueada.getKey())) {
                bloqueos.liberar(funcionId, bloqueada.getKey(), sesion);
            }
        }
    }

    private List<Asiento> asientosDeLaSala(int funcionId) {
        Funcion funcion = funcionDAO.buscarPorId(funcionId)
                .orElseThrow(() -> new IllegalArgumentException("No existe la función " + funcionId));
        return asientoDAO.listarPorSala(funcion.getSalaId());
    }

    /** Mismo mensaje que al reservar: para quien elige es la misma butaca inexistente. */
    private static Asiento buscarEnLaSala(List<Asiento> deLaSala, String codigo) {
        String buscado = codigo == null ? "" : codigo.trim().toUpperCase();
        return deLaSala.stream()
                .filter(a -> a.getCodigo().equals(buscado))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "La butaca " + buscado + " no existe en esa sala"));
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
     *
     * <p>El bloqueo de quien elige no necesita nada de esto: vence solo en el medio donde
     * vive, y no deja ninguna fila que después haya que corregir. Es la diferencia entre
     * un dato del negocio que caducó y una retención temporal que se olvida.
     */
    private void expirarVencidas(int funcionId) {
        LocalDateTime ahora = LocalDateTime.now();
        for (Reserva reserva : reservaDAO.listarPorFuncion(funcionId)) {
            if (reserva.estaVencida(ahora)) {
                reserva.setEstado(EstadoReserva.EXPIRADA);
                reservaDAO.actualizar(reserva);
                // La única línea del log que nadie pidió: pasa sola, sin que haya un
                // usuario del otro lado. Sin registro, una butaca que se libera parece
                // magia el día que un cliente reclama que la tenía reservada.
                LOG.info("reserva {} EXPIRADA · creada {} · {} butacas vuelven a la venta",
                        reserva.getId(), reserva.getCreadaEn(), reserva.getCantidadEntradas());
            }
        }
    }
}
